package com.example.onlinejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entry point for the Online Java sandbox service.
 */
@SpringBootApplication
@EnableScheduling
public class OnlineJavaApplication {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(OnlineJavaApplication.class, args);

  }

}