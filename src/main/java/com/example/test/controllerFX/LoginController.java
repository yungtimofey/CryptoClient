package com.example.test.controllerFX;

import com.example.test.CryptoServiceApplication;
import com.example.test.model.RegistrationParams;
import com.example.test.rest_client.RegistrationClient;
import com.example.test.rest_client.impl.RegistrationClientImpl;
import com.example.test.token.TokenStorage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;


public class LoginController {
    @FXML
    private PasswordField passwordTextField;

    @FXML
    private TextField usernameTextField;

    private final RegistrationClient registrationClient = RegistrationClientImpl.getInstance();

    private final TokenStorage tokenStorage = TokenStorage.getInstance();

    @FXML
    void close(MouseEvent event) {
        Stage stage =(Stage)((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void login(ActionEvent event) {
        String username = usernameTextField.getText();
        String password = passwordTextField.getText();

        if (username.isBlank() || password.isBlank()) {
            CryptoServiceApplication.showAlert("Error login","Empty input fields", Alert.AlertType.ERROR);
            return;
        }

        try {
            String token = registrationClient.login(new RegistrationParams(username, password));
            tokenStorage.setToken(token);
            loadMainPageOrCloseApplication((Stage) ((Node)event.getSource()).getScene().getWindow());
        } catch (Exception e) {
            CryptoServiceApplication.showAlert("Error login","Wrong login data", Alert.AlertType.ERROR);
        }
    }

    private void loadMainPageOrCloseApplication(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com.example.test/fxml/main-page.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1100, 600);
            stage.setTitle("Main");
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
        } catch (IOException e) {
            e.printStackTrace();
            CryptoServiceApplication.showAlertAndCloseApplication(stage, "Error","Can't load main page.");
        }
    }
}
