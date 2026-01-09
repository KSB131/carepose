package com.smhrd.carepose.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "patient_position")
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_num")
    private Long positionNum;
    
    @Column(name = "patient_id", nullable=false, unique = true)
    private String patientId; // Patient 테이블의 PK와 동일한 값

    @Column(name = "last_position")
    private String lastPosition;

    @Column(name = "last_position_time")
    private LocalDateTime lastPositionTime;

    // 환자 정보와 연결 (Optional)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "patient_id",
        referencedColumnName = "patient_id",
        insertable = false,
        updatable = false
    )
    private PatientEntity patient;
    
    
    // monitoring 시간계산
    @Transient   // DB 컬럼 아님
    private Long idleMinutes;

    public Long getIdleMinutes() {
        return idleMinutes;
    }

    public void setIdleMinutes(Long idleMinutes) {
        this.idleMinutes = idleMinutes;
    }
}