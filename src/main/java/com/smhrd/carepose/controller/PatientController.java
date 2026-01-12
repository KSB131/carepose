package com.smhrd.carepose.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.smhrd.carepose.CareposeApplication;
import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.PatientRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/patients")
public class PatientController {

    private final CareposeApplication careposeApplication;

    private final PatientRepository patientRepository;




    /** 등록 처리 */
    @PostMapping("/register")
    public String register(PatientEntity patient) {
        patient.setPatientId(patient.getPatientId());
        // ... 기존 기본값 로직
        patientRepository.save(patient);
        return "redirect:/patients";
    }

    /** 수정 처리 */
    @Transactional
    @PostMapping("/update")
    public String update(PatientEntity patient, @RequestParam("oldPatientId") String oldPatientId) {
        
        // 1. ID(PK)가 변경되었는지 확인
        if (!oldPatientId.equals(patient.getPatientId())) {
            // 기존 데이터를 찾아서 확실히 삭제 처리
            PatientEntity existing = patientRepository.findById(oldPatientId).orElse(null);
            if (existing != null) {
                // 연관된 데이터가 있다면 유지하고 싶을 경우 새 객체에 복사
                patient.setDeviceId(existing.getDeviceId());
                patient.setDeviceStatus(existing.getDeviceStatus());
                
                // 기존 데이터 삭제 및 즉시 반영(flush)
                patientRepository.delete(existing);
                patientRepository.flush(); // ★ DB에 삭제 쿼리를 즉시 보냅니다.
            }
            
            // 새 ID를 침상 번호에도 동일하게 적용
            patient.setRoomBed(patient.getPatientId());
            
            // 2. 새 ID로 저장
            patientRepository.save(patient);
        } else {
            // ID가 같다면 기존 필드만 업데이트
            PatientEntity existing = patientRepository.findById(oldPatientId).orElse(null);
            if (existing != null) {
                existing.setName(patient.getName());
                existing.setGrade(patient.getGrade());
                existing.setUlcerLocation(patient.getUlcerLocation());
                patientRepository.save(existing);
            }
        }
        
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
    @Transactional  // 삭제 작업 중 오류 발생 시 롤백 및 연관 데이터 동시 삭제 보장
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable String id){

        patientRepository.deleteById(id);

        return "redirect:/patients";
    }
    
    /*검색*/
    @GetMapping
    public String list(
            @RequestParam(required=false) String keyword,
            Model model, HttpServletRequest request){

        List<PatientEntity> patients;

        if(keyword != null && !keyword.trim().isEmpty()){
            patients = patientRepository.findByPatientIdContaining(keyword);

        } 
        else {
            patients = patientRepository.findAll();
        }

        model.addAttribute("patients", patients);
        model.addAttribute("keyword", keyword);

        // 사이드바 active 처리
        model.addAttribute("requestURI", request.getRequestURI());
        return "patients";
    }

    
    
    
}
