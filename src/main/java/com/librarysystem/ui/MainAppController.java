package com.librarysystem.ui;

import com.librarysystem.Main;
import com.librarysystem.model.*;
import com.librarysystem.service.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.application.Platform;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream; // Added import
import org.slf4j.Logger; // Added import
import org.slf4j.LoggerFactory; // Added import


public class MainAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class); // Added LOGGER definition

    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane mainTabPane;
    @FXML private Tab browseBooksTab;
    @FXML private Tab myBooksTab;
    @FXML private Tab adminTab;
    @FXML private TabPane adminSubTabPane;

    // Browse Books Tab
    @FXML private TextField searchTitleField;
    @FXML private TextField searchAuthorField;
    @FXML private TextField searchIsbnField;
    @FXML private TableView<Book> booksTableView;
    @FXML private TableColumn<Book, String> bookIsbnColumn;
    @FXML private TableColumn<Book, String> bookTitleColumn;
    @FXML private TableColumn<Book, String> bookAuthorColumn;
    @FXML private TableColumn<Book, Year> bookYearColumn;
    @FXML private TableColumn<Book, String> bookGenreColumn;
    @FXML private TableColumn<Book, Integer> bookAvailableCopiesColumn;
    @FXML private TableColumn<Book, Integer> bookTotalCopiesColumn;
    @FXML private Button borrowButton;

    // My Borrowed Books Tab
    @FXML private TableView<BorrowedBookView> borrowedBooksTableView; // Custom view model
    @FXML private TableColumn<BorrowedBookView, String> borrowedIsbnColumn;
    @FXML private TableColumn<BorrowedBookView, String> borrowedTitleColumn;
    @FXML private TableColumn<BorrowedBookView, String> borrowedAuthorColumn;
    @FXML private TableColumn<BorrowedBookView, LocalDateTime> borrowDateColumn;
    @FXML private TableColumn<BorrowedBookView, LocalDateTime> dueDateColumn;
    @FXML private Button returnButton;

    // Admin - Manage Users Tab
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, Integer> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, User.Role> userRoleColumn;

    // Admin - View Logs Tab
    @FXML private TableView<LogEntry> logsTableView;
    @FXML private TableColumn<LogEntry, Integer> logIdColumn;
    @FXML private TableColumn<LogEntry, LocalDateTime> logTimestampColumn;
    @FXML private TableColumn<LogEntry, Integer> logUserIdColumn;
    @FXML private TableColumn<LogEntry, LogEntry.ActionType> logActionColumn;
    @FXML private TableColumn<LogEntry, String> logDetailsColumn;


    private Main app;
    private User currentUser;
    private BookService bookService;
    private LibraryService libraryService;
    private UserService userService;
    private LogService logService;

    private final ObservableList<Book> booksData = FXCollections.observableArrayList();
    private final ObservableList<BorrowedBookView> borrowedBooksData = FXCollections.observableArrayList();
    private final ObservableList<User> usersData = FXCollections.observableArrayList();
    private final ObservableList<LogEntry> logsData = FXCollections.observableArrayList();

    public MainAppController() {
        // Initialize services - In a real app, use dependency injection
        this.bookService = new BookService();
        this.libraryService = new LibraryService();
        this.userService = new UserService();
        this.logService = new LogService();
    }

    public void setApp(Main app) {
        this.app = app;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + currentUser.getUsername() + " (" + currentUser.getRole().name() + ")");
        configureAdminAccess();
        initializeData(); // Load data after user is set
    }

    private void configureAdminAccess() {
        if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
            if (mainTabPane.getTabs().contains(adminTab)) {
                 mainTabPane.getTabs().remove(adminTab);
            }
        } else {
            if (!mainTabPane.getTabs().contains(adminTab)) {
                // This might be tricky if removed, better to just disable or hide content
                // For simplicity, let's assume it's always there and content is controlled.
                // Or, ensure it's added if it was removed.
            }
            // If adminTab is part of FXML and just needs to be "enabled" or content shown:
            adminTab.setDisable(false); // Ensure it's enabled if it was disabled by default
        }
    }

    @FXML
    public void initialize() {
        // Initialize TableView columns for Books
        bookIsbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        bookAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        bookYearColumn.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        bookGenreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));
        bookAvailableCopiesColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        bookTotalCopiesColumn.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        booksTableView.setItems(booksData);

        // Initialize TableView columns for Borrowed Books
        borrowedIsbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        borrowedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        borrowedAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrowedBooksTableView.setItems(borrowedBooksData);

        // Initialize TableView columns for Users (Admin)
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        usersTableView.setItems(usersData);

        // Initialize TableView columns for Logs (Admin)
        logIdColumn.setCellValueFactory(new PropertyValueFactory<>("logId"));
        logTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        logUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        logActionColumn.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        logDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        logsTableView.setItems(logsData);

        // Add listeners to tab changes to refresh data
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == browseBooksTab) {
                loadAllBooks();
            } else if (newTab == myBooksTab) {
                loadBorrowedBooks();
            } else if (newTab == adminTab) {
                // Default to Manage Books when Admin tab is clicked, or handle sub-tabs
                if (adminSubTabPane.getSelectionModel().getSelectedItem() != null) {
                    handleAdminSubTabChange(adminSubTabPane.getSelectionModel().getSelectedItem());
                } else if (!adminSubTabPane.getTabs().isEmpty()) {
                     adminSubTabPane.getSelectionModel().selectFirst(); // Select first admin sub-tab
                     handleAdminSubTabChange(adminSubTabPane.getTabs().get(0));
                }
            }
        });

        adminSubTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldSubTab, newSubTab) -> {
            if (newSubTab != null) {
                 handleAdminSubTabChange(newSubTab);
            }
        });


        // Initial data load can be triggered here if currentUser is already set,
        // or better, in setCurrentUser after currentUser is confirmed.
    }

    private void handleAdminSubTabChange(Tab selectedSubTab) {
        if (selectedSubTab.getId().equals("manageBooksAdminTab")) {
            // No specific data to load here directly, actions are on selected books from main list
        } else if (selectedSubTab.getId().equals("manageUsersAdminTab")) {
            loadAllUsers();
        } else if (selectedSubTab.getId().equals("viewLogsAdminTab")) {
            loadAllLogs();
        }
    }

    public void initializeData() {
        Platform.runLater(() -> { // Ensure UI updates are on JavaFX Application Thread
            loadAllBooks();
            if (currentUser != null) {
                loadBorrowedBooks();
                if (currentUser.getRole() == User.Role.ADMIN) {
                    // Pre-load data for the initially selected admin tab if any
                    Tab selectedAdminSubTab = adminSubTabPane.getSelectionModel().getSelectedItem();
                    if (selectedAdminSubTab == null && !adminSubTabPane.getTabs().isEmpty()) {
                        selectedAdminSubTab = adminSubTabPane.getTabs().get(0);
                        adminSubTabPane.getSelectionModel().select(selectedAdminSubTab); // Also triggers listener
                    } else if (selectedAdminSubTab != null) {
                         handleAdminSubTabChange(selectedAdminSubTab); // Manually trigger for initial load
                    }
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        currentUser = null; // Clear current user
        if (app != null) {
            app.showLoginView(); // Go back to login screen
        }
    }

    // --- Book Browsing and Borrowing ---
    private void loadAllBooks() {
        List<Book> currentBooks = bookService.getAllBooks();
        booksData.setAll(currentBooks);
    }

    @FXML
    private void handleSearchBooks() {
        String title = searchTitleField.getText().trim();
        String author = searchAuthorField.getText().trim();
        String isbn = searchIsbnField.getText().trim();

        List<Book> allBooks = bookService.getAllBooks(); // Start with all books
        Stream<Book> stream = allBooks.stream();

        if (!title.isEmpty()) {
            stream = stream.filter(book -> book.getTitle().toLowerCase().contains(title.toLowerCase()));
        }
        if (!author.isEmpty()) {
            stream = stream.filter(book -> book.getAuthor().toLowerCase().contains(author.toLowerCase()));
        }
        if (!isbn.isEmpty()) {
            stream = stream.filter(book -> book.getIsbn().toLowerCase().contains(isbn.toLowerCase()));
        }

        booksData.setAll(stream.collect(Collectors.toList()));
    }

    @FXML
    private void handleClearBookSearch() {
        searchTitleField.clear();
        searchAuthorField.clear();
        searchIsbnField.clear();
        loadAllBooks();
    }


    @FXML
    private void handleBorrowBook() {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to borrow.");
            return;
        }
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            return;
        }

        boolean success = libraryService.borrowBook(currentUser.getUserId(), selectedBook.getIsbn());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book '" + selectedBook.getTitle() + "' borrowed successfully.");
            loadAllBooks(); // Refresh book list
            loadBorrowedBooks(); // Refresh user's borrowed list
        } else {
            showAlert(Alert.AlertType.ERROR, "Borrow Failed", "Could not borrow book. It might be unavailable or already borrowed by you.");
            loadAllBooks(); // Refresh to show updated availability just in case
        }
    }

    // --- My Borrowed Books ---
    private void loadBorrowedBooks() {
        if (currentUser == null) return;
        List<BorrowingRecord> records = libraryService.getCurrentlyBorrowedBooksByUser(currentUser.getUserId());
        // Map BorrowingRecord to BorrowedBookView
        borrowedBooksData.setAll(
            records.stream().map(record -> {
                Optional<Book> bookOpt = bookService.findBookByIsbn(
                    // We need a way to get ISBN from bookId in BorrowingRecord
                    // This assumes BookService can get book by its PK (book_id)
                    // Or BorrowingRecord should store more book details (less ideal for normalization)
                    // For now, let's assume BookDAO and BookService have getBookById(int pk)
                    bookService.getBookById(record.getBookId()).map(Book::getIsbn).orElse("N/A ISBn")
                );
                return bookOpt.map(book -> new BorrowedBookView(record, book.getIsbn(), book.getTitle(), book.getAuthor()))
                              .orElse(new BorrowedBookView(record, "Unknown ISBN", "Unknown Title", "Unknown Author"));
            }).collect(Collectors.toList())
        );
    }

    @FXML
    private void handleReturnBook() {
        BorrowedBookView selectedBorrowed = borrowedBooksTableView.getSelectionModel().getSelectedItem();
        if (selectedBorrowed == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to return.");
            return;
        }
         if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            return;
        }

        boolean success = libraryService.returnBook(currentUser.getUserId(), selectedBorrowed.getIsbn());
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book '" + selectedBorrowed.getTitle() + "' returned successfully.");
            loadAllBooks(); // Refresh book list
            loadBorrowedBooks(); // Refresh user's borrowed list
        } else {
            showAlert(Alert.AlertType.ERROR, "Return Failed", "Could not return book. Please check the details.");
        }
    }


    // --- Admin Functionalities ---
    @FXML
    private void handleAddBook() {
        // Open a dialog for adding a new book
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/librarysystem/ui/AddBookDialog.fxml"));
            Parent page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Book");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(logoutButton.getScene().getWindow()); // any node from the main scene
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            AddBookDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setBookService(bookService); // Pass service

            dialogStage.showAndWait();
            loadAllBooks(); // Refresh book list after dialog closes
        } catch (IOException e) {
            LOGGER.error("Failed to load Add Book dialog.", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Add Book dialog.");
        }
    }

    @FXML
    private void handleEditBook() {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book from the 'Browse Books' tab to edit.");
            return;
        }
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/librarysystem/ui/EditBookDialog.fxml"));
            Parent page = loader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Book");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(logoutButton.getScene().getWindow());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            EditBookDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setBookService(bookService);
            controller.setBookToEdit(selectedBook); // Pass the selected book

            dialogStage.showAndWait();
            loadAllBooks(); // Refresh
        } catch (IOException e) {
            LOGGER.error("Failed to load Edit Book dialog.", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Edit Book dialog.");
        }
    }

    @FXML
    private void handleRemoveBook() {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book from the 'Browse Books' tab to remove.");
            return;
        }

        // Check if book has borrowed copies
        if (selectedBook.getAvailableCopies() != selectedBook.getTotalCopies()) {
             showAlert(Alert.AlertType.ERROR, "Cannot Remove", "Book '" + selectedBook.getTitle() + "' has copies currently on loan and cannot be removed.");
             return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to remove '" + selectedBook.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean success = bookService.removeBook(selectedBook.getIsbn());
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Book removed successfully.");
                    loadAllBooks();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Removal Failed", "Could not remove book.");
                }
            }
        });
    }

    private void loadAllUsers() {
        if (currentUser != null && currentUser.getRole() == User.Role.ADMIN) {
            List<User> currentUsers = userService.getAllUsers(); // New method in UserService needed
            usersData.setAll(currentUsers);
        }
    }

    @FXML
    private void handleChangeUserRole() {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to change their role.");
            return;
        }
        if (selectedUser.getUserId() == currentUser.getUserId()) {
            showAlert(Alert.AlertType.WARNING, "Action Denied", "Cannot change your own role.");
            return;
        }

        // Simple dialog to choose new role
        ChoiceDialog<User.Role> dialog = new ChoiceDialog<>(selectedUser.getRole(), User.Role.values());
        dialog.setTitle("Change User Role");
        dialog.setHeaderText("Change role for user: " + selectedUser.getUsername());
        dialog.setContentText("Choose new role:");

        Optional<User.Role> result = dialog.showAndWait();
        result.ifPresent(newRole -> {
            boolean success = userService.updateUserRole(selectedUser.getUserId(), newRole, currentUser);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User role updated successfully.");
                loadAllUsers(); // Refresh user list
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update user role.");
            }
        });
    }


    private void loadAllLogs() {
         if (currentUser != null && currentUser.getRole() == User.Role.ADMIN) {
            List<LogEntry> currentLogs = logService.getAllLogs();
            logsData.setAll(currentLogs);
        }
    }

    @FXML
    private void handleRefreshLogs(){
        loadAllLogs();
    }


    // --- Utility ---
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper method in BookService to get Book by its PK (book_id)
    // This is a temporary reference to what BookService might need.
    // public Optional<Book> getBookById(int bookIdPk) {
    //    return bookDAO.getBookById(bookIdPk);
    // }
}
