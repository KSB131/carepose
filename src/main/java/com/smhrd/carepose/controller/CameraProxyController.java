package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    
    @GetMapping(value = "/video", produces = "multipart/x-mixed-replace; boundary=frame")
    public Flux<DataBuffer> streamVideo() {
        return webClient.get()
                .uri(raspiBaseUrl + "/video_feed")
                .accept(MediaType.parseMediaType("multipart/x-mixed-replace; boundary=frame"))
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnCancel(() -> System.out.println("Streaming canceled by client"))
                .doOnError(e -> System.err.println("Streaming Error: " + e.getMessage()));
    }
}
