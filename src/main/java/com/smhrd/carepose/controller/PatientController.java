package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.PatientRepository;

@Controller
public class PatientController {

	@Autowired
	private PatientRepository patientRepository;


	
}
