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
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "patient") // DB 테이블명
public class PatientEntity {

    @Id
    @Column(name = "patient_id", length = 50)
    private String patientId; // 환자 고유 번호 (PK)

    @Column(name = "name", nullable = false)
    private String name;


    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL)
    private PositionEntity position;
    
    
    
}