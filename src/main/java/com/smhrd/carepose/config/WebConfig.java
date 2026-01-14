package com.smhrd.carepose.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**") // 모든 경로에 대해 검사 수행
                .excludePathPatterns(
                    "/login", "/register", "/logout", // 로그인/가입 관련 제외
                    "/css/**", "/js/**", "/images/**", "/device/**", // 정적 리소스 제외
                    "/api/check-username" // 중복 체크 API 제외
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///C:/carepose-images/images/")
                .setCachePeriod(0); // 실시간 반영
    }
}