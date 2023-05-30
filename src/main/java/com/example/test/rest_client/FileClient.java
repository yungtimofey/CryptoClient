package com.example.test.rest_client;

import com.company.crypto.benaloh.algorithm.Benaloh;
import com.example.test.model.FileDTO;
import com.example.test.model.FileToUpload;
import org.springframework.core.io.Resource;

import java.util.Set;

public interface FileClient {
    String addFile(FileToUpload fileToUpload, String token);

    Set<FileDTO> getAllFiles(String token);

    Set<FileDTO> searchFiles(String name, String token);

    Benaloh.OpenKey getServiceOpenKey(String token);

    Resource getFile(String fileId, String token);

    String getSymmetricalKey(String fileId, Benaloh.OpenKey key, String token);
}
