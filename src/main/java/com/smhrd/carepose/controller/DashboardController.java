package com.smhrd.carepose.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class DashboardController {
	
	 @GetMapping({"/", "/dashboard"})
	    public String dashboardPage(Model model) {
	        // 현재 페이지 표시 (사이드바 메뉴 활성화용)
	        model.addAttribute("currentPage", "dashboard");
	        
	        // TODO: 실제 데이터 조회
	        // model.addAttribute("totalPatients", patientService.count());
	        // model.addAttribute("rooms", roomService.findAllWithBeds());
	        // model.addAttribute("recentAlerts", alertService.findRecent(5));
	        
	        // 샘플 데이터
	        model.addAttribute("totalPatients", 24);
	        model.addAttribute("normalCount", 18);
	        model.addAttribute("warningCount", 4);
	        model.addAttribute("dangerCount", 2);
	        model.addAttribute("alertCount", 3);
	        
	        return "dashboard";  // templates/dashboard.html
	    }


}
