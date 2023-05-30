package com.example.test.model;

import com.company.crypto.aesImpl.mode.SymmetricalBlockMode;
import javafx.scene.control.ProgressBar;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class FileInTable {
    public FileInTable(FileDTO fileDTO) {
        this.id = fileDTO.getId();
        this.name = fileDTO.getName();
        this.bytes = fileDTO.getBytes();
        this.addDate = fileDTO.getAddDate();
        this.encodedSymmetricalKey = fileDTO.getEncodedSymmetricalKey();
        this.iv = fileDTO.getIv();
        this.hash = fileDTO.getHash();
        this.indexForCTR = fileDTO.getIndexForCTR();
        this.mode = fileDTO.getMode();
    }

    public FileInTable(FileToUpload fileToUpload) {
        this.name = fileToUpload.getFileName();
        this.bytes = fileToUpload.getFile().length();

        this.setProgress(0.0);
        this.setStatus(FileProgressStatus.WAITING);
    }

    public FileInTable(FileInTable fileInTable) {
        this.id = fileInTable.id;
        this.name = fileInTable.name;
        this.bytes = fileInTable.bytes;
        this.addDate = fileInTable.addDate;
        this.encodedSymmetricalKey = fileInTable.encodedSymmetricalKey;
        this.iv = fileInTable.iv;
        this.hash = fileInTable.hash;
        this.indexForCTR = fileInTable.indexForCTR;
        this.mode = fileInTable.mode;
    }

    private String id;
    private String name;
    private Long bytes;
    private LocalDate addDate;
    private String encodedSymmetricalKey;
    private String iv;
    private String hash;
    private int indexForCTR;
    private SymmetricalBlockMode mode;

    private FileProgressStatus status = FileProgressStatus.OK;
    private double progress = 1.0;

    public ProgressBar getProgressBar() {
        return new ProgressBar(progress);
    }
}
