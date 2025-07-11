package com.librarysystem.dao;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookDAO.class);

    public boolean addBook(Book book) {
        String sql = "INSERT INTO Books (isbn, title, author, publication_year, genre, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Request generated keys
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setInt(4, book.getPublicationYear().getValue());
            pstmt.setString(5, book.getGenre());
            pstmt.setInt(6, book.getTotalCopies());
            pstmt.setInt(7, book.getAvailableCopies());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setBookIdPk(generatedKeys.getInt(1)); // Set the generated PK on the book object
                        LOGGER.info("Book added successfully: {} with ID {}", book.getTitle(), book.getBookIdPk());
                        return true;
                    } else {
                        LOGGER.warn("Book {} added, but failed to retrieve generated ID.", book.getTitle());
                        // Still counts as success for adding, but ID might be missing on object if not re-fetched
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error adding book: {}", book.getTitle(), e);
        }
        return false;
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        String sql = "SELECT book_id, isbn, title, author, publication_year, genre, total_copies, available_copies FROM Books WHERE isbn = ?"; // Added book_id
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching book by ISBN: {}", isbn, e);
        }
        return Optional.empty();
    }

    public Optional<Book> getBookById(int bookIdPk) { // Parameter renamed for clarity
        // This method assumes you might need to fetch by the auto-incremented book_id from the DB
        String sql = "SELECT book_id, isbn, title, author, publication_year, genre, total_copies, available_copies FROM Books WHERE book_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookIdPk);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBook(rs)); // mapRowToBook now handles book_id
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching book by internal ID: {}", bookIdPk, e);
        }
        return Optional.empty();
    }


    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT book_id, isbn, title, author, publication_year, genre, total_copies, available_copies FROM Books"; // Added book_id
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching all books.", e);
        }
        return books;
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE Books SET title = ?, author = ?, publication_year = ?, genre = ?, total_copies = ?, available_copies = ? WHERE isbn = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setInt(3, book.getPublicationYear().getValue());
            pstmt.setString(4, book.getGenre());
            pstmt.setInt(5, book.getTotalCopies());
            pstmt.setInt(6, book.getAvailableCopies());
            pstmt.setString(7, book.getIsbn());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("Book updated successfully: {}", book.getTitle());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error updating book: {}", book.getTitle(), e);
        }
        return false;
    }

    public boolean deleteBook(String isbn) {
        String sql = "DELETE FROM Books WHERE isbn = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn);
            int affectedRows = pstmt.executeUpdate();
             if (affectedRows > 0) {
                LOGGER.info("Book deleted successfully: ISBN {}", isbn);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting book: ISBN {}", isbn, e);
        }
        return false;
    }

    public List<Book> findBooksByTitle(String title) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT book_id, isbn, title, author, publication_year, genre, total_copies, available_copies FROM Books WHERE title LIKE ?"; // Added book_id
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + title + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding books by title: {}", title, e);
        }
        return books;
    }

    public List<Book> findBooksByAuthor(String author) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT book_id, isbn, title, author, publication_year, genre, total_copies, available_copies FROM Books WHERE author LIKE ?"; // Added book_id
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + author + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error finding books by author: {}", author, e);
        }
        return books;
    }


    // Renamed to mapRowToBook and implemented correctly
    private Book mapRowToBook(ResultSet rs) throws SQLException {
        Book book = new Book(
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                Year.of(rs.getInt("publication_year")),
                rs.getString("genre"),
                rs.getInt("total_copies")
        );
        book.setBookIdPk(rs.getInt("book_id")); // Set the database primary key
        // Crucially, set the available copies from the database state
        // This ensures the Book object accurately reflects the number of copies currently available in the library.
        book.setAvailableCopies(rs.getInt("available_copies"));
        return book;
    }
}
