import cv2
import torch
import numpy as np
import time
import os
import sys
import json
from collections import deque
from ultralytics import YOLO
import torch.nn as nn
import pymysql

torch.set_num_threads(1)

# 침상번호 설정 (여기를 수정하세요)
ROOM_NUMBER = "601A"  # 예: "601A", "601B", "602A" 등

# DB 연결 설정
DB_CONFIG = {
    'host': 'project-db-campus.smhrd.com',
    'port': 3307,
    'user': 'carepose123',
    'password': 'care123',
    'database': 'carepose123',
    'charset': 'utf8mb4'
}

fall_detected = False
FALL_STATUS_FILE = "fall_status.txt"  # 낙상 상태 파일
last_fall_alert_time = 0  # 마지막 낙상 알림 시간
FALL_ALERT_DURATION = 5  # 낙상 알림 지속 시간 (초)

# 초기 상태 파일 생성
with open(FALL_STATUS_FILE, "w") as f:
    json.dump({"fall": False, "room": ROOM_NUMBER, "image": "", "timestamp": ""}, f)

# ===============================
# 1. 포즈 분류 모델
# ===============================
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

# ===============================
# 2. 모델 로드
# ===============================
pose_model = YOLO("yolo11n-pose.pt")

cls_model = PoseClassifier()
cls_model.load_state_dict(torch.load("pose_cls.pt", map_location="cpu"))
cls_model.eval()

pose_names = ["supine", "left", "right"]

# ===============================
# 3. 프레임 설정
# ===============================
FRAME_W, FRAME_H = 640, 480
LEFT_X = int(FRAME_W * 0.10)
RIGHT_X = int(FRAME_W * 0.90)

# ===============================
# 4. 저장 폴더 설정
# ===============================
ULCER_DIR = "ulcer"
FALL_DIR = "fall"

os.makedirs(ULCER_DIR, exist_ok=True)
os.makedirs(FALL_DIR, exist_ok=True)

# 파일 카운터
ulcer_count = {
    "face": 0,
    "left": 0,
    "right": 0
}
fall_count = 0

# 쿨타임
FALL_COOLDOWN = 10  # 낙상 감지 쿨타임 증가 (3초 → 10초)
POSE_COOLDOWN = 2

last_fall_save = 0
last_pose_save = 0

# ===============================
# 5. 자세 안정화 버퍼
# ===============================
POSE_STABLE_FRAMES = 12
pose_buffer = deque(maxlen=POSE_STABLE_FRAMES)
stable_pose = None

# 낙상 연속 감지 버퍼
FALL_STABLE_FRAMES = 3  # 3프레임 연속 감지 필요
fall_buffer = deque(maxlen=FALL_STABLE_FRAMES)

# ===============================
# 6. 낙상 감지 (몸통 기준)
# ===============================
def fall_risk(keypoints):
    # 손목(9,10) + 발목(15,16) 체크
    limb_idx = [9, 10, 15, 16]  # 왼손목, 오른손목, 왼발목, 오른발목
    out_count = 0

    for idx in limb_idx:
        x, y = keypoints[idx]
        if x < LEFT_X or x > RIGHT_X:
            out_count += 1

    return out_count >= 1  # 4개 중 2개 이상 벗어나면 감지

