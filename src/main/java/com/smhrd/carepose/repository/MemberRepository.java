package com.smhrd.carepose.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smhrd.carepose.entity.MemberEntity;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String> {

	public MemberEntity findByUsernameAndPassword(String username, String password);

}
