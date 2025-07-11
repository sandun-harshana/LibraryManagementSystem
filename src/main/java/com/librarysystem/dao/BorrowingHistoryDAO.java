package com.librarysystem.dao;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.BorrowingRecord; // Assuming a BorrowingRecord model
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// We need a model class for BorrowingRecord. Let's define a simple one here
// or assume it will be created in the model package.
// For now, let's assume:
// package com.librarysystem.model;
// import java.time.LocalDateTime;
// public class BorrowingRecord {
//     private int borrowId;
//     private int bookId;
//     private int userId;
//     private LocalDateTime borrowDate;
//     private LocalDateTime dueDate;
//     private LocalDateTime returnDate;
//     // Constructors, getters, setters
// }


public class BorrowingHistoryDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(BorrowingHistoryDAO.class);

    // Inner class or separate file for BorrowingRecord if not already defined
    // For this exercise, I'll define it conceptually.
    // In a real project, com.librarysystem.model.BorrowingRecord would exist.

    public boolean addBorrowingRecord(BorrowingRecord record) {
        String sql = "INSERT INTO BorrowingHistory (book_id, user_id, borrow_date, due_date, return_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, record.getBookId());
            pstmt.setInt(2, record.getUserId());
            pstmt.setTimestamp(3, Timestamp.valueOf(record.getBorrowDate()));
            pstmt.setTimestamp(4, Timestamp.valueOf(record.getDueDate()));
            if (record.getReturnDate() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(record.getReturnDate()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        record.setBorrowId(generatedKeys.getInt(1)); // Assuming BorrowingRecord has setBorrowId
                        LOGGER.info("Borrowing record added successfully: ID {}", record.getBorrowId());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error adding borrowing record for book_id {} and user_id {}", record.getBookId(), record.getUserId(), e);
        }
        return false;
    }

    public Optional<BorrowingRecord> getBorrowingRecordById(int borrowId) {
        String sql = "SELECT borrow_id, book_id, user_id, borrow_date, due_date, return_date FROM BorrowingHistory WHERE borrow_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, borrowId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching borrowing record by ID: {}", borrowId, e);
        }
        return Optional.empty();
    }

    public List<BorrowingRecord> getBorrowingHistoryForUser(int userId) {
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT borrow_id, book_id, user_id, borrow_date, due_date, return_date FROM BorrowingHistory WHERE user_id = ? ORDER BY borrow_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching borrowing history for user_id: {}", userId, e);
        }
        return records;
    }

    public List<BorrowingRecord> getBorrowingHistoryForBook(int bookId) {
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT borrow_id, book_id, user_id, borrow_date, due_date, return_date FROM BorrowingHistory WHERE book_id = ? ORDER BY borrow_date DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching borrowing history for book_id: {}", bookId, e);
        }
        return records;
    }

    public List<BorrowingRecord> getCurrentlyBorrowedBooksByUser(int userId) {
        List<BorrowingRecord> records = new ArrayList<>();
        String sql = "SELECT borrow_id, book_id, user_id, borrow_date, due_date, return_date FROM BorrowingHistory WHERE user_id = ? AND return_date IS NULL ORDER BY due_date ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching currently borrowed books for user_id: {}", userId, e);
        }
        return records;
    }

    public Optional<BorrowingRecord> getLatestBorrowingRecordForBook(int bookId, int userId) {
        String sql = "SELECT borrow_id, book_id, user_id, borrow_date, due_date, return_date FROM BorrowingHistory " +
                     "WHERE book_id = ? AND user_id = ? AND return_date IS NULL ORDER BY borrow_date DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBorrowingRecord(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching latest borrowing record for book_id {} and user_id {}", bookId, userId, e);
        }
        return Optional.empty();
    }


    public boolean updateBorrowingRecord(BorrowingRecord record) {
        // Typically, you'd update the return_date when a book is returned.
        String sql = "UPDATE BorrowingHistory SET book_id = ?, user_id = ?, borrow_date = ?, due_date = ?, return_date = ? WHERE borrow_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, record.getBookId());
            pstmt.setInt(2, record.getUserId());
            pstmt.setTimestamp(3, Timestamp.valueOf(record.getBorrowDate()));
            pstmt.setTimestamp(4, Timestamp.valueOf(record.getDueDate()));
            if (record.getReturnDate() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(record.getReturnDate()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }
            pstmt.setInt(6, record.getBorrowId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("Borrowing record updated successfully: ID {}", record.getBorrowId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error updating borrowing record: ID {}", record.getBorrowId(), e);
        }
        return false;
    }

    private BorrowingRecord mapRowToBorrowingRecord(ResultSet rs) throws SQLException {
        // This assumes BorrowingRecord class exists and has this constructor/setters
        int borrowId = rs.getInt("borrow_id");
        int bookId = rs.getInt("book_id");
        int userId = rs.getInt("user_id");
        LocalDateTime borrowDate = rs.getTimestamp("borrow_date").toLocalDateTime();
        LocalDateTime dueDate = rs.getTimestamp("due_date").toLocalDateTime();
        Timestamp returnDateTimestamp = rs.getTimestamp("return_date");
        LocalDateTime returnDate = (returnDateTimestamp != null) ? returnDateTimestamp.toLocalDateTime() : null;

        // Assuming a constructor: new BorrowingRecord(borrowId, bookId, userId, borrowDate, dueDate, returnDate)
        // Or using setters if default constructor and setters are available.
        BorrowingRecord record = new BorrowingRecord(bookId, userId, borrowDate, dueDate); // Basic constructor
        record.setBorrowId(borrowId);
        record.setReturnDate(returnDate);
        return record;
    }
}
