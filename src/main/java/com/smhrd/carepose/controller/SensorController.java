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
    
	// 🔥 최근 센서값 하나만 보관
    private static SensorData latestSensorData =
        new SensorData("601A", 0, 0, 0);
	
    @PostMapping("/api/sensor")
    public ResponseEntity<Void> receiveSensor(@RequestBody SensorDto dto) {

        latestSensorData = new SensorData(
            dto.getBedId(),
            dto.getTemperature(),
            dto.getHumidity(),
            dto.getTimestamp()
        );

        log.info("🌡️ sensor update - bedId={}, temp={}, hum={}",
            dto.getBedId(),
            dto.getTemperature(),
            dto.getHumidity()
        );

        return ResponseEntity.ok().build();
    }

    // ⭐ 대시보드 → GET
    @GetMapping("/api/sensor")
    public ResponseEntity<SensorData> getSensor() {
        return ResponseEntity.ok(latestSensorData);
    }
}