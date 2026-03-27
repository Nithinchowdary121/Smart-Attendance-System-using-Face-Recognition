package com.attendance.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync
public class BackendApplication {

	public static void main(String[] args) {
		System.out.println("Backend Application starting...");
		Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
		dotenv.entries().forEach(e -> {
            if (System.getProperty(e.getKey()) == null && System.getenv(e.getKey()) == null) {
                System.setProperty(e.getKey(), e.getValue());
            }
        });
		System.out.println("Environment variables loaded. Launching Spring Boot...");
		SpringApplication.run(BackendApplication.class, args);
		System.out.println("Spring Boot Application context started.");
	}

}
