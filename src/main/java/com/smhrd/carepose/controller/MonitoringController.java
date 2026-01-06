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

@Controller
public class MonitoringController {

   @Autowired
   private PatientRepository patientRepository;

   @GetMapping("/monitoring")
   public String dashboard(Model model) {
       List<PatientEntity> patientList = patientRepository.findAll();
       
       model.addAttribute("patients", patientList);
       model.addAttribute("cameras", null);
       return "monitoring";
   }
   
   


   
}
