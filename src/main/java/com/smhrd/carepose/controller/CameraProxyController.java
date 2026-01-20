package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils; // 추가됨
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Mono;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class CameraProxyController {

    @Value("${raspi.base-url}")
    private String raspiBaseUrl;

    private final WebClient webClient;

    public CameraProxyController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping(value = "/frame", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<byte[]> getFrame() {
        return webClient.get()
                .uri(raspiBaseUrl + "/frame")
                .retrieve()
                .bodyToMono(byte[].class);
    }
    
    @GetMapping(value = "/video")
    public ResponseEntity<StreamingResponseBody> streamVideo() {
        // MJPEG 스트림은 연결을 계속 유지해야 하므로 StreamingResponseBody를 사용합니다.
        StreamingResponseBody responseBody = outputStream -> {
            webClient.get()
                    .uri(raspiBaseUrl + "/video_feed")
                    .accept(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .doOnNext(dataBuffer -> {
                        try {
                            // DataBuffer의 내용을 바이트 배열로 읽어 outputStream에 직접 씁니다.
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            outputStream.write(bytes);
                            outputStream.flush(); // 즉시 클라이언트로 전송
                        } catch (IOException e) {
                            throw new RuntimeException("Stream writing failed", e);
                        } finally {
                            // 메모리 누수 방지를 위해 release 합니다.
                            DataBufferUtils.release(dataBuffer);
                        }
                    })
                    .doOnError(e -> System.err.println("WebClient Stream Error: " + e.getMessage()))
                    .blockLast(); // 전체 스트림이 종료될 때까지 연결을 유지합니다.
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                .body(responseBody);
    }
}