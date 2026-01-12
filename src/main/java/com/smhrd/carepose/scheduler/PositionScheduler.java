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

@Component
public class PositionScheduler {

    @Autowired
    private PositionRepository positionRepository;

    // ⏱ 1초마다 자동 실행
    @Scheduled(fixedRate = 1000)
    public void autoUpdatePositionTime() {
    	
		/* System.out.println("⏰ Scheduler 실행됨: " + LocalDateTime.now()); */

        List<PositionEntity> list = positionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (PositionEntity pos : list) {
            if (pos.getLastPositionTime() == null) continue;

            int grade = pos.getPatient().getGrade();

            // 등급별 기준 시간(분 단위)
            int thresholdMinutes;
            switch (grade) {
                case 1: thresholdMinutes = 105; break; // 1시간 45분
                case 2: thresholdMinutes = 90; break;  // 1시간 30분
                case 3: thresholdMinutes = 80; break;  // 1시간 20분
                default: thresholdMinutes = 120;      // grade 0 또는 null
            }

            long minutesElapsed = ChronoUnit.MINUTES.between(pos.getLastPositionTime(), now);
            
            // 기준 시간 이상 경과 시 갱신
            if (minutesElapsed >= thresholdMinutes) {
                pos.setLastPositionTime(now);
                positionRepository.save(pos);
                System.out.println("⏱ 자동 갱신: " + pos.getPatient().getPatientId() + " (grade=" + grade + ")");
            }
            
        }
    }
}
