package com.smhrd.carepose.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.MemberRepository;
import com.smhrd.carepose.repository.PatientRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
   
   
   @Autowired
   MemberRepository memberRepository;
   
   @Autowired
   PatientRepository patientRepository;
   
   @GetMapping("/")
   public String home(){
       return "login";   // login.html 로 이동
   }
   
   @GetMapping("/rooms")
   public String roomSelect(Model model, HttpServletRequest request) {

       // 모든 환자 조회
       List<PatientEntity> patients = patientRepository.findAll();
       model.addAttribute("patients", patients);

       Map<Integer, List<Integer>> roomMap = new LinkedHashMap<>();

       roomMap.put(2, List.of(201, 202, 203));
       roomMap.put(3, List.of(301, 302, 303));
       roomMap.put(5, List.of(501, 502, 503));
       roomMap.put(6, List.of(601, 602, 603));

       model.addAttribute("roomMap", roomMap);
       model.addAttribute("requestURI", request.getRequestURI());

       return "roomSelect";
   }
   
   

}
