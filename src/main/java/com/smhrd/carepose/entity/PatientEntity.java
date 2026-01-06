package com.smhrd.carepose.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Builder
@Getter
@Setter
@Table(name = "patient") // DB 테이블명
public class PatientEntity {

	
	@OneToOne(mappedBy = "patient", cascade = CascadeType.ALL)
	private PositionEntity position;

	
	  @Id
	    @Column(name="patient_id")
	    private String patientId;

	    private String name;

	    private Integer grade;

	    @Column(name="ulcer_location")
	    private String ulcerLocation;

	    @Column(name="device_id")
	    private String deviceId;

	    @Column(name="device_status")
	    private String deviceStatus;

	    @Column(name="room_bed")
	    private String roomBed;
	}