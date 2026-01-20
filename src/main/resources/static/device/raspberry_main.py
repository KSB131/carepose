"""
라즈베리파이 낙상 감지 시스템
- 카메라에서 프레임 캡처
- YOLO Pose 추정
- 자세 분류
- 낙상 판단
- MJPEG 스트리밍
- 낙상 이벤트만 서버로 전송
"""

import cv2
import torch
import numpy as np
import time
import os
import threading
import requests
from collections import deque
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import uvicorn
import torch.nn as nn
from ultralytics import YOLO

# ==================== 설정 ====================
BED_ID = "601A"  # 침대 ID
SERVER_URL = "http://13.209.89.231:8084/api/fall/event"  # Spring Boot 서버

FRAME_W, FRAME_H = 640, 480

# 침상 영역 (추출된 실제 좌표)
BED_X = 178  # 침대 왼쪽
BED_Y = 51   # 침대 위쪽
BED_W = 257  # 침대 너비
BED_H = 360  # 침대 높이
BED_RIGHT = BED_X + BED_W  # 608 (침대 오른쪽)
BED_BOTTOM = BED_Y + BED_H  # 454 (침대 아래)

POSE_STABLE_FRAMES = 12
POSE_COOLDOWN = 2
FALL_STABLE_FRAMES = 3
FALL_COOLDOWN = 10

# 얼굴 프라이버시
ENABLE_FACE_PRIVACY = True
PRIVACY_MODE = "blur"
PIXEL_SIZE = 30
BLUR_STRENGTH = 51

# 모델 경로
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FALL_DIR = os.path.join(BASE_DIR, "fall")
os.makedirs(FALL_DIR, exist_ok=True)

app = FastAPI()

# ==================== 모델 로드 ====================
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

# ==================== 상태 관리 ====================
pose_buffer = deque(maxlen=POSE_STABLE_FRAMES)
fall_buffer = deque(maxlen=FALL_STABLE_FRAMES)
last_pose_time = 0
last_fall_time = 0
stable_pose = None

# 스트리밍용 전역 변수
latest_frame = None
frame_lock = threading.Lock()

# ==================== 프라이버시 보호 ====================
def apply_blur(image, x, y, w, h, strength=51):
    """가우시안 블러 적용"""
    face_region = image[y:y+h, x:x+w]
    if face_region.size > 0:
        blurred = cv2.GaussianBlur(face_region, (strength, strength), 0)
        image[y:y+h, x:x+w] = blurred
    return image

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

def protect_face_with_yolo(image, keypoints, mode="blur"):
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
    
    return image

# ==================== 낙상 판단 ====================
def fall_risk(keypoints):
    """침대 경계를 벗어난 사지 확인 (상하좌우)"""
    limb_idx = [9, 10, 15, 16]  # 왼손목, 오른손목, 왼발목, 오른발목
    
    out_of_bed_count = 0
    for idx in limb_idx:
        x, y = keypoints[idx][0], keypoints[idx][1]
        # 침대 영역을 벗어났는지 확인 (상하좌우)
        if x < BED_X or x > BED_RIGHT or y < BED_Y or y > BED_BOTTOM:
            out_of_bed_count += 1
    
    # 하나 이상의 사지가 침대 밖에 있으면 낙상 위험
    return out_of_bed_count >= 1

# ==================== 서버 전송 ====================
def send_fall_event_to_server(bed_id, image_path, timestamp):
    """낙상 이벤트를 Spring Boot 서버로 전송"""
    try:
        with open(image_path, 'rb') as img_file:
            files = {'image': (os.path.basename(image_path), img_file, 'image/jpeg')}
            data = {
                'bedId': bed_id,
                'timestamp': timestamp
            }
            response = requests.post(SERVER_URL, files=files, data=data, timeout=5)
            
            if response.status_code == 200:
                print(f"✅ 낙상 이벤트 전송 성공: {bed_id} - {timestamp}")
                return True
            else:
                print(f"❌ 낙상 이벤트 전송 실패: {response.status_code}")
                return False
    except Exception as e:
        print(f"❌ 서버 전송 에러: {e}")
        return False

