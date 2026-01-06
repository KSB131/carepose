package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.PatientRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/patients")
public class PatientController {

    private final PatientRepository patientRepository;


    /** 목록 */
    @GetMapping
    public String list(Model model){

        model.addAttribute("patients", patientRepository.findAll());

        return "patients";
    }


    /** 저장 (등록 + 수정 공용) */
    @PostMapping
    public String save(PatientEntity patient){

        // **PK = room_bed**
        patient.setPatientId(patient.getRoomBed());

        // 기본값 처리
        if(patient.getGrade() == null){
            patient.setGrade(0);
        }

        if(patient.getDeviceStatus() == null || patient.getDeviceStatus().isEmpty()){
            patient.setDeviceStatus("미연결");
        }

        patientRepository.save(patient);

        return "redirect:/patients";
    }


    /** 수정폼 */
    @GetMapping("/{id}")
    public String edit(@PathVariable String id, Model model){

        model.addAttribute(
                "patient",
                patientRepository.findById(id).orElse(new PatientEntity())
        );

        return "patients/patient-form";
    }


    /** 삭제 */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id){

        patientRepository.deleteById(id);

        return "redirect:/patients";
    }
}
