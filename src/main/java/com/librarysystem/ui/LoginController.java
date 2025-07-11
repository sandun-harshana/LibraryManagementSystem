package com.librarysystem.ui;

import com.librarysystem.model.User;
import com.librarysystem.service.UserService;
import com.librarysystem.Main; // Assuming Main.java will handle scene changes

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.util.Optional;


public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label messageLabel;

    private UserService userService;
    private Main app; // To call method for changing scene


    public LoginController() {
        this.userService = new UserService(); // Ideally, inject this
    }

    public void setApp(Main app) {
        this.app = app;
    }


    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and password cannot be empty.", Color.RED);
            return;
        }

        Optional<User> userOptional = userService.loginUser(username, password);

        if (userOptional.isPresent()) {
            showMessage("Login successful!", Color.GREEN);
            // Proceed to the main application window
            // This requires a method in Main.java to switch scenes
            if (app != null) {
                app.showMainAppView(userOptional.get());
            } else {
                 // Fallback or error if app reference isn't set
                System.err.println("Main application reference not set in LoginController.");
                showMessage("Error: Application context not found.", Color.RED);
            }
        } else {
            showMessage("Invalid username or password.", Color.RED);
        }
    }

    @FXML
    private void handleRegister() {
        // For simplicity, registration will use the same fields.
        // A real app might have a separate registration form with more fields (e.g., confirm password, email).
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and password cannot be empty for registration.", Color.RED);
            return;
        }

        // Default new users to MEMBER role. Admin creation would be manual or a separate process.
        boolean success = userService.registerUser(username, password, User.Role.MEMBER);

        if (success) {
            showMessage("Registration successful! You can now log in.", Color.GREEN);
            usernameField.clear();
            passwordField.clear();
        } else {
            // UserService logs specific reasons (e.g., username exists)
            showMessage("Registration failed. Username might be taken or an error occurred.", Color.RED);
        }
    }

    private void showMessage(String message, Color color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(color);
    }

    public void initialize() {
        // Initialization logic if needed when FXML is loaded
    }
}
