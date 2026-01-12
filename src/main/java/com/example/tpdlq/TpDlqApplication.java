package com.example.tpdlq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TpDlqApplication {

 public static void main(String[] args) {
        SpringApplication.run(TpDlqApplication.class, args);
    }
}
