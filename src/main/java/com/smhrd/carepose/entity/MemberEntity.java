package com.smhrd.carepose.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "member")
public class MemberEntity {

	@Id
	@Column(nullable=false)
	private String username;
	
	@Column(name = "password",nullable=false)
	private String password;
	
	@Column(name = "room_authority",nullable=false)
	private String roomAuthority;

	@Column(name = "role")
	private String role;
	
	public void setRole (String role) {
		this.role = role;
	}
	
}
