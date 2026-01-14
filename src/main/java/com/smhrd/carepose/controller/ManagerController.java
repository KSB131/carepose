package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class ManagerController {

	 @Autowired
	 MemberRepository memberRepository;
	
	// 1. 관리자 페이지 조회 / URL 접근 제한
	 @GetMapping("/manager")
	    public String managerPage(Model model, HttpServletRequest request, HttpSession session) {
	        
	        // [보안 로직] 세션에서 로그인 유저 정보 확인
	        MemberEntity user = (MemberEntity) session.getAttribute("user");

	        // 관리자가 아니면 /rooms로 리다이렉트
	        if (user == null || !"manager".equals(user.getRole())) {
	            return "redirect:/rooms";
	        }

	        // [데이터 로직] 관리자일 경우에만 아래 실행
	        model.addAttribute("requestURI", request.getRequestURI());
	        
	        // 승인 대기 중인(Role이 Null인) 멤버 리스트 조회
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
