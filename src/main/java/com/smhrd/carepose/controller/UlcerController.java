package com.smhrd.carepose.controller;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PositionRepository;

@RestController
@RequestMapping("/api/ulcer")
public class UlcerController {

    @Autowired
    private PositionRepository positionRepository;

    private static final String POSE_IMAGE_ROOT = "/home/ec2-user/carepose-images/images";

    @PostMapping("/pose")
    public ResponseEntity<Map<String, Object>> receivePoseEvent(
            @RequestParam("bedId") String bedId,
            @RequestParam("poseType") String poseType,
            @RequestParam("image") MultipartFile imageFile) {

        Map<String, Object> response = new HashMap<>();
        System.out.println("\n[요청 수신] Bed ID: " + bedId + ", Pose: " + poseType);

        try {
            // 1. 호실 및 폴더 생성 시도 로깅
            String roomNum = bedId.replaceAll("[^0-9]", "");
            Path targetDir = Paths.get(POSE_IMAGE_ROOT, roomNum, bedId);
            
            if (!Files.exists(targetDir)) {
                System.out.println("📁 폴더가 없어 생성합니다: " + targetDir);
                Files.createDirectories(targetDir);
            }

            // 2. 파일 이름 결정 (face1, face2...)
            int nextNum = 1;
            while (Files.exists(targetDir.resolve(poseType + nextNum + ".jpg"))) {
                nextNum++;
            }
            String fileName = poseType + nextNum + ".jpg";
            Path filePath = targetDir.resolve(fileName);

            // 3. 파일 저장 실행
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ [성공] 사진 저장 완료!");
            System.out.println("📍 저장 경로: " + filePath.toAbsolutePath());
            
            // 4. [추가된 부분] DB 업데이트 호출
            updateDatabase(bedId, poseType);
            
            response.put("success", true);
            response.put("path", filePath.toString());
            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            System.err.println("❌ [실패] 권한 거부! sudo chmod -R 777 /home/ec2-user/carepose-images 실행 필요");
            response.put("message", "권한 부족");
            return ResponseEntity.internalServerError().body(response);
        } catch (IOException e) {
            System.err.println("❌ [실패] 입출력 오류 발생: " + e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            System.err.println("❌ [실패] 알 수 없는 시스템 오류: " + e.toString());
            response.put("message", "기타 시스템 오류");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // DB 업데이트를 담당하는 비공개 메서드
    private void updateDatabase(String bedId, String poseType) {
        String koreanPosition = switch (poseType.toLowerCase()) {
            case "face" -> "앙와위";
            case "left" -> "좌측위";
            case "right" -> "우측위";
            default -> poseType;
        };

        // Repository에 새로 만든 findOptionalByPatientId 사용
        positionRepository.findOptionalByPatientId(bedId).ifPresentOrElse(entity -> {
            // 기존 환자가 있는 경우: Update
            entity.setLastPosition(koreanPosition);
            entity.setLastPositionTime(LocalDateTime.now());
            positionRepository.save(entity);
            System.out.println("📊 [DB Update] " + bedId + " -> " + koreanPosition);
        }, () -> {
            // 신규 환자인 경우: Insert
            PositionEntity newPos = PositionEntity.builder()
                    .patientId(bedId)
                    .lastPosition(koreanPosition)
                    .lastPositionTime(LocalDateTime.now())
                    .build();
            positionRepository.save(newPos);
            System.out.println("📊 [DB Insert] " + bedId + " 신규 등록");
        });
    }
}