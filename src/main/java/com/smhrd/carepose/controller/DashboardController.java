package com.smhrd.carepose.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DashboardController {
	
	@GetMapping({"/dashboard"})
    public String dashboardPage(HttpServletRequest request, Model model) {
        model.addAttribute("currentPage", "dashboard"); // 사이드바 플래그
        model.addAttribute("requestURI", request.getRequestURI());

        // 샘플 데이터
        model.addAttribute("totalPatients", 24);
        model.addAttribute("normalCount", 18);
        model.addAttribute("warningCount", 4);
        model.addAttribute("dangerCount", 2);
        model.addAttribute("alertCount", 3);

        return "dashboard";
    }


}
