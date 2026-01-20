package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smhrd.carepose.entity.FallEntity;
import com.smhrd.carepose.repository.FallRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fall")
public class FallController {
    
    @Autowired
    private FallRepository fallRepository;
    
    private static final String FALL_IMAGE_DIR = "/home/ec2-user/carepose/fall/";
    private static final String FALL_STATUS_FILE = "fall_status.json";
    
    /**
     * 라즈베리파이로부터 낙상 이벤트 수신
     * POST /api/fall/event
     */
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> receiveFallEvent(
            @RequestParam("bedId") String bedId,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("image") MultipartFile imageFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🚨 낙상 이벤트 수신: " + bedId + " - " + timestamp);
            
            // 1. 이미지 저장
            String imageName = imageFile.getOriginalFilename();
            if (imageName == null || imageName.isEmpty()) {
                imageName = "fall_" + bedId + "_" + System.currentTimeMillis() + ".jpg";
            }
            
            File fallDir = new File(FALL_IMAGE_DIR);
            if (!fallDir.exists()) {
                fallDir.mkdirs();
            }
            
            Path imagePath = Paths.get(FALL_IMAGE_DIR, imageName);
            Files.copy(imageFile.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ 이미지 저장: " + imagePath);
            
            // 2. DB 저장
            FallEntity fall = new FallEntity();
            fall.setPatientId(bedId);
            fall.setPicId(imageName);
            fall.setFallBody("wrist");
            
            // timestamp 파싱
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime fallAt = LocalDateTime.parse(timestamp, formatter);
            fall.setFallAt(fallAt);
            
            FallEntity savedFall = fallRepository.save(fall);
            System.out.println("✅ DB 저장 완료: fall_num=" + savedFall.getFallNum());
            
            // 3. fall_status.json 업데이트
            updateFallStatusFile(savedFall.getFallNum(), bedId, imageName, timestamp);
            System.out.println("✅ fall_status.json 업데이트 완료");
            
            response.put("success", true);
            response.put("fall_num", savedFall.getFallNum());
            response.put("message", "낙상 이벤트 처리 완료");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ 낙상 이벤트 처리 실패: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "처리 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * fall_status.json 파일 업데이트
     */
    private void updateFallStatusFile(Integer fallNum, String bedId, String imageName, String timestamp) {
        try {
            Map<String, Object> fallStatus = new HashMap<>();
            fallStatus.put("fall", true);
            fallStatus.put("fall_num", fallNum);
            fallStatus.put("room", bedId);
            fallStatus.put("image", imageName);
            fallStatus.put("timestamp", timestamp);
            
            // JSON 문자열 생성
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"fall\": true,\n");
            json.append("  \"fall_num\": ").append(fallNum).append(",\n");
            json.append("  \"room\": \"").append(bedId).append("\",\n");
            json.append("  \"image\": \"").append(imageName).append("\",\n");
            json.append("  \"timestamp\": \"").append(timestamp).append("\"\n");
            json.append("}");
            
            // 파일 쓰기
            Files.write(Paths.get(FALL_STATUS_FILE), json.toString().getBytes());
            
        } catch (Exception e) {
            System.err.println("❌ fall_status.json 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @PostMapping("/handle/{fallNum}")
    public ResponseEntity<Map<String, Object>> handleFall(@PathVariable Integer fallNum) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FallEntity fall = fallRepository.findById(fallNum).orElse(null);
            
            if (fall == null) {
                response.put("success", false);
                response.put("message", "낙상 기록을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            fall.setHandledAt(LocalDateTime.now());
            fallRepository.save(fall);
            
            response.put("success", true);
            response.put("message", "조치 완료 처리되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<FallEntity>> getRecentFalls(@RequestParam(defaultValue = "10") int limit) {
        try {
            System.out.println("=== /api/fall/recent 호출됨, limit: " + limit + " ===");
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "fallAt"));
            List<FallEntity> falls = fallRepository.findAll(pageRequest).getContent();
            System.out.println("조회된 낙상 데이터 개수: " + falls.size());
            if (!falls.isEmpty()) {
                System.out.println("첫 번째 데이터: " + falls.get(0));
            }
            // 캐시 방지 헤더 추가
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(falls);
        } catch (Exception e) {
            System.err.println("낙상 데이터 조회 중 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFallStatus() {
        try {
            // fall_status.json 파일 경로 (프로젝트 루트)
            File fallStatusFile = new File("fall_status.json");
            
            if (!fallStatusFile.exists()) {
                // 파일이 없으면 기본값 반환
                Map<String, Object> defaultStatus = new HashMap<>();
                defaultStatus.put("fall", false);
                defaultStatus.put("fall_num", 0);
                defaultStatus.put("room", "");
                defaultStatus.put("image", "");
                defaultStatus.put("timestamp", "");
                return ResponseEntity.ok(defaultStatus);
            }
            
            // JSON 파일을 문자열로 읽어서 직접 파싱
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fallStatusFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            
            String jsonString = content.toString();
            System.out.println("📄 fall_status.json 읽음: " + jsonString);
            
            // 간단한 JSON 파싱 (정규식 사용)
            Map<String, Object> fallStatus = new HashMap<>();
            
            Integer currentFallNum = 0;
            // "fall_num": 숫자
            String fallNumPattern = "\"fall_num\":\\s*(\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(fallNumPattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                fallStatus.put("fall_num", Integer.parseInt(matcher.group(1)));
                currentFallNum = Integer.parseInt(matcher.group(1));
            } else {
                fallStatus.put("fall_num", 0);
            }
            fallStatus.put("fall_num", currentFallNum);
            
            // "fall": true 또는 false
            if (jsonString.contains("\"fall\": true")) {
                fallStatus.put("fall", true);
            } else {
                fallStatus.put("fall", false);
            }
            
            // "room": "값"
            String roomPattern = "\"room\":\\s*\"([^\"]*)\"";
            pattern = java.util.regex.Pattern.compile(roomPattern);
            matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                fallStatus.put("room", matcher.group(1));
            } else {
                fallStatus.put("room", "");
            }
            
            // "image": "값"
            String imagePattern = "\"image\":\\s*\"([^\"]*)\"";
            pattern = java.util.regex.Pattern.compile(imagePattern);
            matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                fallStatus.put("image", matcher.group(1));
            } else {
                fallStatus.put("image", "");
            }
            
            // "timestamp": "값"
            String timestampPattern = "\"timestamp\":\\s*\"([^\"]*)\"";
            pattern = java.util.regex.Pattern.compile(timestampPattern);
            matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                fallStatus.put("timestamp", matcher.group(1));
            } else {
                fallStatus.put("timestamp", "");
            }
            
            // 개발을 위해 설정 off
			/*
			 * if (currentFallNum > 0) { FallEntity fallEntity =
			 * fallRepository.findById(currentFallNum).orElse(null); if (fallEntity != null
			 * && fallEntity.getHandledAt() != null) { // DB에 조치 시간(handledAt)이 기록되어 있다면 이미
			 * 확인된 상황임 // 프론트엔드 팝업이 뜨지 않도록 fall 값을 false로 강제 변경 fallStatus.put("fall",
			 * false); System.out.println("🚩 이미 조치된 낙상 건입니다 (fall_num: " + currentFallNum +
			 * "). 팝업을 띄우지 않습니다."); } }
			 */
            
            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(fallStatus);
                
        } catch (IOException e) {
            System.err.println("❌ fall_status.json 읽기 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
