package com.smhrd.carepose.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        
        // 세션에 'user'가 없으면 로그인하지 않은 상태
        if (session.getAttribute("user") == null) {
            // 로그인 페이지로 리다이렉트
            response.sendRedirect("/login");
            return false; // 컨트롤러로 요청을 보내지 않음
        }
        
        return true; // 로그인 상태면 컨트롤러로 진행
    }
}