package com.smhrd.carepose.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smhrd.carepose.entity.PositionEntity;

@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    
    // 대시보드 카운트 - 정상(2시간 ~ 1시간)
    long countByLastPositionTimeAfter(LocalDateTime time);
    
    // 대시보드 범위 카운트
    long countByLastPositionTimeBetween(LocalDateTime start, LocalDateTime end);
    
    PositionEntity findByPatientId(String patientId);
    
    @Query("""
    	    select p
    	    from PositionEntity p
    	    join fetch p.patient
    	""")
    	List<PositionEntity> findAllWithPatient();
    
    Optional<PositionEntity> findOptionalByPatientId(String patientId);
}