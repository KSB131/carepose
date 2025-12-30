package com.smhrd.carepose.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class Patients {
	
	 @GetMapping("/patients")
	    public String patientsPage(@RequestParam(required = false) String keyword,
	                               @RequestParam(required = false) Long roomFilter,
	                               @RequestParam(defaultValue = "0") int page,
	                               Model model) {
	        model.addAttribute("currentPage", "patients");
	        model.addAttribute("keyword", keyword);
	        model.addAttribute("roomFilter", roomFilter);
	        
	        // TODO: 실제 데이터 조회
	        // Page<Patient> patients = patientService.findAll(keyword, roomFilter, page);
	        // model.addAttribute("patients", patients.getContent());
	        // model.addAttribute("totalPages", patients.getTotalPages());
	        // model.addAttribute("currentPage", page);
	        
	        return "patients";
	    }

	    /**
	     * 환자 등록
	     * POST /patients
	     */
	    @PostMapping("/patients")
	    public String createPatient(@RequestParam String bedId,
	                                @RequestParam String name,
	                                @RequestParam Integer pressureUlcerStage,
	                                @RequestParam(required = false) String pressureUlcerArea) {
	        // TODO: 환자 등록 로직
	        // patientService.create(bedId, name, pressureUlcerStage, pressureUlcerArea);
	        
	        return "redirect:/patients";
	    }

}
