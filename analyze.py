# analyze.py
import cv2
import torch
import numpy as np
import time
import os
import json
import pymysql
from collections import deque
from fastapi import FastAPI, UploadFile, Form
from ultralytics import YOLO
import torch.nn as nn
import re
from fastapi.responses import StreamingResponse
import asyncio
from fastapi.middleware.cors import CORSMiddleware

IMAGE_ROOT = r"C:\carepose-images\images"

# ==================== BASE DIR (최상단) ====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# ==================== Spring Boot static 저장 경로 ====================
FALL_DIR = os.path.join(
    BASE_DIR,
    "src",
    "main",
    "resources",
    "static",
    "device",
    "fall"
)
os.makedirs(FALL_DIR, exist_ok=True)

FALL_STATUS_FILE = os.path.join(BASE_DIR, "fall_status.json")

app = FastAPI()

# [추가] 브라우저에서 스트리밍에 접근할 수 있도록 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 2. 전역 변수 추가 (최신 프레임을 저장할 변수)
current_frame = None

# ==================== 설정 ====================
FRAME_W, FRAME_H = 640, 480
LEFT_X = int(FRAME_W * 0.10)
RIGHT_X = int(FRAME_W * 0.90)

POSE_STABLE_FRAMES = 12
POSE_COOLDOWN = 2
FALL_STABLE_FRAMES = 3
FALL_COOLDOWN = 10
FALL_ALERT_DURATION = 5

# 얼굴 프라이버시 설정
ENABLE_FACE_PRIVACY = True
PRIVACY_MODE = "blur"  # "mosaic", "blur", "black"
PIXEL_SIZE = 30
BLUR_STRENGTH = 51

# 화면 표시 설정
ENABLE_DISPLAY = True  # imshow 활성화 여부

os.makedirs(FALL_DIR, exist_ok=True)

FALL_STATUS_FILE = os.path.join(BASE_DIR, "fall_status.json")

DB_CONFIG = {
    'host': 'project-db-campus.smhrd.com',
    'port': 3307,
    'user': 'carepose123',
    'password': 'care123',
    'database': 'carepose123',
    'charset': 'utf8mb4'
}



# ==================== 모델 ====================
class PoseClassifier(nn.Module):
    def __init__(self, num_classes=3):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(34, 128),
            nn.ReLU(),
            nn.Linear(128, 64),
            nn.ReLU(),
            nn.Linear(64, num_classes)
        )

    def forward(self, x):
        return self.net(x)

pose_model = YOLO(os.path.join(BASE_DIR, "yolo11n-pose.pt"))
cls_model = PoseClassifier()
cls_model.load_state_dict(torch.load(os.path.join(BASE_DIR, "pose_cls.pt"), map_location="cpu"))
cls_model.eval()

pose_names = ["face", "left", "right"]

# ==================== 상태 버퍼 ====================
pose_buffer = deque(maxlen=POSE_STABLE_FRAMES)
fall_buffer = deque(maxlen=FALL_STABLE_FRAMES)
last_pose_time = 0
last_fall_time = 0
last_fall_alert = 0
stable_pose = None

