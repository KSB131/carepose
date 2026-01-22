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

	@Value("${raspi1.base-url}")
    private String raspi1BaseUrl; // video용

    @Value("${raspi2.base-url}")
    private String raspi2BaseUrl; // capture용

    private final WebClient webClient;

    public CameraProxyController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping(value = "/frame", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<byte[]> getFrame() {
        return webClient.get()
                .uri(raspi2BaseUrl + "/frame")
                .retrieve()
                .bodyToMono(byte[].class);
    }
    
    @GetMapping(value = "/capture")
    public ResponseEntity<StreamingResponseBody> streamCapture() {

        StreamingResponseBody responseBody = outputStream -> {
            webClient.get()
                    .uri(raspi2BaseUrl + "/capture_feed")
                    .accept(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .doOnNext(dataBuffer -> {
                        try {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            outputStream.write(bytes);
                            outputStream.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            DataBufferUtils.release(dataBuffer);
                        }
                    })
                    .blockLast();
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                .body(responseBody);
    }
    
    @GetMapping(value = "/video")
    public ResponseEntity<StreamingResponseBody> streamVideo() {

        StreamingResponseBody responseBody = outputStream -> {
            webClient.get()
                    .uri(raspi1BaseUrl + "/video_feed")
                    .accept(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .doOnNext(dataBuffer -> {
                        try {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            outputStream.write(bytes);
                            outputStream.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            DataBufferUtils.release(dataBuffer);
                        }
                    })
                    .blockLast();
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                .body(responseBody);
    }
}