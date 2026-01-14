# stream.py
import cv2
import numpy as np
from fastapi import FastAPI, UploadFile, Form
from fastapi.responses import StreamingResponse
import asyncio

app = FastAPI()

# 전역 변수에 최신 프레임 저장
current_frame = None

# 1. 라즈베리 파이로부터 사진을 받는 통로 (send.py가 여기로 쏨)
@app.post("/analyze")
async def analyze(image: UploadFile, bed_id: str = Form(...)):
    global current_frame
    contents = await image.read()
    nparr = np.frombuffer(contents, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    # 여기서 분석(YOLO 등)을 수행한 후 annotated 프레임 생성
    # 일단은 원본을 스트리밍용 변수에 저장
    _, buffer = cv2.imencode('.jpg', frame)
    current_frame = buffer.tobytes()
    return {"status": "success"}

# 2. HTML 팝업창에서 영상을 가져가는 통로
async def generate_frames():
    while True:
        if current_frame is not None:
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + current_frame + b'\r\n')
        await asyncio.sleep(0.05)

@app.get("/video_feed")
async def video_feed():
    return StreamingResponse(generate_frames(), media_type="multipart/x-mixed-replace; boundary=frame")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)