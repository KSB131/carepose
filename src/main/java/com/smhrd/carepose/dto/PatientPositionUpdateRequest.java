package com.smhrd.carepose.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientPositionUpdateRequest {
    private String patientId;   // 603F
    private String position;    // 좌측위
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time; // 저장 시간
}