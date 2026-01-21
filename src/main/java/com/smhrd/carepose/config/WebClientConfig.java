package com.smhrd.carepose.config;

import javax.net.ssl.SSLException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient; // 반드시 이 경로여야 합니다!

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() throws SSLException {
        // 1. SSL 검증을 무시하는 SslContext 생성 (Insecure)
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        // 2. Netty HttpClient에 SSL 설정 적용
        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));
        

        // 3. WebClient 빌더에 위에서 만든 HttpClient 연결
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}