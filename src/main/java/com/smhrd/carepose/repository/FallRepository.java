package com.smhrd.carepose.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smhrd.carepose.entity.FallEntity;

@Repository
public interface FallRepository extends JpaRepository<FallEntity, Integer> {
}
