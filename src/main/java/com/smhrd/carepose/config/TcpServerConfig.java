package com.smhrd.carepose.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PositionRepository;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Slf4j
// @Component  // ⚠️ 비활성화: TcpServerConfig_정선주로 통합됨
public class TcpServerConfig implements CommandLineRunner {

    private static final int PORT = 5001;
    private final tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
    
    // 이미지 저장 기본 경로 (src/main/resources/static/images/)
    private final String BASE_PATH = "src/main/resources/static/images/";

    private static com.smhrd.carepose.model.SensorData latestSensorData = new com.smhrd.carepose.model.SensorData(0, 0, 0);

    @Autowired
    private PositionRepository positionRepository;
    
    @Override
    public void run(String... args) {
        new Thread(this::startServer).start();
        
        updatePatientPosition("601D_left1.jpg");
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("TCP 서버 시작됨 - 포트: {}", PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
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
                log.info("데이터 수신됨 (길이: {})", line.length());

                // 1. 우선 Map으로 전체 파싱 (에러 방지용)
                Map<String, Object> dataMap = objectMapper.readValue(line, Map.class);
                
                // 2. 센서 데이터 처리
                if (dataMap.containsKey("temperature")) {
                    double temp = Double.parseDouble(dataMap.get("temperature").toString());
                    double humi = Double.parseDouble(dataMap.get("humidity").toString());
                    latestSensorData = new com.smhrd.carepose.model.SensorData(temp, humi, 0);
                    log.info("센서 업데이트 - 온도: {}°C, 습도: {}%", temp, humi);
                }

                // 3. 이미지 데이터 처리
                if (dataMap.containsKey("imgName") && dataMap.get("imgData") != null) {
                    String imgName = dataMap.get("imgName").toString();
                    String imgData = dataMap.get("imgData").toString();
                    log.info("이미지 데이터 발견: {}", imgName);
                    saveImage(imgName, imgData);
                }
            }
        } catch (Exception e) {
            log.error("클라이언트 처리 에러: {}", e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (Exception e) { }
        }
    }

    private void saveImage(String fileName, String base64Data) {
        try {
            // [중요] 절대 경로로 테스트해보세요. 프로젝트 루트의 upload 폴더로 설정 예시
            // 실제 경로 확인을 위해 절대 경로를 사용해 보는 것이 가장 좋습니다.
            String absolutePath = new File("").getAbsolutePath() + "/src/main/resources/static/images/";
            
            String patientId = fileName.split("_")[0];           
            String roomNum = patientId.substring(0, 3);         
            String originalFileName = fileName.split("_")[1]; 

            Path directoryPath = Paths.get(absolutePath, roomNum, patientId);
            
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                log.info("폴더 생성 성공: {}", directoryPath);
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            File targetFile = new File(directoryPath.toFile(), originalFileName);
            
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(imageBytes);
                fos.flush(); // 버퍼 비우기
            }
            log.info("파일 저장 완료 위치: {}", targetFile.getAbsolutePath());
            
            updatePatientPosition(fileName);

        } catch (Exception e) {
            log.error("이미지 저장 실패 상세: ", e); // 에러 로그를 상세히 찍도록 변경
        }
    }
    
    private void updatePatientPosition(String fileName) {
       
       log.info("🔥 updatePatientPosition 실행됨 - fileName={}", fileName);

        try {
            // 예: 601A_left1.jpg
            String[] parts = fileName.split("_");
            String patientId = parts[0]; // 601A

            String posturePart = parts[1]; // left1.jpg
            String postureKey = posturePart.replaceAll("[0-9]|\\.jpg", ""); // left

            String position;
            switch (postureKey) {
                case "left":
                    position = "좌측위";
                    break;
                case "right":
                    position = "우측위";
                    break;
                case "face":
                    position = "앙와위";
                    break;
                default:
                    log.warn("알 수 없는 자세: {}", postureKey);
                    return;
            }

            LocalDateTime now = LocalDateTime.now();

            // 기존 데이터 있는지 확인
            PositionEntity pos = positionRepository.findByPatientId(patientId);

            if (pos == null) {
                pos = new PositionEntity();
                pos.setPatientId(patientId);
            }

            pos.setLastPosition(position);
            pos.setLastPositionTime(now);

            positionRepository.save(pos);

            log.info("📌 자세 저장 완료 - 환자: {}, 자세: {}, 시간: {}",
                    patientId, position, now);

        } catch (Exception e) {
            log.error("❌ patient_position 저장 실패", e);
        }
    }


    public static com.smhrd.carepose.model.SensorData getLatestSensorData() {
        return latestSensorData;
    }
}