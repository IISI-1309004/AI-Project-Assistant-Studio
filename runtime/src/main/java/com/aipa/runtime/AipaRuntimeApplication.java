package com.aipa.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AipaRuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AipaRuntimeApplication.class, args);
    }
}
