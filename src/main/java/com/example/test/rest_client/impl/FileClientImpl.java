package com.example.test.rest_client.impl;

import com.company.crypto.benaloh.algorithm.Benaloh;
import com.example.test.config.RestClientConfig;
import com.example.test.model.FileToUpload;
import com.example.test.rest_client.FileClient;
import com.example.test.model.FileDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class FileClientImpl implements FileClient {
    private static final String GET_ALL_FILES_URL = "http://localhost:8080/api/files/all";
    private static final String SEARCH_FILES_URL = "http://localhost:8080/api/files/search";
    private static final String GET_OPEN_KEY_URL = "http://localhost:8080/api/files/open-key";
    private static final String POST_FILE_URL = "http://localhost:8080/api/files";
    public static final String DOWNLOAD_FILE_URL = "http://localhost:8080/api/files/get";
    public static final String GET_SYMMETRICAL_KEY_URL = "http://localhost:8080/api/files/cipher-key";

    private static final FileClientImpl restClient = new FileClientImpl();
    public static FileClientImpl getInstance() {
        return restClient;
    }

    private final RestTemplate restTemplate;
    private FileClientImpl() {
        restTemplate = RestClientConfig.getRestTemplate();
    }

    @Override
    public String addFile(FileToUpload fileToUpload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(fileToUpload.getFile().getPath()));
        body.add("name", fileToUpload.getFileName());
        body.add("key", Base64.getEncoder().encodeToString(fileToUpload.getKey()));
        body.add("mode", fileToUpload.getMode().name());
        body.add("iv", Base64.getEncoder().encodeToString(fileToUpload.getIv()));
        body.add("hash", Base64.getEncoder().encodeToString(fileToUpload.getHash()));
        body.add("index-for-ctr", fileToUpload.getIndexForCTR());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(
                POST_FILE_URL,
                requestEntity,
                String.class
        ).getBody();
    }

    @Override
    public Set<FileDTO> getAllFiles(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        return restTemplate.exchange(
                GET_ALL_FILES_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Set<FileDTO>>() {
                }
        ).getBody();
    }

    @Override
    public Set<FileDTO> searchFiles(String name, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(SEARCH_FILES_URL)
                .queryParam("name", name);

        return restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Set<FileDTO>>() {
                }
        ).getBody();
    }

    @Override
    public Benaloh.OpenKey getServiceOpenKey(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        return restTemplate.exchange(
                GET_OPEN_KEY_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Benaloh.OpenKey.class
        ).getBody();
    }

    @Override
    public Resource getFile(String fileId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        return restTemplate.exchange(
                DOWNLOAD_FILE_URL + "/" + fileId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Resource.class
        ).getBody();
    }

    @Override
    public String getSymmetricalKey(String fileId, Benaloh.OpenKey key, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(GET_SYMMETRICAL_KEY_URL)
                .queryParam("id", fileId)
                .queryParam("y", key.getY())
                .queryParam("r", key.getR())
                .queryParam("n", key.getN());

        return restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        ).getBody();
    }
}
