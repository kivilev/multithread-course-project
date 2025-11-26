package dev.sorokin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymentStubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentStubApplication.class, args);
    }

}
