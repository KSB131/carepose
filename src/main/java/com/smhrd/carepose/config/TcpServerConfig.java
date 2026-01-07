package com.smhrd.carepose.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
@Component
public class TcpServerConfig implements CommandLineRunner {
    
    private static final int PORT = 5001;
    private final tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
    
    // 최신 센서 데이터를 저장할 static 변수
    private static com.smhrd.carepose.model.SensorData latestSensorData = new com.smhrd.carepose.model.SensorData(0, 0, 0);
    
    @Override
    public void run(String... args) {
        new Thread(this::startServer).start();
    }
    
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("TCP 서버 시작됨 - 포트: {}", PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("클라이언트 연결됨: {}", clientSocket.getInetAddress());
                
                // 각 클라이언트를 별도 스레드에서 처리
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            log.error("서버 에러: ", e);
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("수신 데이터: {}", line);
                
                // JSON 파싱
                com.smhrd.carepose.model.SensorData sensorData = objectMapper.readValue(line, com.smhrd.carepose.model.SensorData.class);
                
                // 최신 데이터 저장
                latestSensorData = sensorData;
                
                // 데이터 직접 처리
                log.info("온도: {}°C, 습도: {}%", 
                    sensorData.getTemperature(), 
                    sensorData.getHumidity());
                
                // 필요한 로직 여기에 추가
                // 예: 이상 온도 감지
                if (sensorData.getTemperature() > 30) {
                    log.warn("높은 온도 감지: {}°C", sensorData.getTemperature());
                }
            }
        } catch (Exception e) {
            log.error("클라이언트 처리 에러: ", e);
        } finally {
            try {
                clientSocket.close();
            } catch (Exception e) {
                log.error("소켓 닫기 에러: ", e);
            }
        }
    }
    
    // 최신 센서 데이터를 반환하는 메서드
    public static com.smhrd.carepose.model.SensorData getLatestSensorData() {
        return latestSensorData;
    }
}