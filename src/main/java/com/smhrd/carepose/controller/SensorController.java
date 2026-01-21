package com.smhrd.carepose.controller;

import com.smhrd.carepose.dto.SensorDto;
import com.smhrd.carepose.model.SensorData;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SensorController {
    
    // 🔥 초기값을 null로 설정하여 데이터가 아직 한 번도 안 왔음을 표시
    private static SensorData latestSensorData = null;
    
    @PostMapping("/api/sensor")
    public ResponseEntity<Void> receiveSensor(@RequestBody SensorDto dto) {
        latestSensorData = new SensorData(
            dto.getBedId(),
            dto.getTemperature(),
            dto.getHumidity(),
            dto.getTimestamp()
        );

        log.info("🌡️ sensor update - bedId={}, temp={}, hum={}",
            dto.getBedId(), dto.getTemperature(), dto.getHumidity());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/sensor")
    public ResponseEntity<SensorData> getSensor() {
        // 데이터가 없으면 204 No Content 혹은 null 반환
        if (latestSensorData == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latestSensorData);
    }
}