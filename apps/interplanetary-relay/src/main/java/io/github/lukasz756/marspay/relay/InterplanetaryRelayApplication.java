package io.github.lukasz756.marspay.relay;

import io.github.lukasz756.marspay.relay.delivery.RelayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(RelayProperties.class)
public class InterplanetaryRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterplanetaryRelayApplication.class, args);
    }
}