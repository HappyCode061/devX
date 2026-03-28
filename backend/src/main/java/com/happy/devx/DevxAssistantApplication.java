package com.happy.devx;

import com.happy.devx.ingestion.config.IngestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IngestionProperties.class)
public class DevxAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevxAssistantApplication.class, args);
    }
}
