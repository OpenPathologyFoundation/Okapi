package com.starling.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StarlingAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(StarlingAuthApplication.class, args);
    }

}
