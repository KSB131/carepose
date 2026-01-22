package com.smhrd.carepose.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.entity.PositionEntity;
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
	    model.addAttribute("currentPage", "dashboard");
	    model.addAttribute("requestURI", request.getRequestURI());

	    List<PatientEntity> patients = patientRepository.findAll();
	    model.addAttribute("patients", patients);

	    if (!patients.isEmpty()) {
	        model.addAttribute("patient", patients.get(0));
	    }
	    
	    // 1. 전체 환자 수 조회
	    List<PositionEntity> allPositions = positionRepository.findAllWithPatient();
	    model.addAttribute("totalPatientCount", allPositions.size());

	    LocalDateTime now = LocalDateTime.now();

	    // 2. 남은 시간 기준 카운팅 (방법 2 로직)
	    long normalCount = 0;
	    long cautionCount = 0;
	    long dangerCount = 0;

	    for (PositionEntity pos : allPositions) {
	        if (pos.getLastPositionTime() == null) continue;

	        // 등급별 기준 시간(분) 설정
	        int thresholdMinutes;
	        switch (pos.getPatient().getGrade()) {
	            case 1: thresholdMinutes = 105; break; // 1시간 45분
	            case 2: thresholdMinutes = 90;  break; // 1시간 30분
	            case 3: thresholdMinutes = 80;  break; // 1시간 20분
	            default: thresholdMinutes = 120; break; // grade 0 또는 기타
	        }

	        // 경과 시간 계산
	        long minutesElapsed = java.time.Duration.between(pos.getLastPositionTime(), now).toMinutes();
	        // 남은 시간 계산
	        long minutesRemaining = thresholdMinutes - minutesElapsed;

	        // 방법 2 기준 적용
	        if (minutesRemaining > 60) {
	            normalCount++;    // 남은시간 1시간 초과 (정상)
	        } else if (minutesRemaining > 10) {
	            cautionCount++;   // 남은시간 10분 ~ 60분 (주의)
	        } else {
	            dangerCount++;    // 남은시간 10분 미만 (위험 - 0분 이하 포함)
	        }
	    }

	    model.addAttribute("normalPatientCount", normalCount);
	    model.addAttribute("cautionPatientCount", cautionCount);
	    model.addAttribute("dangerPatientCount", dangerCount);

	    return "dashboard";
	}


}
