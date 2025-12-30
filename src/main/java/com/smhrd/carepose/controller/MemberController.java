package com.smhrd.carepose.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smhrd.carepose.repository.MemberRepository;
import com.smhrd.carepose.entity.MemberEntity;   // ⭐ 이거 중요

@Controller
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
                                  @RequestParam String pwConfirm,
                                  @RequestParam String roomAuthority,
                                  Model model) {

        if(!password.equals(pwConfirm)){
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
}
