package com.librarysystem.service;

import com.librarysystem.dao.BookDAO;
import com.librarysystem.model.Book;
import com.github.javafaker.Faker; // For generating test data if needed

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookDAO bookDAO;

    // @Mock // LogService is instantiated directly in BookService constructor if not injected
    // private LogService logService;

    @InjectMocks
    private BookService bookService;

    @Mock
    private LogService mockLogService; // We'll need to inject this manually or use a spy on real LogService if BookService instantiates it.
                                      // For now, let's assume BookService can take LogService in constructor or setter for testing.
                                      // The current BookService constructor takes LogService.

    private Book sampleBook;
    private Faker faker;

    @BeforeEach
    void setUp() {
        // Re-initialize BookService with the mocked LogService if @InjectMocks doesn't cover it due to new()
        // This is tricky if BookService news up its own LogService.
        // For better testability, BookService should allow LogService injection.
        // The constructor BookService(BookDAO, LogService) is good for this.
        // @InjectMocks should handle this if the constructor is `public BookService(BookDAO bookDAO, LogService logService)`

        faker = new Faker();
        sampleBook = new Book(
                faker.code().isbn13(),
                faker.book().title(),
                faker.book().author(),
                Year.of(faker.number().numberBetween(1900, 2023)),
                faker.book().genre(),
                5
        );
        sampleBook.setBookIdPk(1); // Assume it has a PK
    }

    @Test
    void testAddBook_Success() {
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.empty());
        when(bookDAO.addBook(any(Book.class))).thenReturn(true);

        boolean result = bookService.addBook(sampleBook.getIsbn(), sampleBook.getTitle(), sampleBook.getAuthor(),
                sampleBook.getPublicationYear(), sampleBook.getGenre(), sampleBook.getTotalCopies());

        assertTrue(result, "Adding a new book should succeed.");
        verify(bookDAO).addBook(any(Book.class));
        verify(mockLogService).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_ADDED), anyString());
    }

    @Test
    void testAddBook_DuplicateIsbn() {
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));

        boolean result = bookService.addBook(sampleBook.getIsbn(), sampleBook.getTitle(), sampleBook.getAuthor(),
                sampleBook.getPublicationYear(), sampleBook.getGenre(), sampleBook.getTotalCopies());

        assertFalse(result, "Adding a book with a duplicate ISBN should fail.");
        verify(bookDAO, never()).addBook(any(Book.class)); // Ensure addBook is not called
        verify(mockLogService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testAddBook_InvalidParameters_ReturnsFalse() {
        assertFalse(bookService.addBook("", "Title", "Author", Year.now(), "Genre", 1)); // Empty ISBN
        assertFalse(bookService.addBook("123", "", "Author", Year.now(), "Genre", 1));     // Empty Title
        assertFalse(bookService.addBook("123", "Title", "", Year.now(), "Genre", 1));      // Empty Author
        assertFalse(bookService.addBook("123", "Title", "Author", null, "Genre", 1));    // Null Year
        assertFalse(bookService.addBook("123", "Title", "Author", Year.now(), "Genre", -1)); // Negative copies
        verify(mockLogService, never()).recordLog(any(), any(), anyString());
    }


    @Test
    void testFindBookByIsbn_Exists() {
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        Optional<Book> foundBook = bookService.findBookByIsbn(sampleBook.getIsbn());
        assertTrue(foundBook.isPresent(), "Book should be found by ISBN.");
        assertEquals(sampleBook.getTitle(), foundBook.get().getTitle());
    }

    @Test
    void testFindBookByIsbn_NotExists() {
        when(bookDAO.getBookByIsbn("nonexistentisbn")).thenReturn(Optional.empty());
        Optional<Book> foundBook = bookService.findBookByIsbn("nonexistentisbn");
        assertFalse(foundBook.isPresent(), "Book should not be found for a nonexistent ISBN.");
    }

    @Test
    void testFindBookByIsbn_NullOrEmptyIsbn_ReturnsEmpty() {
        Optional<Book> foundBookNull = bookService.findBookByIsbn(null);
        assertFalse(foundBookNull.isPresent(), "Should not find book with null ISBN.");
        Optional<Book> foundBookEmpty = bookService.findBookByIsbn("");
        assertFalse(foundBookEmpty.isPresent(), "Should not find book with empty ISBN.");
         verify(bookDAO, never()).getBookByIsbn(anyString());
    }

    @Test
    void testGetAllBooks() {
        List<Book> books = new ArrayList<>();
        books.add(sampleBook);
        when(bookDAO.getAllBooks()).thenReturn(books);

        List<Book> result = bookService.getAllBooks();
        assertFalse(result.isEmpty(), "Should return a list of books.");
        assertEquals(1, result.size());
    }

    @Test
    void testUpdateBookDetails_Success() {
        when(bookDAO.getBookByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));
        when(bookDAO.updateBook(any(Book.class))).thenReturn(true);

        boolean result = bookService.updateBookDetails(sampleBook.getIsbn(), "New Title", null, null, null, null);
        assertTrue(result, "Updating book details should succeed.");
        verify(bookDAO).updateBook(argThat(book -> book.getTitle().equals("New Title")));
        verify(mockLogService).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_UPDATED), anyString());
    }

    @Test
    void testUpdateBookDetails_BookNotFound() {
        when(bookDAO.getBookByIsbn("nonexistentisbn")).thenReturn(Optional.empty());
        boolean result = bookService.updateBookDetails("nonexistentisbn", "New Title", null, null, null, null);
        assertFalse(result, "Updating non-existent book should fail.");
        verify(bookDAO, never()).updateBook(any(Book.class));
        verify(mockLogService, never()).recordLog(any(), any(), anyString());
    }


    @Test
    void testRemoveBook_Success() {
        when(bookDAO.deleteBook(sampleBook.getIsbn())).thenReturn(true);
        boolean result = bookService.removeBook(sampleBook.getIsbn());
        assertTrue(result, "Removing a book should succeed.");
        verify(bookDAO).deleteBook(sampleBook.getIsbn());
        verify(mockLogService).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_REMOVED), anyString());
    }

    @Test
    void testRemoveBook_NullOrEmptyIsbn_ReturnsFalse() {
        assertFalse(bookService.removeBook(null));
        assertFalse(bookService.removeBook(""));
        verify(bookDAO, never()).deleteBook(anyString());
        verify(mockLogService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testPopulateDatabaseWithSampleBooksIfEmpty_WhenEmpty() {
        when(bookDAO.getAllBooks()).thenReturn(new ArrayList<>()); // Simulate empty DB
        when(bookDAO.getBookByIsbn(anyString())).thenReturn(Optional.empty()); // New books don't exist yet
        when(bookDAO.addBook(any(Book.class))).thenReturn(true); // Simulate successful add

        bookService.populateDatabaseWithSampleBooksIfEmpty();

        verify(bookDAO, times(100)).addBook(any(Book.class)); // Use literal 100
        // Also verify logging for each book added
        verify(mockLogService, times(100)).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_ADDED), anyString()); // Use literal 100
    }

    @Test
    void testPopulateDatabaseWithSampleBooksIfEmpty_WhenNotEmpty() {
        List<Book> existingBooks = new ArrayList<>();
        existingBooks.add(sampleBook); // Simulate non-empty DB
        when(bookDAO.getAllBooks()).thenReturn(existingBooks);

        bookService.populateDatabaseWithSampleBooksIfEmpty();

        verify(bookDAO, never()).addBook(any(Book.class)); // Should not add any books
        verify(mockLogService, never()).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.BOOK_ADDED), anyString());
    }
}
