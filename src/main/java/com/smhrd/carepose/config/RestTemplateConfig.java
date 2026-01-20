package com.smhrd.carepose.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient; // 반드시 reactor.netty 패키지여야 함

@Configuration
public class RestTemplateConfig {

	@Bean
	public WebClient webClient() throws Exception {
	    SslContext sslContext = SslContextBuilder
	            .forClient()
	            .trustManager(InsecureTrustManagerFactory.INSTANCE)
	            .build();

	    HttpClient httpClient = HttpClient.create()
	            .secure(t -> t.sslContext(sslContext))
	            .responseTimeout(Duration.ofMinutes(30)) // 응답 대기 시간 대폭 연장
	            .keepAlive(true);

	    return WebClient.builder()
	            .clientConnector(new ReactorClientHttpConnector(httpClient))
	            // 모든 사이즈의 데이터를 허용하도록 설정
	            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)) 
	            .build();
	}
}