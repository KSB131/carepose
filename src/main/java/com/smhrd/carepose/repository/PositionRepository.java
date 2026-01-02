package com.smhrd.carepose.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smhrd.carepose.entity.PositionEntity;

@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    // 필요한 경우 특정 환자 ID로 조회하는 메서드 추가
}