# ==================== 카메라 워커 ====================
def camera_worker():
    """카메라에서 프레임을 읽고 분석"""
    global latest_frame, last_fall_time, last_pose_time, stable_pose
    
    cap = cv2.VideoCapture(0)
    cap.set(3, FRAME_W)
    cap.set(4, FRAME_H)
    
    print(f"📷 카메라 시작")
    print(f"🛏️  침대 ID: {BED_ID}")
    print(f"🌐 서버: {SERVER_URL}")
    
    while True:
        ret, frame = cap.read()
        if not ret:
            print("❌ 카메라 읽기 실패")
            time.sleep(1)
            continue
        
        # YOLO Pose 추정
        results = pose_model(frame, imgsz=320, conf=0.6, verbose=False)[0]
        
        if results.keypoints is None or len(results.keypoints) == 0:
            # 사람 감지 안됨
            with frame_lock:
                latest_frame = frame.copy()
            continue
        
        kpts = results.keypoints.xy.cpu().numpy()
        confs = results.boxes.conf.cpu().numpy()
        
        if len(confs) == 0 or len(kpts) == 0:
            with frame_lock:
                latest_frame = frame.copy()
            continue
        
        keypoints = kpts[np.argmax(confs)]
        
        # YOLO 어노테이션
        annotated = results[0].plot()
        
        # 얼굴 프라이버시 보호
        if ENABLE_FACE_PRIVACY:
            annotated = protect_face_with_yolo(annotated, keypoints, PRIVACY_MODE)
        
        # ===== 낙상 감지 =====
        is_fall = fall_risk(keypoints)
        fall_buffer.append(is_fall)
        
        now = time.time()
        fall_detected = False
        
        if fall_buffer.count(True) >= FALL_STABLE_FRAMES:
            fall_detected = True
            cv2.putText(
                annotated,
                "WARNING: FALL RISK",
                (30, 40),
                cv2.FONT_HERSHEY_SIMPLEX,
                1.2,
                (0, 0, 255),
                3
            )
            
            # 낙상 이벤트 서버 전송
            if now - last_fall_time > FALL_COOLDOWN:
                timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
                image_name = f"fall_{BED_ID}_{int(now)}.jpg"
                image_path = os.path.join(FALL_DIR, image_name)
                
                # 이미지 저장
                cv2.imwrite(image_path, annotated)
                
                # 서버로 이벤트 전송 (비동기)
                threading.Thread(
                    target=send_fall_event_to_server,
                    args=(BED_ID, image_path, timestamp),
                    daemon=True
                ).start()
                
                last_fall_time = now
                print(f"🚨 낙상 감지! {BED_ID} - {timestamp}")
        
        # ===== 자세 분류 =====
        x = torch.tensor(keypoints.flatten(), dtype=torch.float32).unsqueeze(0)
        pred = cls_model(x).argmax(1).item()
        pose_buffer.append(pred)
        
        if pose_buffer.count(pred) >= POSE_STABLE_FRAMES:
            if stable_pose != pred and now - last_pose_time > POSE_COOLDOWN:
                stable_pose = pred
                last_pose_time = now
                print(f"🛏️  자세 변경: {pose_names[pred]}")
        
        # 자세 표시
        cv2.putText(
            annotated,
            f"POSE: {pose_names[pred]}",
            (30, 80),
            cv2.FONT_HERSHEY_SIMPLEX,
            1,
            (255, 255, 0),
            2
        )
        
        # 침대 경계선 (녹색 사각형)
        cv2.rectangle(annotated, (BED_X, BED_Y), (BED_RIGHT, BED_BOTTOM), (255, 255, 0), 2)
        # 모서리 강조 표시
        corner_size = 20
        cv2.line(annotated, (BED_X, BED_Y), (BED_X + corner_size, BED_Y), (0, 255, 0), 4)
        cv2.line(annotated, (BED_X, BED_Y), (BED_X, BED_Y + corner_size), (0, 255, 0), 4)
        
        # 침대 ID 표시
        cv2.putText(
            annotated,
            f"Bed: {BED_ID}",
            (FRAME_W - 150, 30),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.7,
            (255, 255, 255),
            2
        )
        
        # 최신 프레임 저장 (스트리밍용)
        with frame_lock:
            latest_frame = annotated.copy()
        
        time.sleep(0.01)  # CPU 부하 감소
    
    cap.release()

# ==================== MJPEG 스트리밍 ====================
def generate_frames():
    """MJPEG 스트림 생성"""
    while True:
        with frame_lock:
            if latest_frame is None:
                time.sleep(0.1)
                continue
            frame = latest_frame.copy()
        
        # 시간 표시
        cv2.putText(
            frame,
            time.strftime("%Y-%m-%d %H:%M:%S"),
            (10, FRAME_H - 20),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.5,
            (255, 255, 255),
            1
        )
        
        # JPEG 인코딩
        ret, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
        if not ret:
            continue
        
        frame_bytes = buffer.tobytes()
        
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
        
        time.sleep(0.03)  # ~30fps

# ==================== FastAPI 엔드포인트 ====================
@app.get("/")
async def root():
    return {
        "status": "running",
        "service": "Raspberry Pi Fall Detection System",
        "bed_id": BED_ID,
        "server": SERVER_URL,
        "endpoints": {
            "video_feed": "/video_feed (실시간 분석 화면)"
        }
    }

@app.get("/video_feed")
async def video_feed():
    """실시간 분석 화면 스트리밍"""
    return StreamingResponse(
        generate_frames(),
        media_type="multipart/x-mixed-replace; boundary=frame"
    )

@app.get("/status")
async def status():
    """현재 상태 조회"""
    return {
        "bed_id": BED_ID,
        "current_pose": pose_names[stable_pose] if stable_pose is not None else "unknown",
        "server_url": SERVER_URL
    }

# ==================== 실행 ====================
if __name__ == "__main__":
    # 카메라 워커 시작
    camera_thread = threading.Thread(target=camera_worker, daemon=True)
    camera_thread.start()
    
    print("=" * 60)
    print("🚀 Raspberry Pi Fall Detection System")
    print(f"🛏️  침대 ID: {BED_ID}")
    print(f"🌐 서버: {SERVER_URL}")
    print(f"📹 스트리밍: http://라즈베리파이IP:8005/video_feed")
    print(f"📊 상태: http://라즈베리파이IP:800/status")
    print("=" * 60)
    
    # FastAPI 서버 실행
    uvicorn.run(app, host="0.0.0.0", port=8005, timeout_keep_alive=75)
