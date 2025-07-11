package com.librarysystem;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.User;
import com.librarysystem.service.BookService;
import com.librarysystem.ui.LoginController;
import com.librarysystem.ui.MainAppController; // Will be created next

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private Stage primaryStage;
    private User currentUser; // To store the logged-in user details

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Library Management System");

        // Initialize Database Schema & Populate Books
        try {
            LOGGER.info("Initializing database schema...");
            DatabaseUtil.initializeSchema(); // Make sure DB is up and schema.sql is runnable
            LOGGER.info("Database schema initialization attempted.");

            LOGGER.info("Populating sample books if database is empty...");
            BookService bookService = new BookService(); // Create BookService instance
            bookService.populateDatabaseWithSampleBooksIfEmpty();
            LOGGER.info("Sample book population attempted.");

        } catch (Exception e) {
            LOGGER.error("Error during initial database setup: {}", e.getMessage(), e);
            // Optionally show an error dialog to the user
        }


        showLoginView();
    }

    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/librarysystem/ui/LoginView.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setApp(this); // Pass a reference of Main to the controller

            Scene scene = new Scene(root, 400, 300);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.error("Failed to load LoginView.fxml", e);
            // Handle error, maybe show an alert
        }
    }

    public void showMainAppView(User user) {
        this.currentUser = user;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/librarysystem/ui/MainAppView.fxml")); // This FXML will be created next
            Parent root = loader.load();

            MainAppController controller = loader.getController();
            controller.setApp(this);
            controller.setCurrentUser(currentUser);
            // controller.initializeData(); // Method in MainAppController to load initial data based on user

            Scene scene = new Scene(root, 1024, 768); // Larger scene for main app
            primaryStage.setScene(scene);
            primaryStage.setTitle("Library System - Welcome " + currentUser.getUsername());
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.error("Failed to load MainAppView.fxml", e);
            // Handle error
        } catch (Exception e) { // Catch other potential errors from controller init
            LOGGER.error("Error initializing MainAppView", e);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public static void main(String[] args) {
        LOGGER.info("Application starting...");
        launch(args);
        LOGGER.info("Application finished.");
    }
}
