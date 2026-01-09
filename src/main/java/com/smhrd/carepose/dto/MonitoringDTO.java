package com.smhrd.carepose.dto;

import java.time.LocalDateTime;

public record MonitoringDTO(
    String patientId,
    String lastPosition,
    LocalDateTime lastPositionTime
) {}