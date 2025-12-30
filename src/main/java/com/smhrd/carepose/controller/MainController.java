package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.repository.MemberRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
	
	
	@Autowired
	MemberRepository memberRepository;

	@GetMapping("/nursmain")
		public String nursmain() {
	return "monitoring";
	}

	@GetMapping("/caremain")
	public String caremain() {
		return "dashboard";
	}
	
	@PostMapping("/login")
	public String login(MemberEntity entity , HttpSession session) {
		entity = memberRepository.findByUsernameAndPassword(entity.getUsername(),entity.getPassword());
		
		if(entity !=null) {
			session.setAttribute("entity", entity);					
		}
	return "redirect:/";
	}
	
	
	
	
	
	
	
}
