package com.project.grcplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@SpringBootApplication
public class RiskManagement {

	public static void main(String[] args) {
		SpringApplication.run(RiskManagement.class, args);
	}
}
