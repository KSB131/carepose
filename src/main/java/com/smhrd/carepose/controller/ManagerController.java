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
public class ManagerController {

	 @Autowired
	 MemberRepository memberRepository;
	
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
