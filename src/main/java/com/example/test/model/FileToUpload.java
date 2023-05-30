package com.example.test.model;

import com.company.crypto.aesImpl.mode.SymmetricalBlockMode;
import lombok.Data;

import java.io.File;

@Data
public class FileToUpload {
    private File file;
    private String fileName;
    private SymmetricalBlockMode mode;
    private byte[] iv;
    private int indexForCTR;
    private byte[] hash;
    private byte[] key;
}
