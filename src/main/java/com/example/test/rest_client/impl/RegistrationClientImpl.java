package com.example.test.rest_client.impl;

import com.example.test.config.RestClientConfig;
import com.example.test.model.RegistrationParams;
import com.example.test.rest_client.RegistrationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RegistrationClientImpl implements RegistrationClient {
    private static final String LOGIN_URL = "http://localhost:8080/api/registration/login";

    private static final RegistrationClient registrationClient = new RegistrationClientImpl();
    public static RegistrationClient getInstance() {
        return registrationClient;
    }

    private final RestTemplate restTemplate;
    private RegistrationClientImpl() {
        restTemplate = RestClientConfig.getRestTemplate();
    }

    @Override
    public String login(RegistrationParams registrationParams) {
        ResponseEntity<String> token = restTemplate.postForEntity(LOGIN_URL, registrationParams, String.class);
        return token.getBody();
    }
}
