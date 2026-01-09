package com.smhrd.carepose.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PositionRepository;

@Component
public class PositionScheduler {

    @Autowired
    private PositionRepository positionRepository;

    // ⏱ 1분마다 자동 실행
    @Scheduled(fixedRate = 1000)
    public void autoUpdatePositionTime() {
    	
		/* System.out.println("⏰ Scheduler 실행됨: " + LocalDateTime.now()); */

        List<PositionEntity> list = positionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (PositionEntity pos : list) {
            if (pos.getLastPositionTime() == null) continue;

            long minutes =
                ChronoUnit.MINUTES.between(pos.getLastPositionTime(), now);
            
			/*
			 * System.out.println( "환자 " + pos.getPatientId() + " 경과분: " + minutes );
			 */

            // ✅ 2시간 초과 시 자동 갱신
            if (minutes >= 120) {
                pos.setLastPositionTime(now);
                positionRepository.save(pos);

                System.out.println("⏱ 자동 갱신: " + pos.getPatientId());
            }
        }
    }
}
