package com.smhrd.carepose.controller;

import com.smhrd.carepose.config.TcpServerConfig;
import com.smhrd.carepose.model.SensorData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SensorController {
    
    // 센서 데이터 API (AJAX 요청용)
    @GetMapping("/api/sensor")
    public SensorData getSensorData() {
        return TcpServerConfig.getLatestSensorData();
    }
}