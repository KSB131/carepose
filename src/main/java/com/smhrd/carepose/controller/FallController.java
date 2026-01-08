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
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "fallAt"));
            List<FallEntity> falls = fallRepository.findAll(pageRequest).getContent();
            return ResponseEntity.ok(falls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
