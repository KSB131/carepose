package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smhrd.carepose.entity.FallEntity;
import com.smhrd.carepose.repository.FallRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fall")
public class FallController {
    
    @Autowired
    private FallRepository fallRepository;
    
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
            
            // "fall": true 또는 false
            if (jsonString.contains("\"fall\": true")) {
                fallStatus.put("fall", true);
            } else {
                fallStatus.put("fall", false);
            }
            
            // "fall_num": 숫자
            String fallNumPattern = "\"fall_num\":\\s*(\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(fallNumPattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                fallStatus.put("fall_num", Integer.parseInt(matcher.group(1)));
            } else {
                fallStatus.put("fall_num", 0);
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
