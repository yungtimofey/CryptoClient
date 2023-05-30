package com.example.test.rest_client.impl;

import com.example.test.model.FileDTO;
import com.example.test.rest_client.AccountClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

public class AccountClientImpl implements AccountClient {
    private static final String GET_NAME_URL = "http://localhost:8080/api/accounts/info";
    private static final String GET_UPLOADED_FILES_URL = "http://localhost:8080/api/accounts/files-uploaded";
    private static final String GET_DOWNLOADED_FILES_URL = "http://localhost:8080/api/accounts/files-downloaded";

    private static final AccountClient accountClient = new AccountClientImpl();

    public static AccountClient getInstance() {
        return accountClient;
    }

    private final RestTemplate restTemplate;

    private AccountClientImpl() {
        restTemplate = new RestTemplate();
    }

    @Override
    public String getName(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(GET_NAME_URL, HttpMethod.GET, requestEntity, String.class);
        return response.getBody();
    }

    @Override
    public Set<FileDTO> getUploadedFiles(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        return restTemplate.exchange(
                GET_UPLOADED_FILES_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Set<FileDTO>>() {
                }
        ).getBody();
    }

    @Override
    public Set<FileDTO> getDownloadedFiles(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        return restTemplate.exchange(
                GET_DOWNLOADED_FILES_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Set<FileDTO>>() {
                }
        ).getBody();
    }
}
