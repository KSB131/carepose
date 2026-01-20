package com.smhrd.carepose.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient; // 반드시 reactor.netty 패키지여야 함

@Configuration
public class RestTemplateConfig {

    @Bean
    public WebClient webClient() throws Exception {
        // 1. 라즈베리파이의 사설 HTTPS 인증서를 무조건 신뢰하도록 설정
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        // 2. 위 설정을 적용한 HttpClient 생성
        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext)) // HTTPS 보안 검사 통과 설정
                .keepAlive(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)) // 버퍼 제한 해제
                .build();
    }
}