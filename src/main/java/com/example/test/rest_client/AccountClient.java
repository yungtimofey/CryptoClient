package com.example.test.rest_client;

import com.example.test.model.FileDTO;

import java.util.Set;

public interface AccountClient {
    String getName(String token);

    Set<FileDTO> getUploadedFiles(String token);

    Set<FileDTO> getDownloadedFiles(String token);
}
