package com.smhrd.carepose.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PatientRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MonitoringController {

   @Autowired
   private PatientRepository patientRepository;

   @GetMapping("/monitoring")
   public String monitoringPage(Model model, HttpServletRequest request) {
       List<PatientEntity> patientList = patientRepository.findAll();
       model.addAttribute("patients", patientList);

       // 카메라 데이터가 있다면 추가 가능
       model.addAttribute("cameras", null);

       // 사이드바 active 처리용
       model.addAttribute("requestURI", request.getRequestURI());

       return "monitoring";
   }
   
   


   
}
