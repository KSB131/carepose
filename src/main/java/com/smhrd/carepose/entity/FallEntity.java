package com.smhrd.carepose.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "fall")
public class FallEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fall_num")
    private Integer fallNum;
    
    @Column(name = "patient_id", length = 50)
    private String patientId;
    
    @Column(name = "pic_id", length = 50)
    private String picId;
    
    @Column(name = "fall_body", length = 50)
    private String fallBody;
    
    @Column(name = "fall_at")
    private LocalDateTime fallAt;
    
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    
    @Column(name = "checked_id", length = 50)
    private String checkedId;
}
