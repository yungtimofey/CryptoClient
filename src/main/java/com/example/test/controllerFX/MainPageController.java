package com.example.test.controllerFX;

import com.company.crypto.aesImpl.CypherInformant;
import com.company.crypto.aesImpl.SymmetricBlockCypher;
import com.company.crypto.aesImpl.algorithm.SymmetricalBlockEncryptionAlgorithm;
import com.company.crypto.aesImpl.algorithm.impl.RC6Bits32;
import com.company.crypto.aesImpl.mode.SymmetricalBlockMode;
import com.company.crypto.aesImpl.round.impl.RoundKeysGeneratorImpl;
import com.company.crypto.benaloh.algebra.prime.PrimeCheckerType;
import com.company.crypto.benaloh.algorithm.Benaloh;
import com.company.crypto.benaloh.algorithm.impl.BenalohImpl;
import com.example.test.CryptoServiceApplication;
import com.example.test.model.FileDTO;
import com.example.test.model.FileInTable;
import com.example.test.model.FileProgressStatus;
import com.example.test.model.FileToUpload;
import com.example.test.rest_client.AccountClient;
import com.example.test.rest_client.FileClient;
import com.example.test.rest_client.impl.AccountClientImpl;
import com.example.test.rest_client.impl.FileClientImpl;
import com.example.test.token.TokenStorage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class MainPageController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private ChoiceBox<SymmetricalBlockMode> chooseBoxForMode;

    // TextFields
    @FXML
    private TextField searchTextField;
    @FXML
    private TextField fileNameToUploadTextField;

    // Tables
    @FXML
    private TableView<FileInTable> allFilesTable;
    @FXML
    private TableView<FileInTable> uploadedFilesTable;
    @FXML
    private TableView<FileInTable> downloadedFilesTable;

    // Columns
    @FXML
    private TableColumn<?, ?> allFilesBytes;
    @FXML
    private TableColumn<?, ?> allFilesDate;
    @FXML
    private TableColumn<?, ?> allFilesName;

    @FXML
    private TableColumn<?, ?> downloadedFileBytes;
    @FXML
    private TableColumn<?, ?> downloadedFileDate;
    @FXML
    private TableColumn<?, ?> downloadedFileProgress;
    @FXML
    private TableColumn<?, ?> downloadedFileStatus;
    @FXML
    private TableColumn<?, ?> downloadedFilesName;

    @FXML
    private TableColumn<?, ?> uploadedFilesBytes;
    @FXML
    private TableColumn<?, ?> uploadedFilesDate;
    @FXML
    private TableColumn<?, ?> uploadedFilesName;
    @FXML
    private TableColumn<?, ?> uploadedFilesProgress;
    @FXML
    private TableColumn<?, ?> uploadedFilesStatus;

    // Buttons
    @FXML
    private Button downloadedFilesButton;
    @FXML
    private Button uploadedFilesButton;
    @FXML
    private Button allFilesButton;

    // Anchors
    @FXML
    private AnchorPane uploadedFilesAnchorPane;
    @FXML
    private AnchorPane downloadedFilesAnchorPane;
    @FXML
    private AnchorPane allFilesAnchorPane;

    private Map<Button, AnchorPane> buttonAndItsAnchor;
    private Map<Button, Function<String, Set<FileDTO>>> buttonAndItsFunctionToGetFileDTOSet;

    private ExecutorService executorForUploading;
    private ExecutorService executorForDownloading;

    private ScheduledExecutorService executorForShowingProgressDownloading;
    private ScheduledExecutorService executorForShowingProgressUploading;

    private volatile boolean pageIsNotSwitched;

    private LinkedBlockingDeque<FileInTable> fileDequeForDownloading;
    private LinkedBlockingDeque<FileToUpload> fileDequeForUploading;

    private FileInTable selectedFileToDownload;
    private File selectedFileToUpload;
    private SymmetricalBlockMode selectedModeToUpload;

    private FileInTable fileThatIsDownloading;
    private FileInTable fileThatIsUploading;

    private Set<FileInTable> fileInTableSet;
    private final ObservableList<FileInTable> fileInTableObservableList = FXCollections.observableArrayList();

    private final TokenStorage tokenStorage = TokenStorage.getInstance();

    private final AccountClient accountClient = AccountClientImpl.getInstance();
    private final FileClient fileClient = FileClientImpl.getInstance();

    private static final RC6Bits32.CipherKeyLength CIPHER_KEY_LENGTH = RC6Bits32.CipherKeyLength.BIT_128;
    private final SymmetricalBlockEncryptionAlgorithm rc6 = RC6Bits32.getInstance(
            new RoundKeysGeneratorImpl(32, 20, CIPHER_KEY_LENGTH)
    );

    private Benaloh benaloh;

    private String pathToSaveDownloadedFile;

    private final byte[] buffer = new byte[1024];

    public MainPageController() {
        byte[] nArray;
        do {
            benaloh = new BenalohImpl(PrimeCheckerType.MILLER_RABIN, 0.9999999, 293);
            nArray = benaloh.getOpenKey().getN().toByteArray();
        } while (nArray.length >= Byte.MAX_VALUE);
    }

    // init
    public void initialize() {
        welcomeLabel.setText("Welcome, " + accountClient.getName(tokenStorage.getToken()));

        buttonAndItsAnchor = Map.of(
                allFilesButton, allFilesAnchorPane,
                downloadedFilesButton, downloadedFilesAnchorPane,
                uploadedFilesButton, uploadedFilesAnchorPane
        );

        buttonAndItsFunctionToGetFileDTOSet = Map.of(
                allFilesButton, fileClient::getAllFiles,
                downloadedFilesButton, accountClient::getDownloadedFiles,
                uploadedFilesButton, accountClient::getUploadedFiles
        );

        initAllFilesTable();
        initUploadedFilesTable();
        initDownloadedFilesTable();

        fileInTableSet = fileClient.getAllFiles(tokenStorage.getToken())
                .stream()
                .map(FileInTable::new)
                .collect(Collectors.toSet());
        fileInTableObservableList.clear();
        fileInTableObservableList.addAll(fileInTableSet);

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            fileInTableSet = fileClient.searchFiles(newValue, tokenStorage.getToken())
                    .stream()
                    .map(FileInTable::new)
                    .collect(Collectors.toSet());

            fileInTableObservableList.clear();
            fileInTableObservableList.addAll(fileInTableSet);
        });

        chooseBoxForMode.getItems().addAll(Arrays.stream(SymmetricalBlockMode.values()).toList());
        chooseBoxForMode.setOnAction(actionEvent -> selectedModeToUpload = chooseBoxForMode.getValue());

        fileDequeForDownloading = new LinkedBlockingDeque<>();
        fileDequeForUploading = new LinkedBlockingDeque<>();

        pageIsNotSwitched = true;

        executorForUploading = Executors.newSingleThreadExecutor();
        executorForUploading.submit(() -> {
            try {
                uploadingRunnable();
            } catch (InterruptedException e) {
                log.info("Stop uploading task");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executorForDownloading = Executors.newSingleThreadExecutor();
        executorForDownloading.submit(() -> {
            try {
                downloadingRunnable();
            } catch (InterruptedException e) {
                log.info("Stop downloading task");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        executorForShowingProgressDownloading = Executors.newSingleThreadScheduledExecutor();
        executorForShowingProgressUploading = Executors.newSingleThreadScheduledExecutor();
    }

    private void initAllFilesTable() {
        allFilesTable.setItems(fileInTableObservableList);

        allFilesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        allFilesName.setStyle("-fx-alignment: CENTER-LEFT;");

        allFilesBytes.setCellValueFactory(new PropertyValueFactory<>("bytes"));
        allFilesBytes.setStyle("-fx-alignment: CENTER;");

        allFilesDate.setCellValueFactory(new PropertyValueFactory<>("addDate"));
        allFilesDate.setStyle("-fx-alignment: CENTER;");
    }

    private void initDownloadedFilesTable() {
        downloadedFilesTable.setItems(fileInTableObservableList);

        downloadedFilesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        downloadedFilesName.setStyle("-fx-alignment: CENTER-LEFT;");

        downloadedFileBytes.setCellValueFactory(new PropertyValueFactory<>("bytes"));
        downloadedFileBytes.setStyle("-fx-alignment: CENTER;");

        downloadedFileDate.setCellValueFactory(new PropertyValueFactory<>("addDate"));
        downloadedFileDate.setStyle("-fx-alignment: CENTER;");

        downloadedFileStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        downloadedFileStatus.setStyle("-fx-alignment: CENTER;");

        downloadedFileProgress.setCellValueFactory(new PropertyValueFactory<>("progressBar"));
        downloadedFileProgress.setStyle("-fx-alignment: CENTER;");
    }

    private void initUploadedFilesTable() {
        uploadedFilesTable.setItems(fileInTableObservableList);

        uploadedFilesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        allFilesName.setStyle("-fx-alignment: CENTER-LEFT;");

        uploadedFilesBytes.setCellValueFactory(new PropertyValueFactory<>("bytes"));
        uploadedFilesBytes.setStyle("-fx-alignment: CENTER;");

        uploadedFilesDate.setCellValueFactory(new PropertyValueFactory<>("addDate"));
        uploadedFilesDate.setStyle("-fx-alignment: CENTER;");

        uploadedFilesStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        uploadedFilesStatus.setStyle("-fx-alignment: CENTER;");

        uploadedFilesProgress.setCellValueFactory(new PropertyValueFactory<>("progressBar"));
        uploadedFilesProgress.setStyle("-fx-alignment: CENTER;");
    }


    // uploading
    @FXML
    void chooseFileToUpload(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        selectedFileToUpload = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedFileToUpload != null) {
            fileNameToUploadTextField.setText(selectedFileToUpload.getName());
        }
    }

    @FXML
    void uploadFile(ActionEvent event) {
        String errorName = "Error uploading";
        if (selectedFileToUpload == null) {
            CryptoServiceApplication.showAlert(errorName, "No file to upload", Alert.AlertType.ERROR);
            return;
        }

        String fileNameOfFileToUpload = fileNameToUploadTextField.getText();
        if (fileNameOfFileToUpload.isBlank()) {
            CryptoServiceApplication.showAlert(errorName, "No file name", Alert.AlertType.ERROR);
            return;
        }

        if (selectedModeToUpload == null) {
            CryptoServiceApplication.showAlert(errorName, "No mode selected", Alert.AlertType.ERROR);
            return;
        }

        FileToUpload fileToUpload = new FileToUpload();
        fileToUpload.setFile(new File(selectedFileToUpload.toURI()));
        fileToUpload.setFileName(fileNameOfFileToUpload);
        fileToUpload.setMode(SymmetricalBlockMode.valueOf(selectedModeToUpload.name()));
        fileDequeForUploading.add(fileToUpload);

        selectedFileToUpload = null;
        fileNameToUploadTextField.setText("");
    }

    private void uploadingRunnable() throws InterruptedException {
        while (pageIsNotSwitched) {
            fileThatIsUploading = null;
            FileToUpload fileToUpload = fileDequeForUploading.take();

            fileThatIsUploading = new FileInTable(fileToUpload);
            fileThatIsUploading.setStatus(FileProgressStatus.ENCODING);
            fileThatIsUploading.setProgress(0.0);

            byte[] cipherKey = getRandomArray(CIPHER_KEY_LENGTH.bitsNumber / Byte.SIZE);
            byte[] iv = getRandomArray(rc6.getOpenTextBlockSizeInBytes());
            byte[] hash = getRandomArray(rc6.getOpenTextBlockSizeInBytes());

            File tmpFile = getTmpFileToEncode();
            CypherInformant cypherInformant = new CypherInformant(fileToUpload.getFile().length());

            ScheduledFuture<?> scheduledFuture = null;
            try (SymmetricBlockCypher cipher = SymmetricBlockCypher.build(cipherKey, fileToUpload.getMode(), rc6, iv, 0, hash)) {
                scheduledFuture = executorForShowingProgressDownloading.scheduleAtFixedRate(
                        () -> {
                            fileThatIsUploading.setProgress(cypherInformant.getPercentsOfProcessedBytes() / 100.0);
                            log.info("Process:" + (cypherInformant.getPercentsOfProcessedBytes() / 100.0));
                            uploadedFilesTable.refresh();
                        },
                        0,
                        1,
                        TimeUnit.SECONDS
                );

                cipher.encode(fileToUpload.getFile(), tmpFile, cypherInformant);
                fileThatIsUploading.setProgress(1.0);
            } catch (IOException e) {
                Platform.runLater(() -> CryptoServiceApplication.showAlert(
                        "Error encoding",
                        "Can't encode file " + fileToUpload.getFileName(),
                        Alert.AlertType.ERROR
                ));
                continue;
            } finally {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
            }

            Benaloh.OpenKey serverOpenKey;
            try {
                serverOpenKey = fileClient.getServiceOpenKey(tokenStorage.getToken());
            } catch (Exception e) {
                Platform.runLater(() -> CryptoServiceApplication.showAlert(
                        "Server error",
                        "Get key of file " + fileToUpload.getFileName(),
                        Alert.AlertType.ERROR
                ));
                continue;
            }

            byte[][] encodedSymmetricalKey = new byte[cipherKey.length][];
            int numberOfBytesInEncodedCipherKey = 0;
            for (int i = 0; i < cipherKey.length; i++) {
                byte byteOfCipherKey = cipherKey[i];
                encodedSymmetricalKey[i] = benaloh.encode(new byte[]{byteOfCipherKey}, serverOpenKey);
                assert encodedSymmetricalKey[i].length < Byte.MAX_VALUE;
                numberOfBytesInEncodedCipherKey += encodedSymmetricalKey[i].length;
            }

            fileToUpload.setFile(tmpFile);
            fileToUpload.setKey(convertTwoDArrayToOneD(encodedSymmetricalKey, numberOfBytesInEncodedCipherKey));
            fileToUpload.setIv(iv);
            fileToUpload.setIndexForCTR(0);
            fileToUpload.setHash(hash);
            try {
                fileClient.addFile(fileToUpload, tokenStorage.getToken());
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> CryptoServiceApplication.showAlert("Server error", "Can't add file " + fileToUpload.getFileName(), Alert.AlertType.ERROR));
            }
        }
    }

    private byte[] getRandomArray(int arrayLength) {
        byte[] cipherKey = new byte[arrayLength];
        for (int i = 0; i < cipherKey.length; i++) {
            byte randomByte = (byte) ThreadLocalRandom.current().nextInt();
            cipherKey[i] = randomByte != 0 ? randomByte : Byte.MAX_VALUE;
        }
        return cipherKey;
    }

    private File getTmpFileToEncode() {
        String nameOfTmpFileToEncode = "encode";
        File tmpFile = new File(nameOfTmpFileToEncode);
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {

        } catch (IOException e) {
            shutdownExecutors();
            CryptoServiceApplication.showAlertAndCloseApplication(
                    (Stage) allFilesButton.getScene().getWindow(),
                    "Error encoding",
                    "No tmp file to encode!"
            );
        }
        return tmpFile;
    }

    private byte[] convertTwoDArrayToOneD(byte[][] twoDArray, int numberOfBytes) {
        byte[] oneDArray = new byte[numberOfBytes + twoDArray.length];

        int ptr = 0;
        for (byte[] array : twoDArray) {
            oneDArray[ptr++] = (byte) array.length;
            System.arraycopy(array, 0, oneDArray, ptr, array.length);
            ptr += array.length;
        }
        return oneDArray;
    }

    // downloading
    @FXML
    void choosePath(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedPath = directoryChooser.showDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedPath != null) {
            pathToSaveDownloadedFile = selectedPath.getPath();
        }
    }

    @FXML
    void selectFileToDownload(MouseEvent event) {
        selectedFileToDownload = allFilesTable.getSelectionModel().getSelectedItem();
    }

    @FXML
    void downloadFile(ActionEvent event) {
        if (selectedFileToDownload == null) {
            CryptoServiceApplication.showAlert("Error downloading", "Choose file", Alert.AlertType.ERROR);
            return;
        }
        if (pathToSaveDownloadedFile == null) {
            CryptoServiceApplication.showAlert("Error downloading", "Select path", Alert.AlertType.ERROR);
            return;
        }

        if (fileDequeForDownloading.contains(selectedFileToDownload)) {
            FileInTable copiedFile = new FileInTable(selectedFileToDownload);
            copiedFile.setProgress(0);
            copiedFile.setStatus(FileProgressStatus.WAITING);
            fileDequeForDownloading.add(copiedFile);
            return;
        }

        selectedFileToDownload.setProgress(0);
        selectedFileToDownload.setStatus(FileProgressStatus.WAITING);
        fileDequeForDownloading.add(selectedFileToDownload);
    }

    private void downloadingRunnable() throws InterruptedException {
        while (pageIsNotSwitched) {
            fileThatIsDownloading = null;
            fileThatIsDownloading = fileDequeForDownloading.take();

            Resource resource = fileClient.getFile(fileThatIsDownloading.getId(), tokenStorage.getToken());
            String nameOfTmpFileToDecode = "decode";
            File srcFileToDecode = new File(nameOfTmpFileToDecode);
            try {
                fileThatIsDownloading.setStatus(FileProgressStatus.DOWNLOADING);
                tryToCopyFileAndShowProgress(resource, fileThatIsDownloading.getBytes(), srcFileToDecode);
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> CryptoServiceApplication.showAlert(
                        "Error downloading",
                        "Can't download file:" + fileThatIsDownloading.getName(),
                        Alert.AlertType.ERROR
                ));
                continue;
            }

            fileThatIsDownloading.setProgress(0);
            fileThatIsDownloading.setStatus(FileProgressStatus.DECODING);
            String symmetricalKeyFromServer;
            try {
                symmetricalKeyFromServer = fileClient.getSymmetricalKey(fileThatIsDownloading.getId(), benaloh.getOpenKey(), tokenStorage.getToken());
            } catch (Exception e) {
                Platform.runLater(() -> CryptoServiceApplication.showAlert(
                        "Error downloading",
                        "Can't get key of file:" + fileThatIsDownloading.getName(),
                        Alert.AlertType.ERROR
                ));
                continue;
            }

            byte[] oneDSymmetricalKey = Base64.getDecoder().decode(symmetricalKeyFromServer);
            byte[][] twoDSymmetricalKey = translateOneDSymmetricalKeyToTwoD(oneDSymmetricalKey);
            byte[] symmetricalCipherKey = decodeTwoDSymmetricalKey(twoDSymmetricalKey);

            byte[] iv = Base64.getDecoder().decode(fileThatIsDownloading.getIv());
            byte[] hash = Base64.getDecoder().decode(fileThatIsDownloading.getHash());
            int indexForCTR = fileThatIsDownloading.getIndexForCTR();

            String nameOfFileToDecode = pathToSaveDownloadedFile + "\\" + fileThatIsDownloading.getName();
            File destFileForDecoding = getFileToDecode(nameOfFileToDecode);
            CypherInformant cypherInformant = new CypherInformant(srcFileToDecode.length());

            ScheduledFuture<?> scheduledFuture = null;
            try (SymmetricBlockCypher cipher = SymmetricBlockCypher.build(symmetricalCipherKey, fileThatIsDownloading.getMode(), rc6, iv, indexForCTR, hash)) {
                scheduledFuture = executorForShowingProgressDownloading.scheduleAtFixedRate(
                        () -> {
                            fileThatIsDownloading.setProgress(cypherInformant.getPercentsOfProcessedBytes() / 100.0);
                            log.info("Process:" + cypherInformant.getPercentsOfProcessedBytes() / 100.0);
                            downloadedFilesTable.refresh();
                        },
                        0,
                        1,
                        TimeUnit.SECONDS
                );
                cipher.decode(srcFileToDecode, destFileForDecoding, cypherInformant);

                fileThatIsDownloading.setProgress(1);
                fileThatIsDownloading.setStatus(FileProgressStatus.OK);
                downloadedFilesTable.refresh();
            } catch (IOException e) {
                Platform.runLater(() -> CryptoServiceApplication.showAlert(
                        "Error encoding",
                        "Encryption error with file " + srcFileToDecode.getName(),
                        Alert.AlertType.ERROR
                ));
            } finally {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
            }
        }
    }

    private void tryToCopyFileAndShowProgress(Resource resource, long lengthOfResource, File dest) throws IOException {
        try (
                BufferedInputStream inputStream = new BufferedInputStream(resource.getInputStream());
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dest));
        ) {
            int read;
            long allRead = 0;
            while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, read);
                allRead += read;

                fileThatIsDownloading.setProgress(allRead * 100.0 / lengthOfResource);
                downloadedFilesTable.refresh();
            }
        }
    }

    private byte[][] translateOneDSymmetricalKeyToTwoD(byte[] oneDSymmetricalKey) {
        int sizeOfKey = 0;
        int arrayPtr = 0;
        while (arrayPtr < oneDSymmetricalKey.length) {
            int size = oneDSymmetricalKey[arrayPtr++];
            arrayPtr += size;
            sizeOfKey++;
        }

        arrayPtr = 0;

        byte[][] twoDKey = new byte[sizeOfKey][];
        for (int i = 0; i < sizeOfKey; i++) {
            int size = oneDSymmetricalKey[arrayPtr++];
            twoDKey[i] = new byte[size];

            System.arraycopy(oneDSymmetricalKey, arrayPtr, twoDKey[i], 0, size);
            arrayPtr += size;
        }
        return twoDKey;
    }

    private byte[] decodeTwoDSymmetricalKey(byte[][] twoDSymmetricalKey) {
        byte[] decodedCipherKey = new byte[twoDSymmetricalKey.length];
        for (int i = 0; i < twoDSymmetricalKey.length; i++) {
            byte[] decodedByte = benaloh.decode(twoDSymmetricalKey[i]);
            assert decodedByte.length == 1;
            decodedCipherKey[i] = decodedByte[0];
        }
        return decodedCipherKey;
    }

    private File getFileToDecode(String fileName) {
        File tmpFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {

        } catch (IOException e) {
            shutdownExecutors();
            CryptoServiceApplication.showAlertAndCloseApplication(
                    (Stage) allFilesButton.getScene().getWindow(),
                    "Error decoding",
                    "No tmp file to decode!"
            );
        }
        return tmpFile;
    }


    // service loading
    @FXML
    void loadAllFiles(ActionEvent event) {
        if (!searchTextField.getText().isEmpty()) {
            searchTextField.setText("");
        }

        selectedFileToDownload = null;
        selectedFileToUpload = null;
        fileNameToUploadTextField.setText("");

        Button currentButton = ((Button) event.getSource());
        setVisibleOneAnchorAndMakeInvisibleOthers(currentButton);
    }

    @FXML
    void loadDownloadedFiles(ActionEvent event) {
        Button currentButton = ((Button) event.getSource());
        setVisibleOneAnchorAndMakeInvisibleOthers(currentButton);

        fileInTableObservableList.addAll(fileDequeForDownloading);

        if (fileThatIsDownloading != null) {
            fileInTableObservableList.add(fileThatIsDownloading);
        }
    }

    @FXML
    void loadUploadedFiles(ActionEvent event) {
        Button currentButton = ((Button) event.getSource());
        setVisibleOneAnchorAndMakeInvisibleOthers(currentButton);

        fileInTableObservableList.addAll(fileDequeForUploading
                .stream()
                .map(FileInTable::new)
                .toList()
        );

        if (fileThatIsUploading != null) {
            fileInTableObservableList.add(fileThatIsUploading);
        }
    }

    private void setVisibleOneAnchorAndMakeInvisibleOthers(Button buttonClicked) {
        buttonAndItsAnchor.forEach((button, anchorPane) -> {
            if (button.equals(buttonClicked)) {
                anchorPane.setVisible(true);

                Function<String, Set<FileDTO>> functionToGetFilesFromServer = buttonAndItsFunctionToGetFileDTOSet.get(button);
                fileInTableSet = functionToGetFilesFromServer.apply(tokenStorage.getToken())
                        .stream()
                        .map(FileInTable::new)
                        .collect(Collectors.toSet());

                fileInTableObservableList.clear();
                fileInTableObservableList.addAll(fileInTableSet);
            } else {
                anchorPane.setVisible(false);
            }
        });
    }


    // closing + logout
    @FXML
    public void logout(MouseEvent event) throws IOException {
        shutdownExecutors();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com.example.test/fxml/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Login");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }


    @FXML
    void close(MouseEvent event) {
        shutdownExecutors();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }


    public void shutdownExecutors() {
        pageIsNotSwitched = false;

        executorForShowingProgressDownloading.shutdown();
        executorForShowingProgressUploading.shutdown();

        executorForUploading.shutdownNow();
        executorForDownloading.shutdownNow();
    }
}
