package com.librarysystem.ui;

import com.librarysystem.model.Book;
import com.librarysystem.service.BookService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.Year;
import java.time.format.DateTimeParseException;

public class AddBookDialogController {

    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField yearField;
    @FXML private TextField genreField;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label errorMessageLabel;

    private Stage dialogStage;
    private BookService bookService;
    private boolean saved = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBookService(BookService bookService) {
        this.bookService = bookService;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSaveBook() {
        String isbn = isbnField.getText();
        String title = titleField.getText();
        String author = authorField.getText();
        String yearStr = yearField.getText();
        String genre = genreField.getText();
        Integer totalCopies = copiesSpinner.getValue();

        if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || yearStr.isEmpty() || genre.isEmpty()) {
            setErrorMessage("All fields must be filled.");
            return;
        }

        Year publicationYear;
        try {
            publicationYear = Year.parse(yearStr);
            if (publicationYear.isAfter(Year.now().plusYears(1))) { // Basic validation for future year
                 setErrorMessage("Publication year cannot be too far in the future.");
                 return;
            }
        } catch (DateTimeParseException e) {
            setErrorMessage("Invalid year format. Please use YYYY.");
            return;
        }

        if (totalCopies == null || totalCopies < 0) {
             setErrorMessage("Total copies must be a non-negative number.");
            return;
        }
        if (totalCopies == 0 && bookService.findBookByIsbn(isbn).isEmpty()) {
            // If it's a new book, it should have at least 1 copy if totalCopies is set to 0.
            // However, our spinner min is 1. So this check is more for robustness.
            // For adding new book, totalCopies must be > 0.
            // The spinner already enforces this.
        }


        boolean success = bookService.addBook(isbn, title, author, publicationYear, genre, totalCopies);

        if (success) {
            saved = true;
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book added successfully!");
            dialogStage.close();
        } else {
            // BookService might log specific errors (e.g., duplicate ISBN)
            setErrorMessage("Failed to add book. ISBN might already exist or invalid data.");
            // showAlert(Alert.AlertType.ERROR, "Error", "Could not add book. Check console for details.");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void setErrorMessage(String message) {
        errorMessageLabel.setText(message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
