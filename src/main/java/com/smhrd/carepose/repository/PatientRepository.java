package com.smhrd.carepose.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smhrd.carepose.entity.PatientEntity;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, String> {
    
	/*
    // 특정 병실/침상 번호로 환자 찾기 예시
    PatientEntity findByPatientId(String patientId);
    
    // 특정 권한(병동)에 속한 환자 전체 목록 가져오기 예시
    // 만약 roomBed가 '602'로 시작하는 환자들을 찾고 싶다면
    List<PatientEntity> findByPatientIdStartingWith(String prefix);
*/
}