package com.okapi.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OkapiAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OkapiAuthApplication.class, args);
    }

}
