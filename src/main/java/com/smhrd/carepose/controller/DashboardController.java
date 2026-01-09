package com.smhrd.carepose.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.repository.PatientRepository;
import com.smhrd.carepose.repository.PositionRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {
	
	@Autowired
	private PositionRepository positionRepository;
	
	private final PatientRepository patientRepository;
	
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
        
        // DB에서 총 환자 수 조회
        long totalPatientCount = patientRepository.count();
        model.addAttribute("totalPatientCount", totalPatientCount);

        // DB에서 정상/주의/위험 시간 환자 수 조회
        LocalDateTime now = LocalDateTime.now();
        	
        	// 정상 (남은시간 - 1 ~ 2시간)
	        LocalDateTime normal = now.minusMinutes(60);
	        // 주의 (남은시간 - 10분 ~ 1시간)
	        LocalDateTime caution = now.minusMinutes(110);
	        // 위험 (남은시간 - 0 ~ 10분)
	        LocalDateTime danger = now.minusMinutes(120);
	        
	        long normalPatientCount = positionRepository.countByLastPositionTimeAfter(normal);
	        long cautionPatientCount = positionRepository.countByLastPositionTimeBetween(caution, normal);
	        long dangerPatientCount = positionRepository.countByLastPositionTimeBetween(danger, caution);
	
	        model.addAttribute("normalPatientCount", normalPatientCount);
	        model.addAttribute("cautionPatientCount", cautionPatientCount);
	        model.addAttribute("dangerPatientCount", dangerPatientCount);

        
        return "dashboard";
    }


}
