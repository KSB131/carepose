package com.smhrd.carepose.controller;


	import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
public class VideoProxyController {

    // 라즈베리파이 ngrok URL
    private static final String RASPI_STREAM_URL = "https://ectromelic-janette-commendably.ngrok-free.dev/video_feed";
    
    @GetMapping(value = "/video_feed", produces = "multipart/x-mixed-replace; boundary=frame")
    public void streamVideo(HttpServletResponse response) {
        String raspiUrl = RASPI_STREAM_URL;
        
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(raspiUrl).openConnection();
            connection.setRequestMethod("GET");
            // ngrok 경고 페이지 우회
            connection.setRequestProperty("ngrok-skip-browser-warning", "true");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000); // 10초 연결 타임아웃
            connection.setReadTimeout(30000); // 30초 읽기 타임아웃
            connection.connect();
            
            // 응답 헤더 설정
            response.setContentType("multipart/x-mixed-replace; boundary=frame");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Connection", "keep-alive");
            
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    // 클라이언트 연결 체크
                    if (response.isCommitted() && !isClientConnected(response)) {
                        break;
                    }
                    response.getOutputStream().write(buffer, 0, bytesRead);
                    response.getOutputStream().flush();
                }
            }
        } catch (java.io.IOException e) {
            // Broken pipe는 클라이언트가 연결을 끊은 정상 상황
            if (!e.getMessage().contains("Broken pipe") && !e.getMessage().contains("Connection reset")) {
                System.out.println("⚠️ 스트리밍 연결 실패: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ 스트리밍 오류: " + e.getMessage());
        }
    }
    
    private boolean isClientConnected(HttpServletResponse response) {
        try {
            response.getOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}