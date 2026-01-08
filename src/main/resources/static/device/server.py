from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os
import json

app = FastAPI()

# server.py가 있는 디렉터리 기준으로 fall_status.txt 경로 설정
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FALL_STATUS_FILE = os.path.join(BASE_DIR, "fall_status.txt")

# 웹페이지에서 접근 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # 나중에 필요하면 제한 가능
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 테스트용 API
@app.get("/health")
def health_check():
    return {"status": "ok"}


@app.get("/fall_status")
async def fall_status():
    # 파일에서 낙상 상태 읽기 (JSON 형식)
    try:
        if os.path.exists(FALL_STATUS_FILE):
            with open(FALL_STATUS_FILE, "r") as f:
                data = json.load(f)
                return data
    except:
        pass
    return {"fall": False, "room": "", "image": "", "timestamp": ""}