# ===============================
# 7. 웹캠 시작
# ===============================
cap = cv2.VideoCapture(0)
print("📷 실행 중 (ESC 종료)")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.resize(frame, (FRAME_W, FRAME_H))

    results = pose_model(frame, imgsz=320, conf=0.7, verbose=False)[0]
    annotated = results.plot()

    if results.keypoints is not None and len(results.keypoints.xy) > 0:
        kpts = results.keypoints.xy.cpu().numpy()
        confs = results.boxes.conf.cpu().numpy()

        best = np.argmax(confs)
        keypoints = kpts[best]

        # ===============================
        # 낙상 감지 → fall 폴더 (연속 감지 필요)
        # ===============================
        is_fall_now = fall_risk(keypoints)
        fall_buffer.append(is_fall_now)
        
        # 3프레임 연속 감지되면 낙상으로 판단
        if fall_buffer.count(True) >= FALL_STABLE_FRAMES:
            fall_detected = True
            
            cv2.putText(annotated, "⚠ FALL RISK!", (30, 40),
                        cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 0, 255), 3)

            now = time.time()
            if now - last_fall_save > FALL_COOLDOWN:
                # 고유한 파일명 생성 (언더스코어 없이 간단하게)
                timestamp = time.strftime("%Y%m%d%H%M%S")
                filename = f"fall{ROOM_NUMBER}{timestamp}.jpg"
                filepath = f"{FALL_DIR}/{filename}"
                cv2.imwrite(filepath, frame)
                print(f"📸 낙상 저장 → {filepath}")
                
                current_time = time.strftime("%Y-%m-%d %H:%M:%S")
                fall_num = None
                
                # DB에 낙상 기록 저장
                try:
                    conn = pymysql.connect(**DB_CONFIG)
                    cursor = conn.cursor()
                    sql = "INSERT INTO fall (patient_id, pic_id, fall_body, fall_at) VALUES (%s, %s, %s, %s)"
                    cursor.execute(sql, (ROOM_NUMBER, filename, "", current_time))
                    conn.commit()
                    
                    # INSERT된 fall_num 가져오기
                    fall_num = cursor.lastrowid
                    print(f"✅ DB 저장 완료: fall_num={fall_num}, {ROOM_NUMBER} - {filename}")
                except Exception as e:
                    print(f"❌ DB 저장 실패: {e}")
                finally:
                    if 'conn' in locals():
                        cursor.close()
                        conn.close()
                
                # 파일에 낙상 상태 저장 (JSON 형식) - fall_num 포함
                with open(FALL_STATUS_FILE, "w") as f:
                    json.dump({
                        "fall": True,
                        "fall_num": fall_num,
                        "room": ROOM_NUMBER,
                        "image": filename,
                        "timestamp": current_time
                    }, f)
                
                last_fall_save = now
                last_fall_alert_time = now  # 알림 시작 시간 기록
        else:
            fall_detected = False
        
        # 낙상 알림 지속 시간이 지나면 false로 리셋
        now = time.time()
        if now - last_fall_alert_time > FALL_ALERT_DURATION:
            with open(FALL_STATUS_FILE, "w") as f:
                json.dump({"fall": False, "room": ROOM_NUMBER, "image": "", "timestamp": ""}, f)

        # ===============================
        # 포즈 분류
        # ===============================
        x = torch.tensor(keypoints.flatten(), dtype=torch.float32).unsqueeze(0)
        with torch.no_grad():
            pred = cls_model(x).argmax(1).item()

        pose_buffer.append(pred)

        # ===============================
        # 자세 변경 → ulcer 폴더
        # ===============================
        if pose_buffer.count(pred) >= POSE_STABLE_FRAMES:
            if stable_pose != pred:
                now = time.time()
                if now - last_pose_save > POSE_COOLDOWN:

                    if pred == 0:
                        label = "face"
                    elif pred == 1:
                        label = "left"
                    else:
                        label = "right"

                    ulcer_count[label] += 1
                    filename = f"{ULCER_DIR}/{label}{ulcer_count[label]}.jpg"
                    cv2.imwrite(filename, frame)

                    print(f"🛏 자세 변경 저장 → {filename}")

                    stable_pose = pred
                    last_pose_save = now

        cv2.putText(annotated, f"POSE: {pose_names[pred]}", (30, 80),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

    # ===============================
    # 사이드 영역 표시
    # ===============================
    cv2.rectangle(annotated, (0, 0), (LEFT_X, FRAME_H), (255, 0, 0), 2)
    cv2.rectangle(annotated, (RIGHT_X, 0), (FRAME_W, FRAME_H), (255, 0, 0), 2)

    cv2.imshow("Bedsore + Fall Detection", annotated)

    if cv2.waitKey(1) == 27:
        break

cap.release()
cv2.destroyAllWindows()
