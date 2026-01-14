# send.py
import cv2
import requests
import time

SERVER_URL = "http://192.168.219.206:8002/analyze"
BED_ID = "601A"

cap = cv2.VideoCapture(0)  # APC930 카메라
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

FRAME_INTERVAL = 0.2  # 1초에 5프레임

print("📷 Raspberry Pi Camera Sender 시작")

while True:
    ret, frame = cap.read()
    if not ret:
        print("❌ 카메라 프레임 실패")
        break

    _, buf = cv2.imencode(".jpg", frame)

    try:
        requests.post(
            SERVER_URL,
            files={"image": buf.tobytes()},
            data={"bed_id": BED_ID},
            timeout=1
        )
    except Exception as e:
        print("⚠ 서버 전송 실패:", e)

    time.sleep(FRAME_INTERVAL)

cap.release()
