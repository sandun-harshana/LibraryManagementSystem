package com.librarysystem.service;

import com.librarysystem.dao.BookDAO;
import com.librarysystem.dao.UserDAO;
import com.librarysystem.dao.BorrowingHistoryDAO;
import com.librarysystem.model.Book;
import com.librarysystem.model.User;
import com.librarysystem.model.BorrowingRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.Collections; // For empty list

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceTest {

    @Mock private BookDAO bookDAO;
    @Mock private UserDAO userDAO;
    @Mock private BorrowingHistoryDAO borrowingHistoryDAO;
    @Mock private LogService logService;

    @InjectMocks
    private LibraryService libraryService;

    private User sampleUser;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1, "testUser", "hashedPass", User.Role.MEMBER);
        sampleBook = new Book("1234567890", "Test Book", "Test Author", Year.of(2021), "Fiction", 2);
        sampleBook.setBookIdPk(101); // Set the DB primary key
        sampleBook.setAvailableCopies(1); // Assume 1 copy is available
    }

    @Test
    void testBorrowBook_Success() {
        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        when(borrowingHistoryDAO.getCurrentlyBorrowedBooksByUser(sampleUser.getUserId())).thenReturn(Collections.emptyList());
        when(bookDAO.updateBook(any(Book.class))).thenReturn(true);
        when(borrowingHistoryDAO.addBorrowingRecord(any(BorrowingRecord.class))).thenReturn(true);

        boolean result = libraryService.borrowBook(sampleUser.getUserId(), sampleBook.getIsbn());

        assertTrue(result, "Borrowing book should succeed.");
        assertEquals(0, sampleBook.getAvailableCopies(), "Available copies should decrement.");
        verify(bookDAO).updateBook(sampleBook);
        verify(borrowingHistoryDAO).addBorrowingRecord(any(BorrowingRecord.class));
        verify(logService).recordLog(eq(sampleUser.getUserId()), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_BORROW), anyString());
    }

    @Test
    void testBorrowBook_UserNotFound() {
        when(userDAO.getUserById(99)).thenReturn(Optional.empty()); // Non-existent user
        boolean result = libraryService.borrowBook(99, sampleBook.getIsbn());
        assertFalse(result, "Borrowing should fail if user not found.");
        verify(logService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testBorrowBook_BookNotFound() {
        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn("nonexistentisbn")).thenReturn(Optional.empty());
        boolean result = libraryService.borrowBook(sampleUser.getUserId(), "nonexistentisbn");
        assertFalse(result, "Borrowing should fail if book not found.");
         verify(logService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testBorrowBook_NoCopiesAvailable() {
        sampleBook.setAvailableCopies(0); // No copies available
        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));

        boolean result = libraryService.borrowBook(sampleUser.getUserId(), sampleBook.getIsbn());
        assertFalse(result, "Borrowing should fail if no copies are available.");
        verify(bookDAO, never()).updateBook(any(Book.class)); // Book shouldn't be updated
        verify(logService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testBorrowBook_BookIdPkMissing() {
        sampleBook.setBookIdPk(0); // Simulate PK not being set
        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        // No need to mock updateBook or addBorrowingRecord as it should fail before that

        boolean result = libraryService.borrowBook(sampleUser.getUserId(), sampleBook.getIsbn());

        assertFalse(result, "Borrowing should fail if book's primary key is missing.");
        verify(bookDAO).updateBook(sampleBook); // It will try to update, then rollback
        verify(borrowingHistoryDAO, never()).addBorrowingRecord(any(BorrowingRecord.class));
        verify(logService, never()).recordLog(any(), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_BORROW), anyString());
    }


    @Test
    void testReturnBook_Success() {
        BorrowingRecord activeRecord = new BorrowingRecord(sampleBook.getBookIdPk(), sampleUser.getUserId(), LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(9));
        activeRecord.setBorrowId(1); // Simulate DB ID

        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        when(borrowingHistoryDAO.getLatestBorrowingRecordForBook(sampleBook.getBookIdPk(), sampleUser.getUserId()))
            .thenReturn(Optional.of(activeRecord));
        when(borrowingHistoryDAO.updateBorrowingRecord(any(BorrowingRecord.class))).thenReturn(true);
        when(bookDAO.updateBook(any(Book.class))).thenReturn(true);

        // Initial state for available copies before return
        int initialAvailableCopies = sampleBook.getAvailableCopies();

        boolean result = libraryService.returnBook(sampleUser.getUserId(), sampleBook.getIsbn());

        assertTrue(result, "Returning book should succeed.");
        assertNotNull(activeRecord.getReturnDate(), "Return date should be set on the record.");
        assertEquals(initialAvailableCopies + 1, sampleBook.getAvailableCopies(), "Available copies should increment.");
        verify(bookDAO).updateBook(sampleBook);
        verify(borrowingHistoryDAO).updateBorrowingRecord(activeRecord);
        verify(logService).recordLog(eq(sampleUser.getUserId()), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_RETURN), anyString());
    }

    @Test
    void testReturnBook_NoActiveBorrowingRecord() {
        when(userDAO.getUserById(sampleUser.getUserId())).thenReturn(Optional.of(sampleUser));
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        when(borrowingHistoryDAO.getLatestBorrowingRecordForBook(sampleBook.getBookIdPk(), sampleUser.getUserId()))
            .thenReturn(Optional.empty()); // No active record

        boolean result = libraryService.returnBook(sampleUser.getUserId(), sampleBook.getIsbn());
        assertFalse(result, "Returning should fail if no active borrowing record found.");
        verify(logService, never()).recordLog(any(), any(), anyString());
    }
}
