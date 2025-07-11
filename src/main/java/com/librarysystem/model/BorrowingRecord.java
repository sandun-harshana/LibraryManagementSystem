package com.librarysystem.model;

import java.time.LocalDateTime;

public class BorrowingRecord {
    private int borrowId;
    private int bookId; // Foreign key to Books table (refers to Books.book_id)
    private int userId; // Foreign key to Users table
    private LocalDateTime borrowDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate; // Null if not yet returned

    // Constructor for creating a new record before saving to DB (borrowId is auto-generated)
    public BorrowingRecord(int bookId, int userId, LocalDateTime borrowDate, LocalDateTime dueDate) {
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null; // Default to not returned
    }

    // Constructor for loading a record from the DB (all fields available)
    public BorrowingRecord(int borrowId, int bookId, int userId, LocalDateTime borrowDate, LocalDateTime dueDate, LocalDateTime returnDate) {
        this.borrowId = borrowId;
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Getters
    public int getBorrowId() {
        return borrowId;
    }

    public int getBookId() {
        return bookId;
    }

    public int getUserId() {
        return userId;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    // Setters
    public void setBorrowId(int borrowId) { // Typically set after DB insertion
        this.borrowId = borrowId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isReturned() {
        return this.returnDate != null;
    }

    @Override
    public String toString() {
        return "BorrowingRecord{" +
                "borrowId=" + borrowId +
                ", bookId=" + bookId +
                ", userId=" + userId +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + (returnDate != null ? returnDate : "Not Returned") +
                '}';
    }
}
