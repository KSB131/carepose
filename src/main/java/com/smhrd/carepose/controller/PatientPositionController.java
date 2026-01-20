package com.smhrd.carepose.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.carepose.dto.PatientPositionUpdateRequest;
import com.smhrd.carepose.service.PatientPositionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/patient")
public class PatientPositionController {

    private final PatientPositionService patientPositionService;

    @PostMapping("/position")
    public ResponseEntity<Void> updatePosition(
            @RequestBody PatientPositionUpdateRequest request
    ) {
        patientPositionService.updatePosition(
                request.getPatientId(),
                request.getPosition(),
                request.getTime()
        );
        return ResponseEntity.ok().build();
    }
}
