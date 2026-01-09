package com.smhrd.carepose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CareposeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareposeApplication.class, args);
	}

}
