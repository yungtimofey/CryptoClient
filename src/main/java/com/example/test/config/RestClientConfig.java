package com.example.test.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

public final class RestClientConfig {
    private static final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofMinutes(3))
            .build();

    public static RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private RestClientConfig() {

    }
}
