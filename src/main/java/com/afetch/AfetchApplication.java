package com.afetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AfetchApplication {

    private static final Logger log = LoggerFactory.getLogger(AfetchApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(AfetchApplication.class, args);
        log.info("✓ AfetchApplication started successfully. Spring context id={}", ctx.getId());
    }
}
