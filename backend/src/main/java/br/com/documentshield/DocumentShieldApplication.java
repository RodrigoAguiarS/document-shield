package br.com.documentshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class DocumentShieldApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentShieldApplication.class, args);
    }
}
