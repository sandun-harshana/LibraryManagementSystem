package com.librarysystem.ui;

import com.librarysystem.model.Book;
import com.librarysystem.service.BookService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.Year;
import java.time.format.DateTimeParseException;

public class EditBookDialogController {

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
    private Book bookToEdit;
    private boolean saved = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setBookService(BookService bookService) {
        this.bookService = bookService;
    }

    public void setBookToEdit(Book book) {
        this.bookToEdit = book;
        if (book != null) {
            isbnField.setText(book.getIsbn());
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            yearField.setText(book.getPublicationYear().toString());
            genreField.setText(book.getGenre());
            copiesSpinner.getValueFactory().setValue(book.getTotalCopies());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSaveChanges() {
        String title = titleField.getText();
        String author = authorField.getText();
        String yearStr = yearField.getText();
        String genre = genreField.getText();
        Integer totalCopies = copiesSpinner.getValue();

        if (title.isEmpty() || author.isEmpty() || yearStr.isEmpty() || genre.isEmpty()) {
            setErrorMessage("Title, Author, Year, and Genre fields must be filled.");
            return;
        }

        Year publicationYear;
        try {
            publicationYear = Year.parse(yearStr);
             if (publicationYear.isAfter(Year.now().plusYears(1))) {
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

        // Check if new total copies is less than number of currently borrowed copies
        int borrowedCount = bookToEdit.getTotalCopies() - bookToEdit.getAvailableCopies();
        if (totalCopies < borrowedCount) {
            setErrorMessage("Total copies ("+ totalCopies +") cannot be less than currently borrowed copies (" + borrowedCount + ").");
            return;
        }


        boolean success = bookService.updateBookDetails(
                bookToEdit.getIsbn(),
                title,
                author,
                publicationYear,
                genre,
                totalCopies
        );

        if (success) {
            saved = true;
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book details updated successfully!");
            dialogStage.close();
        } else {
            setErrorMessage("Failed to update book. Please check data or console for errors.");
            // showAlert(Alert.AlertType.ERROR, "Error", "Could not update book. Check console for details.");
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
