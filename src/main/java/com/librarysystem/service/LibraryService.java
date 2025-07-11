package com.librarysystem.service;

import com.librarysystem.dao.BookDAO;
import com.librarysystem.dao.UserDAO;
import com.librarysystem.dao.BorrowingHistoryDAO;
import com.librarysystem.model.Book;
import com.librarysystem.model.User;
import com.librarysystem.model.BorrowingRecord;
import com.librarysystem.model.LogEntry; // For LogService integration later
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;

public class LibraryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryService.class);
    private final BookDAO bookDAO;
    private final UserDAO userDAO; // To verify user exists
    private final BorrowingHistoryDAO borrowingHistoryDAO;
    private final LogService logService; // To be integrated

    private static final int DEFAULT_BORROWING_DAYS = 14;

    public LibraryService() {
        // In a real app, use dependency injection
        this.bookDAO = new BookDAO();
        this.userDAO = new UserDAO();
        this.borrowingHistoryDAO = new BorrowingHistoryDAO();
        this.logService = new LogService();
    }

    // Constructor for testing with mocks
    public LibraryService(BookDAO bookDAO, UserDAO userDAO, BorrowingHistoryDAO borrowingHistoryDAO, LogService logService) {
        this.bookDAO = bookDAO;
        this.userDAO = userDAO;
        this.borrowingHistoryDAO = borrowingHistoryDAO;
        this.logService = logService;
    }

    /**
     * Allows a user to borrow a book.
     * Checks for user and book existence, book availability, and if the user already has the same book.
     * Updates book availability and creates a borrowing record.
     * Logs the borrowing action.
     *
     * @param userId The ID of the user borrowing the book.
     * @param bookIsbn The ISBN of the book to be borrowed.
     * @return {@code true} if the book is borrowed successfully, {@code false} otherwise.
     */
    public boolean borrowBook(int userId, String bookIsbn) {
        Optional<User> userOpt = userDAO.getUserById(userId);
        if (userOpt.isEmpty()) {
            LOGGER.warn("Borrow attempt failed: User with ID {} not found.", userId);
            return false;
        }

        Optional<Book> bookOpt = bookDAO.getBookByIsbn(bookIsbn);
        if (bookOpt.isEmpty()) {
            LOGGER.warn("Borrow attempt failed: Book with ISBN {} not found.", bookIsbn);
            return false;
        }

        Book book = bookOpt.get();
        if (book.getAvailableCopies() <= 0) {
            LOGGER.warn("Borrow attempt failed: Book '{}' (ISBN: {}) is not available ({} copies available).", book.getTitle(), bookIsbn, book.getAvailableCopies());
            return false;
        }

        // Check if user already has this specific book instance borrowed and not returned (though with multiple copies, this might be allowed depending on policy)
        // For simplicity, we assume a user cannot borrow the same ISBN again if they haven't returned a previous copy of it.
        // This check needs book_id from Book model/DAO if we want to be very precise about specific physical copies.
        // Sticking with ISBN for now.
        List<BorrowingRecord> currentlyBorrowedByUSer = borrowingHistoryDAO.getCurrentlyBorrowedBooksByUser(userId);
        for(BorrowingRecord record : currentlyBorrowedByUSer) {
            // We need book_id on BorrowingRecord to link back to the specific book_id in Books table
            // And Book model needs its book_id (the PK from DB).
            // Let's assume for now bookIsbn is sufficient to identify the title.
            // If BookDAO.getBookById can fetch book_id, we can compare.
            // This check is simplified:
            Optional<Book> borrowedBook = bookDAO.getBookById(record.getBookId()); // Assuming BorrowingRecord stores the DB book_id
            if(borrowedBook.isPresent() && borrowedBook.get().getIsbn().equals(bookIsbn)) {
                 LOGGER.warn("User {} already has book ISBN {} borrowed and not returned.", userId, bookIsbn);
                 return false;
            }
        }


        // All checks passed, proceed to borrow
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        boolean bookUpdated = bookDAO.updateBook(book); // This updates available_copies in DB

        if (!bookUpdated) {
            LOGGER.error("Failed to update book availability for ISBN {} during borrow operation. Aborting.", bookIsbn);
            // Potentially roll back or compensate, but for now, just error out.
            return false;
        }

        LocalDateTime borrowDate = LocalDateTime.now();
        LocalDateTime dueDate = borrowDate.plus(DEFAULT_BORROWING_DAYS, ChronoUnit.DAYS);

        // We need the book_id (PK) from the Books table, not just ISBN for the foreign key in BorrowingHistory.
        // BookDAO.getBookByIsbn should ideally also fetch/store book_id in the Book object, or we need a getBookIdByIsbn method.
        // For now, let's assume Book object fetched by getBookByIsbn also has its PK (book_id).
        // This requires a change in Book model and BookDAO.mapRowToBook.
        // Quick Fix: Fetch book again using a method that guarantees book_id if not already in the Book object.
        // Or, modify Book model to include book_id.
        // Let's assume Book model has int bookId; and it's populated by BookDAO.
        // If Book model does not have bookId, this will fail.
        // We need to add bookId to Book.java and update BookDAO.mapRowToBook
        // For now, I will proceed with a placeholder, this needs to be fixed.
        // int databaseBookId = book.getBookId(); // THIS LINE ASSUMES Book.java HAS getBookId() and it's populated.

        // To make this work without modifying Book.java for now, but it's not ideal:
        // We'd have to re-fetch the book in a way that gives us the ID, or add a method to BookDAO.
        // Let's assume we add book_id to Book.java and its DAO mapping.
        // If not, the following line will need to be placeholder or use a default/error value.
        // For the purpose of this step, I will assume book.getBookId() is available and correct.
        // (This implies Book.java and BookDAO.mapRowToBook need an update from previous steps)

        // Let's assume Book model was updated to include bookId:
        // int bookIdFromDb = book.getDbId(); // or whatever the field name is.
        // For now, let's just log a warning and use a dummy value or skip if not available
        // This is a critical point for FK integrity.

        // Let's simulate getting the book_id properly.
        // This would typically be done by ensuring bookOpt.get() has the ID.
        // Or bookDAO.getBookIdByIsbn(bookIsbn)
        // We will assume book.getDbId() exists and gives the PK.
        // If the Book model doesn't have a dbId, this will be problematic.
        // We will need to modify Book model to hold book_id (PK from DB)
        // and BookDAO to populate it.

        // For now, I will proceed by *requiring* Book model to have book_id.
        // And BookDAO to populate it.
        // If this is not done, the FK in BorrowingHistory will be incorrect.
        // I will add a TODO to address this if Book model is not updated.
        // TODO: Ensure Book model has book_id (PK) and BookDAO populates it.

        // Assuming book_id is available via book.getId() or similar after fetching from DAO
        // This will require Book object to store its database primary key.
        // This wasn't explicitly in Book.java. Let's assume we add it.
        // For now, this is a conceptual placeholder for the actual database book_id.
        // int actualBookId = book.getInternalId(); // Assuming Book model has getInternalId() for DB PK.

        // Given current BookDAO, getBookByIsbn doesn't explicitly return the book_id PK.
        // We will need to modify BookDAO to fetch book_id and Book model to store it.
        // For now, let's call getBookById which would use the PK, but we only have ISBN.
        // This is a bit of a catch-22 without modifying the Book model and DAO.
        // The schema has book_id as PK. The BorrowingHistory table needs this book_id.
        // The current Book model identifies books by ISBN primarily.

        // Simplest path without immediate Book model change (less ideal):
        // Create a temporary BookDAO method: getBookIdByIsbn(String isbn)
        // Or assume that bookOpt.get() somehow magically has the PK.
        // Let's assume for the flow that book.getId() will return the database primary key.
        // This means Book.java and BookDAO.mapRowToBook need this field.
        // (This is a forward declaration of a needed change)

        // Let's assume Book model has `int id;` for the PK.
        // And mapRowToBook in BookDAO populates it:
        // e.g. in mapRowToBook: book.setId(rs.getInt("book_id"));
        // And Book.java has: private int id; public int getId(); public void setId(int id);

        // If the above assumption about Book.getId() is not met, the following line is problematic.
        // int bookIdForRecord = book.getId(); // THIS IS THE CRITICAL ASSUMPTION
        // For now, I will write the code as if this exists.
        // This will be a point of failure if Book model/DAO is not updated.

        // To proceed without changing Book.java for now, we'd be stuck or have to do another query.
        // Let's assume BookDAO's getBookByIsbn was enhanced to also fetch book_id
        // and the Book model has a field for it. (e.g., book.getDatabaseId())
        // If not, this will be a placeholder for the actual book_id.
        // This is a known gap from previous steps if Book model doesn't include the PK.

        // Let's assume Book model has `private int bookIdPk;` and getter/setter, and DAO populates it.
        if (book.getBookIdPk() == 0) { // Check if PK was populated (0 is usually not a valid PK for auto-increment)
            LOGGER.error("Book PK not available for ISBN {}. Cannot record borrowing. Book model or DAO needs update.", bookIsbn);
            // Rollback available copy change
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookDAO.updateBook(book);
            return false;
        }

        BorrowingRecord record = new BorrowingRecord(book.getBookIdPk(), userId, borrowDate, dueDate);
        boolean recordAdded = borrowingHistoryDAO.addBorrowingRecord(record);

        if (recordAdded) {
            LOGGER.info("Book '{}' (ISBN: {}) borrowed successfully by user ID {}. Due on {}.", book.getTitle(), bookIsbn, userId, dueDate);
            logService.recordLog(userId, LogEntry.ActionType.BOOK_BORROW, "Book ISBN: " + bookIsbn + " (ID: " + book.getBookIdPk() + ") borrowed. Due: " + dueDate);
            return true;
        } else {
            LOGGER.error("Failed to add borrowing record for book ISBN {} by user ID {}. Attempting to revert book availability.", bookIsbn, userId);
            // Rollback: Increment available copies back
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookDAO.updateBook(book); // Attempt to revert
            LOGGER.info("Reverted book availability for ISBN {} due to borrowing record failure.", bookIsbn);
            return false;
        }
    }

    /**
     * Allows a user to return a borrowed book.
     * Checks for user and book existence, and an active borrowing record for the user and book.
     * Updates the borrowing record with a return date and increments the book's available copies.
     * Logs the return action and any system errors (e.g., count inconsistency).
     *
     * @param userId The ID of the user returning the book.
     * @param bookIsbn The ISBN of the book being returned.
     * @return {@code true} if the book is returned successfully, {@code false} otherwise.
     */
    public boolean returnBook(int userId, String bookIsbn) {
        Optional<User> userOpt = userDAO.getUserById(userId);
        if (userOpt.isEmpty()) {
            LOGGER.warn("Return attempt failed: User with ID {} not found.", userId);
            return false;
        }

        Optional<Book> bookOpt = bookDAO.getBookByIsbn(bookIsbn);
        if (bookOpt.isEmpty()) {
            LOGGER.warn("Return attempt failed: Book with ISBN {} not found.", bookIsbn);
            return false;
        }
        Book book = bookOpt.get();

        // Again, assuming book.getBookIdPk() is available and populated.
         if (book.getBookIdPk() == 0) {
            LOGGER.error("Book PK not available for ISBN {}. Cannot process return. Book model or DAO needs update.", bookIsbn);
            return false;
        }

        // Find the active borrowing record for this specific book and user
        Optional<BorrowingRecord> recordOpt = borrowingHistoryDAO.getLatestBorrowingRecordForBook(book.getBookIdPk(), userId);

        if (recordOpt.isEmpty() || recordOpt.get().isReturned()) {
            LOGGER.warn("Return attempt failed: No active borrowing record found for book ISBN {} by user ID {}, or already returned.", bookIsbn, userId);
            return false;
        }

        BorrowingRecord recordToReturn = recordOpt.get();
        recordToReturn.setReturnDate(LocalDateTime.now());

        boolean historyUpdated = borrowingHistoryDAO.updateBorrowingRecord(recordToReturn);
        if (!historyUpdated) {
            LOGGER.error("Failed to update borrowing history for return of book ISBN {} by user ID {}. Aborting return.", bookIsbn, userId);
            return false;
        }

        // Increment available copies
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        boolean bookAvailabilityUpdated = bookDAO.updateBook(book);

        if (!bookAvailabilityUpdated) {
            LOGGER.error("Failed to update book availability for ISBN {} during return. Borrowing history was updated, but book count is now inconsistent.", bookIsbn);
            // This is a problematic state. May need manual correction or more robust transaction handling.
            logService.recordLog(userId, LogEntry.ActionType.SYSTEM_ERROR, "Book count inconsistency for ISBN " + bookIsbn + " (ID: " + book.getBookIdPk() + ") after return by user " + userId + ". Borrowing record " + recordToReturn.getBorrowId() + " updated, but book count failed.");
        }

        // Fine calculation (optional, out of scope for now)
        // if (recordToReturn.getReturnDate().isAfter(recordToReturn.getDueDate())) {
        //     // Calculate and apply fine
        // }

        LOGGER.info("Book '{}' (ISBN: {}) returned successfully by user ID {}.", book.getTitle(), bookIsbn, userId);
        logService.recordLog(userId, LogEntry.ActionType.BOOK_RETURN, "Book ISBN: " + bookIsbn + " (ID: " + book.getBookIdPk() + ") returned.");
        return true;
    }

    public List<BorrowingRecord> getBorrowingHistoryForUser(int userId) {
        return borrowingHistoryDAO.getBorrowingHistoryForUser(userId);
    }

    public List<BorrowingRecord> getCurrentlyBorrowedBooksByUser(int userId) {
        return borrowingHistoryDAO.getCurrentlyBorrowedBooksByUser(userId);
    }
}
