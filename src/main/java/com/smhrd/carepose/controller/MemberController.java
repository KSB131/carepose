package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smhrd.carepose.repository.MemberRepository;
import com.smhrd.carepose.entity.MemberEntity;   // ⭐ 이거 중요
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberController {

    @Autowired
    MemberRepository memberRepository;

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerProcess(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String passwordConfirm,
                                  @RequestParam String roomAuthority,
                                  Model model) {

        if(!password.equals(passwordConfirm)){
            model.addAttribute("error","비밀번호 확인이 일치하지 않습니다.");
            return "register";
        }

        MemberEntity member = MemberEntity.builder()
                .username(username)
                .password(password)
                .roomAuthority(roomAuthority)
                .build();

        memberRepository.save(member);

        return "redirect:/login";
    }
    
    @GetMapping("/api/check-username")
    @ResponseBody
    public boolean checkUsername(@RequestParam String username) {
       return memberRepository.existsByUsername(username);
    }
    
    // 로그인 로직 처리 (get)
    @GetMapping("/login")
    public String loginPage() {
    	return "login";
    }
    

    
    // 로그인 로직 처리 (post)
    @PostMapping("/login")
    public String loginProcess(@RequestParam String username,
	                           @RequestParam String password,
	                           HttpSession session,
	                           Model model) {
       // DB에서 사용자 정보 조회
       MemberEntity member = memberRepository.findByUsernameAndPassword(username, password);
       
       if (member == null) {
           model.addAttribute("error", "사번 또는 비밀번호가 일치하지 않습니다.");
           return "login";
       }
       
       // 세션에 로그인 정보 저장
       session.setAttribute("user", member);
       session.setAttribute("role", member.getRole());
       // 담당 병동/병실에(roomAuthority) 따른 페이지 분기
       String authority = member.getRoomAuthority();
       
       String role = member.getRole();
       
       if ("nurse".equals(role)) {
    	   return "redirect:/dashboard";
       } else if ("caregiver".equals(role)) {
    	   return "redirect:/rooms";
       } else if ("manager".equals(role)) {
    	   return "redirect:/manager";
       } else {
    	   return "redirect:/login";
       }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
       session.invalidate();
       return "redirect:/login";
    }
    
    
}
