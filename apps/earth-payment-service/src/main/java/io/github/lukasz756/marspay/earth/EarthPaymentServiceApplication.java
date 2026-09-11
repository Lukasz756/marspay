package io.github.lukasz756.marspay.earth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EarthPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                EarthPaymentServiceApplication.class,
                args
        );
    }
}