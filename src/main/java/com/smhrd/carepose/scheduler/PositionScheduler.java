package com.smhrd.carepose.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PatientRepository;
import com.smhrd.carepose.repository.PositionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
public class PositionScheduler {

    private final PositionRepository positionRepository;
    
    @Autowired
    public PositionScheduler(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    // ⏱ 1초마다 자동 실행
    @Transactional
    @Scheduled(fixedRate = 10000)
    public void autoUpdatePositionTime() {
       
      /* System.out.println("⏰ Scheduler 실행됨: " + LocalDateTime.now()); */

       List<PositionEntity> list = positionRepository.findAllWithPatient();
        LocalDateTime now = LocalDateTime.now();

        for (PositionEntity pos : list) {
            if (pos.getLastPositionTime() == null) continue;

            int grade = pos.getPatient().getGrade();

            // 등급별 기준 시간(분 단위)
            int thresholdSeconds;
            switch (grade) {
               case 1: thresholdSeconds = 105 * 60; break;  // 1시간45분
               case 2: thresholdSeconds = 90 * 60; break;   // 1시간30분
               case 3: thresholdSeconds = 80 * 60; break;   // 1시간20분
               case 4: thresholdSeconds = 15; break;
                default: thresholdSeconds = 120 * 60;      // grade 0 또는 null
            }

            long secondsElapsed =
                    ChronoUnit.SECONDS.between(pos.getLastPositionTime(), now);

            // 기준 시간 이상 경과 시 갱신
            if (secondsElapsed >= thresholdSeconds) {
                pos.setLastPositionTime(now);
                positionRepository.save(pos);
                System.out.println("⏱ 자동 갱신: " +
                    pos.getPatient().getPatientId() + " (grade=" + grade + ")");
            }}
            
        }
    }
