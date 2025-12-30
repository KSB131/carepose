package com.smhrd.carepose.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder


public class MemberEntity {

	@Id
	@Column(nullable=false)
	private String username;
	
	@Column(name = "password",nullable=false)
	private String password;
	
	@Column(name = "roomAuthority",nullable=false)
	private String roomAuthority;

	
	
	
	
}
