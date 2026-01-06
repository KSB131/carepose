package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.MemberRepository;
import com.smhrd.carepose.repository.PatientRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
   
   
   @Autowired
   MemberRepository memberRepository;
   
   @Autowired
   PatientRepository patientRepository;

   @GetMapping("/dashboard")
   public String nursmain() {
      return "dashboard";
   }
   
   
   @GetMapping("/login")
   public String loginPage() {
      return "login";
   }
   
   @GetMapping("/rooms")
   public String roomSelect(Model model) {
	   List<PatientEntity> patients = patientRepository.findAll();
	   model.addAttribute("patients", patients);
	   return "roomSelect";
   }
   
}
