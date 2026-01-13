package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AlertsController {

	@Autowired
	MemberRepository memberRepository;
	
	// 알림센터 페이지 조회
	@GetMapping("/alerts")
	public String manager(Model model, HttpServletRequest request) {
	    model.addAttribute("requestURI", request.getRequestURI());
	    
	    return "alerts";
	}
	
}
