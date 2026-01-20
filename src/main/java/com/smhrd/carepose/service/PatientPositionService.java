package com.smhrd.carepose.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PositionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientPositionService {

    private final PositionRepository repository;

    @Transactional
    public void updatePosition(String patientId, String position, LocalDateTime time) {

        PositionEntity entity = repository.findByPatientId(patientId);

        if (entity == null) {
            entity = PositionEntity.builder()
                    .patientId(patientId)
                    .lastPosition(position)
                    .lastPositionTime(time)
                    .build();
        } else {
            entity.setLastPosition(position);
            entity.setLastPositionTime(time);
        }

        repository.save(entity);
    }
}
