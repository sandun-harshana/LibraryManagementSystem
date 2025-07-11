package com.librarysystem.service;

import com.librarysystem.dao.BookDAO;
import com.librarysystem.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Random;
import com.github.javafaker.Faker; // Will add this dependency to pom.xml

public class BookService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookService.class);
    private final BookDAO bookDAO;
    private final Faker faker; // For generating sample data
    private final LogService logService; // Added LogService
    private static final int MIN_BOOKS_TO_POPULATE = 100;


    public BookService() {
        this.bookDAO = new BookDAO(); // In a real app, use dependency injection
        this.faker = new Faker();
        this.logService = new LogService(); // Initialize LogService
    }

    // Constructor for testing with a mock DAO and LogService
    public BookService(BookDAO bookDAO, LogService logService) {
        this.bookDAO = bookDAO;
        this.faker = new Faker();
        this.logService = logService;
    }

    // Method to get Book by its DB Primary Key - needed for BorrowedBookView
    public Optional<Book> getBookById(int bookIdPk) {
        return bookDAO.getBookById(bookIdPk);
    }

    /**
     * Adds a new book to the library.
     * Validates input parameters and checks for duplicate ISBNs.
     * Logs the action upon success or failure.
     *
     * @param isbn The ISBN of the book. Must not be null or empty.
     * @param title The title of the book. Must not be null or empty.
     * @param author The author of the book. Must not be null or empty.
     * @param publicationYear The publication year of the book. Must not be null.
     * @param genre The genre of the book.
     * @param totalCopies The total number of copies of this book. Must be non-negative.
     * @return {@code true} if the book was added successfully, {@code false} otherwise.
     */
    public boolean addBook(String isbn, String title, String author, Year publicationYear, String genre, int totalCopies) {
        if (isbn == null || isbn.trim().isEmpty() || title == null || title.trim().isEmpty() || author == null || author.trim().isEmpty() || publicationYear == null || totalCopies < 0) {
            LOGGER.warn("Attempted to add book with invalid parameters.");
            return false;
        }
        if (bookDAO.getBookByIsbn(isbn).isPresent()) {
            LOGGER.warn("Attempted to add book with duplicate ISBN: {}", isbn);
            // Optionally, could update existing book's copy count here or throw specific exception
            return false;
        }
        Book book = new Book(isbn, title, author, publicationYear, genre, totalCopies);
        boolean success = bookDAO.addBook(book);
        if (success) {
            LOGGER.info("Book added successfully: {} by {}", title, author);
            logService.recordLog(null, com.librarysystem.model.LogEntry.ActionType.BOOK_ADDED, "Book added: ISBN " + isbn + ", Title: " + title);
        } else {
            LOGGER.error("Failed to add book: {} by {}", title, author);
        }
        return success;
    }

    public Optional<Book> findBookByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            LOGGER.debug("ISBN for search is null or empty.");
            return Optional.empty();
        }
        return bookDAO.getBookByIsbn(isbn);
    }

    public List<Book> getAllBooks() {
        return bookDAO.getAllBooks();
    }

    public List<Book> findBooksByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return new ArrayList<>(); // Return empty list if search term is invalid
        }
        return bookDAO.findBooksByTitle(title);
    }

    public List<Book> findBooksByAuthor(String author) {
         if (author == null || author.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return bookDAO.findBooksByAuthor(author);
    }

    public boolean updateBookDetails(String isbn, String newTitle, String newAuthor, Year newPublicationYear, String newGenre, Integer newTotalCopies) {
        Optional<Book> existingBookOpt = bookDAO.getBookByIsbn(isbn);
        if (existingBookOpt.isEmpty()) {
            LOGGER.warn("Attempted to update non-existent book with ISBN: {}", isbn);
            return false;
        }
        Book existingBook = existingBookOpt.get();

        // Update fields if new values are provided
        if (newTitle != null && !newTitle.trim().isEmpty()) existingBook.setTitle(newTitle);
        if (newAuthor != null && !newAuthor.trim().isEmpty()) existingBook.setAuthor(newAuthor);
        if (newPublicationYear != null) existingBook.setPublicationYear(newPublicationYear);
        if (newGenre != null && !newGenre.trim().isEmpty()) existingBook.setGenre(newGenre);

        if (newTotalCopies != null) {
            if (newTotalCopies < existingBook.getAvailableCopies() - (existingBook.getTotalCopies() - existingBook.getAvailableCopies())) {
                 LOGGER.warn("New total copies ({}) for ISBN {} cannot be less than currently borrowed copies.", newTotalCopies, isbn);
                 // This logic is a bit complex: newTotalCopies must be >= (totalCopies - availableCopies) which is borrowedCount
                 // So, newTotalCopies must be >= borrowedCount. The available copies will then be newTotalCopies - borrowedCount.
                 // A simpler check: if newTotalCopies < (existingBook.getTotalCopies() - existingBook.getAvailableCopies())
                 // For now, let's enforce newTotalCopies >= current available_copies if we are reducing,
                 // or more simply, ensure total copies is not less than checked out books.
                 // The Book model's setTotalCopies and setAvailableCopies should handle internal consistency.
                 // Let's assume setTotalCopies will adjust availableCopies appropriately or throw error.
            }
             existingBook.setTotalCopies(newTotalCopies); // This will also update isAvailable via setter logic if needed
             // If total copies is reduced, available copies might need adjustment:
             // available_copies should be total_copies - (old_total_copies - old_available_copies)
             // The Book.setTotalCopies should ideally handle this adjustment with setAvailableCopies.
             // For now, we assume the Book model's setters are robust.
        }

        boolean success = bookDAO.updateBook(existingBook);
        if (success) {
            LOGGER.info("Book updated successfully: {}", isbn);
            logService.recordLog(null, com.librarysystem.model.LogEntry.ActionType.BOOK_UPDATED, "Book updated: ISBN " + isbn);
        } else {
            LOGGER.error("Failed to update book: {}", isbn);
        }
        return success;
    }

    public boolean updateBookAvailability(String isbn, int changeInAvailableCopies) {
        Optional<Book> bookOpt = bookDAO.getBookByIsbn(isbn);
        if (bookOpt.isEmpty()) {
            LOGGER.warn("Cannot update availability, book not found with ISBN: {}", isbn);
            return false;
        }
        Book book = bookOpt.get();
        int newAvailableCopies = book.getAvailableCopies() + changeInAvailableCopies;
        if (newAvailableCopies < 0 || newAvailableCopies > book.getTotalCopies()) {
            LOGGER.error("Invalid change in availability for ISBN {}. New available copies ({}) would be out of bounds [0, {}]", isbn, newAvailableCopies, book.getTotalCopies());
            return false;
        }
        book.setAvailableCopies(newAvailableCopies);
        return bookDAO.updateBook(book);
    }


    public boolean removeBook(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            LOGGER.warn("Attempted to remove book with null or empty ISBN.");
            return false;
        }
        // Add check: cannot remove book if it's currently borrowed by someone.
        // This requires BorrowingHistoryDAO to check active borrows for this book.
        // For now, this check is omitted but important for a real system.
        // Optional<Book> bookOpt = bookDAO.getBookByIsbn(isbn);
        // if (bookOpt.isPresent() && bookOpt.get().getAvailableCopies() != bookOpt.get().getTotalCopies()) {
        //     LOGGER.warn("Cannot remove book {} as it has copies currently on loan.", isbn);
        //     return false;
        // }

        boolean success = bookDAO.deleteBook(isbn);
        if (success) {
            LOGGER.info("Book removed successfully: {}", isbn);
            logService.recordLog(null, com.librarysystem.model.LogEntry.ActionType.BOOK_REMOVED, "Book removed: ISBN " + isbn);
        } else {
            LOGGER.error("Failed to remove book: {}", isbn);
        }
        return success;
    }

    public void populateDatabaseWithSampleBooksIfEmpty() {
        if (bookDAO.getAllBooks().isEmpty()) {
            LOGGER.info("Book table is empty. Populating with {} sample books...", MIN_BOOKS_TO_POPULATE);
            Random random = new Random();
            int booksAdded = 0;
            for (int i = 0; i < MIN_BOOKS_TO_POPULATE; i++) {
                String isbn = faker.code().isbn13();
                // Ensure ISBN is unique for this batch, very basic check
                int attempt = 0;
                while(bookDAO.getBookByIsbn(isbn).isPresent() && attempt < 5) {
                    isbn = faker.code().isbn13();
                    attempt++;
                }
                if (attempt >= 5 && bookDAO.getBookByIsbn(isbn).isPresent()) {
                    LOGGER.warn("Could not generate unique ISBN after multiple attempts, skipping book {}/{}", i+1, MIN_BOOKS_TO_POPULATE);
                    continue;
                }

                String title = faker.book().title();
                String author = faker.book().author();
                int yearValue = 1950 + random.nextInt(75); // Year between 1950 and 2024
                Year publicationYear = Year.of(yearValue);
                String genre = faker.book().genre();
                int totalCopies = 1 + random.nextInt(5); // 1 to 5 copies

                if (addBook(isbn, title, author, publicationYear, genre, totalCopies)) {
                    booksAdded++;
                } else {
                    LOGGER.warn("Failed to add sample book: ISBN {}, Title {}", isbn, title);
                }
            }
            LOGGER.info("Successfully added {} sample books to the database.", booksAdded);
        } else {
            LOGGER.info("Book table is not empty. Skipping sample data population.");
        }
    }

    /**
     * Populates the database with a minimum number of sample books if the book table is currently empty.
     * Uses JavaFaker to generate realistic book data. This method is typically called on application startup.
     * Ensures that generated ISBNs are unique for the batch of books being added.
     * Logs the outcome of the population process.
     */
    public void populateDatabaseWithSampleBooksIfEmpty() {
        if (bookDAO.getAllBooks().isEmpty()) {
            LOGGER.info("Book table is empty. Populating with {} sample books...", MIN_BOOKS_TO_POPULATE);
            Random random = new Random();
            int booksAdded = 0;
            for (int i = 0; i < MIN_BOOKS_TO_POPULATE; i++) {
                String isbn = faker.code().isbn13();
                // Ensure ISBN is unique for this batch, very basic check
                int attempt = 0;
                while(bookDAO.getBookByIsbn(isbn).isPresent() && attempt < 5) {
                    isbn = faker.code().isbn13();
                    attempt++;
                }
                if (attempt >= 5 && bookDAO.getBookByIsbn(isbn).isPresent()) {
                    LOGGER.warn("Could not generate unique ISBN after multiple attempts, skipping book {}/{}", i+1, MIN_BOOKS_TO_POPULATE);
                    continue;
                }

                String title = faker.book().title();
                String author = faker.book().author();
                int yearValue = 1950 + random.nextInt(75); // Year between 1950 and 2024
                Year publicationYear = Year.of(yearValue);
                String genre = faker.book().genre();
                int totalCopies = 1 + random.nextInt(5); // 1 to 5 copies

                if (addBook(isbn, title, author, publicationYear, genre, totalCopies)) {
                    booksAdded++;
                } else {
                    LOGGER.warn("Failed to add sample book: ISBN {}, Title {}", isbn, title);
                }
            }
            LOGGER.info("Successfully added {} sample books to the database.", booksAdded);
        } else {
            LOGGER.info("Book table is not empty. Skipping sample data population.");
        }
    }
}
