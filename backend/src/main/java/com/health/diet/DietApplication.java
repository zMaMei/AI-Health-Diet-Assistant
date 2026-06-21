package com.health.diet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
/* Spring Boot启动类 */
public class DietApplication {

    /* 应用入口 */
    public static void main(String[] args) {
        SpringApplication.run(DietApplication.class, args);
    }
}
