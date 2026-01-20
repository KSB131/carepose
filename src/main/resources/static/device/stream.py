import cv2
import asyncio
from fastapi import FastAPI, Response
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import numpy as np

# ================= 설정 =================
FRAME_W, FRAME_H = 640, 480

app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 전역 카메라 객체
camera = None

def init_camera():
    global camera
    if camera is None or not camera.isOpened():
        camera = cv2.VideoCapture(0)
        if not camera.isOpened():
            camera = cv2.VideoCapture(0, cv2.CAP_V4L2)
        
        camera.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M','J','P','G'))
        camera.set(cv2.CAP_PROP_FRAME_WIDTH, FRAME_W)
        camera.set(cv2.CAP_PROP_FRAME_HEIGHT, FRAME_H)
        camera.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    return camera

# ================= 스트리밍 =================
async def generate_frames():
    cam = init_camera()
    
    try:
        while True:
            ret, frame = cam.read()
            if not ret:
                await asyncio.sleep(0.1)
                continue

            frame = cv2.resize(frame, (FRAME_W, FRAME_H))

            # MJPEG 인코딩
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])

            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + buffer.tobytes()
                + b"\r\n"
            )

            await asyncio.sleep(0.033)  # 약 30fps
    except Exception as e:
        print(f"Error in generate_frames: {e}")
        if camera:
            camera.release()

@app.get("/video_feed")
async def video_feed():
    return StreamingResponse(
        generate_frames(),
        media_type="multipart/x-mixed-replace; boundary=frame",
        headers={
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': '0',
        }
    )

@app.get("/frame")
async def get_frame():
    try:
        cam = init_camera()
        ret, frame = cam.read()
        
        if ret:
            frame = cv2.resize(frame, (FRAME_W, FRAME_H))
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
            
            return Response(
                content=buffer.tobytes(),
                media_type="image/jpeg",
                headers={
                    'Cache-Control': 'no-cache, no-store, must-revalidate',
                    'Pragma': 'no-cache',
                    'Expires': '0',
                }
            )
        else:
            blank = np.zeros((FRAME_H, FRAME_W, 3), dtype=np.uint8)
            _, buffer = cv2.imencode('.jpg', blank)
            return Response(content=buffer.tobytes(), media_type="image/jpeg")
    except Exception as e:
        print(f"Error in get_frame: {e}")
        blank = np.zeros((FRAME_H, FRAME_W, 3), dtype=np.uint8)
        cv2.putText(blank, "Camera Error", (50, 240), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
        _, buffer = cv2.imencode('.jpg', blank)
        return Response(content=buffer.tobytes(), media_type="image/jpeg")

# ================= 실행 =================
if __name__ == "__main__":
    print("🎥 카메라 스트리밍 서버 시작...")
    print(f"📡 http://0.0.0.0:8003/video_feed")
    print(f"📷 http://0.0.0.0:8003/frame")
    uvicorn.run(app, host="0.0.0.0", port=8003, timeout_keep_alive=75)