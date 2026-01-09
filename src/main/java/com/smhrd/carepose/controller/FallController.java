package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smhrd.carepose.entity.FallEntity;
import com.smhrd.carepose.repository.FallRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
}
