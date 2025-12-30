package com.smhrd.carepose.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
	public class HomeController {

	    @GetMapping("/")
	    public String home(){
	        return "login";   // login.html 로 이동
	    }
	}


