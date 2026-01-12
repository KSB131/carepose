package com.smhrd.carepose.controller;

import java.util.Comparator;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
   
   
   @Autowired
   MemberRepository memberRepository;
   
   @Autowired
   PatientRepository patientRepository;


   
   
   @GetMapping("/rooms")
   public String roomSelect(Model model, HttpServletRequest request) {
       // 모든 환자 조회
       List<PatientEntity> patients = patientRepository.findAll();
       model.addAttribute("patients", patients);

       // 사진의 배치 순서대로 수동 리스트 생성 (사진에 적힌 빨간 글씨 순서)
       // 201, 301, 501, 601 (첫 줄)
       // 202, 302, 502, 502 (둘째 줄 - 사진 기준)
       // 203, 601, 602, 603 (셋째 줄 - 사진 기준)
       
       // ※ 사진의 중복된 번호들을 정리하여 가장 유사한 순서로 배열합니다.
       List<String> roomList = List.of(
           "201", "301", "501", "601",
           "202", "302", "502", "602", // 사진에 502가 두 번 적혀있어 그대로 반영하거나 수정 필요
           "203", "303", "503", "603"
       );

       model.addAttribute("roomList", roomList);
       model.addAttribute("requestURI", request.getRequestURI());

       return "roomSelect";
   }
}