# ==================== 유틸 ====================
def apply_mosaic(image, x, y, w, h, pixel_size=20):
    """모자이크 적용"""
    face_region = image[y:y+h, x:x+w]
    if face_region.size > 0:
        small_h = max(1, h // pixel_size)
        small_w = max(1, w // pixel_size)
        temp = cv2.resize(face_region, (small_w, small_h), interpolation=cv2.INTER_LINEAR)
        mosaic = cv2.resize(temp, (w, h), interpolation=cv2.INTER_NEAREST)
        image[y:y+h, x:x+w] = mosaic
    return image

def apply_blur(image, x, y, w, h, strength=51):
    """가우시안 블러 적용"""
    face_region = image[y:y+h, x:x+w]
    if face_region.size > 0:
        blurred = cv2.GaussianBlur(face_region, (strength, strength), 0)
        image[y:y+h, x:x+w] = blurred
    return image

def apply_black_box(image, x, y, w, h):
    """검은 박스"""
    cv2.rectangle(image, (x, y), (x+w, y+h), (0, 0, 0), -1)
    text = "( ◕‿◕ )"
    font_scale = w / 150
    thickness = max(1, int(w / 100))
    text_size = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, font_scale, thickness)[0]
    text_x = x + (w - text_size[0]) // 2
    text_y = y + (h + text_size[1]) // 2
    cv2.putText(image, text, (text_x, text_y), 
               cv2.FONT_HERSHEY_SIMPLEX, font_scale, (255, 255, 255), thickness)
    return image

def protect_face_with_yolo(image, keypoints, mode="mosaic"):
    """YOLO 키포인트로 얼굴 영역 프라이버시 보호"""
    face_kpts = keypoints[:5]
    valid_points = face_kpts[face_kpts[:, 0] > 0]
    
    if len(valid_points) >= 2:
        x_min = int(valid_points[:, 0].min())
        y_min = int(valid_points[:, 1].min())
        x_max = int(valid_points[:, 0].max())
        y_max = int(valid_points[:, 1].max())
        
        margin = int((x_max - x_min) * 0.3)
        x_min = max(0, x_min - margin)
        y_min = max(0, y_min - int(margin * 1.5))
        x_max = min(image.shape[1], x_max + margin)
        y_max = min(image.shape[0], y_max + margin)
        
        w = x_max - x_min
        h = y_max - y_min
        
        if w > 0 and h > 0:
            if mode == "mosaic":
                image = apply_mosaic(image, x_min, y_min, w, h, PIXEL_SIZE)
            elif mode == "blur":
                image = apply_blur(image, x_min, y_min, w, h, BLUR_STRENGTH)
            elif mode == "black":
                image = apply_black_box(image, x_min, y_min, w, h)
    
    return image

def fall_risk(keypoints):
    limb_idx = [9, 10, 15, 16]
    return sum(
        1 for idx in limb_idx
        if keypoints[idx][0] < LEFT_X or keypoints[idx][0] > RIGHT_X
    ) >= 1


def write_fall_status(data):
    with open(FALL_STATUS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False)


def save_fall_to_db(bed_id, image_name, timestamp):
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    sql = """
        INSERT INTO fall (patient_id, pic_id, fall_body, fall_at)
        VALUES (%s, %s, %s, %s)
    """
    cursor.execute(sql, (bed_id, image_name, "wrist", timestamp))
    conn.commit()
    cursor.close()
    conn.close()

def get_room_folder(bed_id: str):
    """
    601A → C:\carepose-images\images\601\601A
    """
    room = bed_id[:3]   # 601
    room_dir = os.path.join(IMAGE_ROOT, room)
    bed_dir = os.path.join(room_dir, bed_id)

    os.makedirs(bed_dir, exist_ok=True)
    return bed_dir

def get_next_pose_filename(bed_dir: str, pose: str):
    """
    face1.jpg, face2.jpg ... 중 가장 큰 번호 +1
    """
    pattern = re.compile(rf"^{pose}(\d+)\.jpg$")
    max_num = 0

    for fname in os.listdir(bed_dir):
        m = pattern.match(fname)
        if m:
            max_num = max(max_num, int(m.group(1)))

    return f"{pose}{max_num + 1}.jpg"

# ==================== API ====================
@app.post("/analyze")
async def analyze(image: UploadFile, bed_id: str = Form(...)):
    global last_fall_time, last_fall_alert, last_pose_time, stable_pose, current_frame

    frame = cv2.imdecode(
        np.frombuffer(await image.read(), np.uint8),
        cv2.IMREAD_COLOR
    )
    frame = cv2.resize(frame, (FRAME_W, FRAME_H))

    results = pose_model(frame, imgsz=320, conf=0.6, verbose=False)[0]

    if results.keypoints is None or len(results.keypoints) == 0:
        return {"status": "no_person"}

    kpts = results.keypoints.xy.cpu().numpy()
    confs = results.boxes.conf.cpu().numpy()
    
    # 사람이 감지되지 않았을 때 처리
    if len(confs) == 0 or len(kpts) == 0:
        return {"status": "no_person"}
    
    keypoints = kpts[np.argmax(confs)]

    # YOLO 결과로 어노테이션된 이미지 생성
    annotated = results.plot()

    # 얼굴 프라이버시 보호
    if ENABLE_FACE_PRIVACY:
        annotated = protect_face_with_yolo(
            annotated,
            keypoints,
            mode=PRIVACY_MODE
        )

    # ===== 낙상 =====
    is_fall = fall_risk(keypoints)
    fall_buffer.append(is_fall)

    now = time.time()
    fall_detected = False
    
    if fall_buffer.count(True) >= FALL_STABLE_FRAMES:
        fall_detected = True
        # 화면에 경고 표시
        cv2.putText(
            annotated,
            "WARNING: FALL RISK",
            (30, 40),
            cv2.FONT_HERSHEY_SIMPLEX,
            1.2,
            (0, 0, 255),
            3
        )
        
        if now - last_fall_time > FALL_COOLDOWN:
            timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
            image_name = f"fall_{bed_id}_{int(now)}.jpg"
            cv2.imwrite(os.path.join(FALL_DIR, image_name), annotated)

            save_fall_to_db(bed_id, image_name, timestamp)

            write_fall_status({
                "fall": True,
                "room": bed_id,
                "image": image_name,
                "timestamp": timestamp
            })

            last_fall_time = now
            last_fall_alert = now

    if now - last_fall_alert > FALL_ALERT_DURATION:
        write_fall_status({"fall": False})

    # ===== 자세 =====
    x = torch.tensor(keypoints.flatten(), dtype=torch.float32).unsqueeze(0)
    pred = cls_model(x).argmax(1).item()
    pose_buffer.append(pred)

    if pose_buffer.count(pred) >= POSE_STABLE_FRAMES:
       if stable_pose != pred and now - last_pose_time > POSE_COOLDOWN:
           stable_pose = pred
           last_pose_time = now
   
           pose_name = pose_names[pred]  # face / left / right
   
           # 1. 침대 폴더 확보
           bed_dir = get_room_folder(bed_id)

           # 2. 다음 파일명 생성
           file_name = get_next_pose_filename(bed_dir, pose_name)

           # 3. 저장
           save_path = os.path.join(bed_dir, file_name)
           cv2.imwrite(save_path, annotated)

           print(f"📸 저장 완료: {save_path}")
    
    # 화면에 자세 정보 표시
    cv2.putText(
        annotated,
        f"POSE: {pose_names[pred]}",
        (30, 80),
        cv2.FONT_HERSHEY_SIMPLEX,
        1,
        (0, 255, 0),
        2
    )
    
    # 침대 경계선 표시
    cv2.rectangle(annotated, (0, 0), (LEFT_X, FRAME_H), (255, 0, 0), 2)
    cv2.rectangle(annotated, (RIGHT_X, 0), (FRAME_W, FRAME_H), (255, 0, 0), 2)
    
    # 화면 표시
    if ENABLE_DISPLAY:
        cv2.imshow(f"Bed {bed_id} - Analysis", annotated)
        cv2.waitKey(1)
        
    _, buffer = cv2.imencode('.jpg', annotated) # 'frame' 대신 'annotated'를 사용하여 분석 내용 노출
    current_frame = buffer.tobytes()

    return {"status": "ok"}

# 4. 스트리밍 엔드포인트 추가 (기존 stream.py 내용)
async def generate_frames():
    global current_frame
    while True:
        if current_frame is not None:
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + current_frame + b'\r\n')
        await asyncio.sleep(0.04) # 약 25 FPS 유지

@app.get("/video_feed")
async def video_feed():
    return StreamingResponse(generate_frames(), media_type="multipart/x-mixed-replace; boundary=frame")
    
