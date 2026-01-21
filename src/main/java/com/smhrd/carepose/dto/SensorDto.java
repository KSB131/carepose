package com.smhrd.carepose.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorDto {

    private String bedId;
    private double temperature;
    private double humidity;
    private long timestamp;
}
