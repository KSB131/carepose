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
   
   @GetMapping("/")
   public String home(){
       return "login";   // login.html 로 이동
   }
   
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
   
// 1. 관리자 페이지 조회
@GetMapping("/manager")
public String manager(Model model, HttpServletRequest request) {
    model.addAttribute("requestURI", request.getRequestURI());
    
    // Service가 없으므로 repository를 직접 호출합니다.
    // 메서드 이름은 Repository에 정의한 findByRoleIsNull()을 사용해야 합니다.
    List<MemberEntity> pendingMembers = memberRepository.findByRoleIsNull();
    
    model.addAttribute("pendingMembers", pendingMembers);
    
    return "manager";
}

// 2. 직원 승인 로직
@GetMapping("/manager/approve")
public String approveMember(String username, String role) {
    // 1. username으로 해당 사용자 정보 가져오기
    MemberEntity member = memberRepository.findById(username).orElse(null);
    
    if (member != null) {
        // 2. role(권한) 업데이트 (전달받은 nurse 또는 caregiver 저장)
        member.setRole(role);
        memberRepository.save(member); // DB에 반영
    }
    
    return "redirect:/manager"; // 다시 관리자 페이지로 이동
}

// 3. 직원 삭제 로직
@GetMapping("/manager/delete")
public String deleteMember(String username) {
    // 해당 아이디의 데이터 삭제
    memberRepository.deleteById(username);
    
    return "redirect:/manager"; // 다시 관리자 페이지로 이동
}
}
