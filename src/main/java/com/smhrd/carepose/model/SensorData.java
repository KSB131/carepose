package com.smhrd.carepose.model;

public class SensorData {
    private double temperature;
    private double humidity;
    private double timestamp;
    
    // 기본 생성자
    public SensorData() {}
    
    // 모든 필드를 받는 생성자
    public SensorData(double temperature, double humidity, double timestamp) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }
    
    // Getter, Setter
    public double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    
    public double getHumidity() {
        return humidity;
    }
    
    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }
    
    public double getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
}