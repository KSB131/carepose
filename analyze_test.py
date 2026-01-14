import cv2
import numpy as np
import os
import time
import re

# ==================== 경로 설정 ====================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 1️⃣ 낙상 이미지 (Spring Boot static)
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

# 2️⃣ 자세 이미지
IMAGE_ROOT = r"C:\carepose-images\images"

BED_ID = "601D"
POSE_NAME = "left"  # face / left / right


# ==================== 유틸 ====================
def get_room_folder(bed_id: str):
    room = bed_id[:3]
    bed_dir = os.path.join(IMAGE_ROOT, room, bed_id)
    os.makedirs(bed_dir, exist_ok=True)
    return bed_dir


def get_next_pose_filename(bed_dir: str, pose: str):
    pattern = re.compile(rf"^{pose}(\d+)\.jpg$")
    max_num = 0

    for fname in os.listdir(bed_dir):
        m = pattern.match(fname)
        if m:
            max_num = max(max_num, int(m.group(1)))

    return f"{pose}{max_num + 1}.jpg"


# ==================== 더미 이미지 생성 ====================
def create_dummy_image(text: str):
    img = np.zeros((480, 640, 3), dtype=np.uint8)
    img[:] = (40, 40, 40)

    cv2.putText(
        img,
        text,
        (50, 240),
        cv2.FONT_HERSHEY_SIMPLEX,
        1.2,
        (0, 255, 0),
        3
    )
    return img


# ==================== 테스트 실행 ====================
if __name__ == "__main__":
    print("🧪 저장 테스트 시작")

    # ---------- 낙상 저장 ----------
    fall_img = create_dummy_image("TEST FALL IMAGE")
    fall_name = f"fall_{BED_ID}_{int(time.time())}.jpg"
    fall_path = os.path.join(FALL_DIR, fall_name)

    cv2.imwrite(fall_path, fall_img)
    print(f"✅ 낙상 이미지 저장 완료 → {fall_path}")

    # ---------- 자세 저장 ----------
    bed_dir = get_room_folder(BED_ID)
    pose_filename = get_next_pose_filename(bed_dir, POSE_NAME)
    pose_path = os.path.join(bed_dir, pose_filename)

    pose_img = create_dummy_image("TEST POSE IMAGE")
    cv2.imwrite(pose_path, pose_img)
    print(f"✅ 자세 이미지 저장 완료 → {pose_path}")

    print("🎉 모든 저장 테스트 성공")
