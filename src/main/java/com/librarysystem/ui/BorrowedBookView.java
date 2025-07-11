package com.librarysystem.ui;

import com.librarysystem.model.BorrowingRecord;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDateTime;

// ViewModel for displaying borrowed book details in a TableView
public class BorrowedBookView {
    private final SimpleIntegerProperty borrowId;
    private final SimpleIntegerProperty bookId; // The PK of the book
    private final SimpleStringProperty isbn;
    private final SimpleStringProperty title;
    private final SimpleStringProperty author;
    private final SimpleObjectProperty<LocalDateTime> borrowDate;
    private final SimpleObjectProperty<LocalDateTime> dueDate;

    public BorrowedBookView(BorrowingRecord record, String isbn, String title, String author) {
        this.borrowId = new SimpleIntegerProperty(record.getBorrowId());
        this.bookId = new SimpleIntegerProperty(record.getBookId());
        this.isbn = new SimpleStringProperty(isbn);
        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.borrowDate = new SimpleObjectProperty<>(record.getBorrowDate());
        this.dueDate = new SimpleObjectProperty<>(record.getDueDate());
    }

    public int getBorrowId() {
        return borrowId.get();
    }
    public SimpleIntegerProperty borrowIdProperty() {
        return borrowId;
    }

    public int getBookId() {
        return bookId.get();
    }
    public SimpleIntegerProperty bookIdProperty() {
        return bookId;
    }

    public String getIsbn() {
        return isbn.get();
    }
    public SimpleStringProperty isbnProperty() {
        return isbn;
    }

    public String getTitle() {
        return title.get();
    }
    public SimpleStringProperty titleProperty() {
        return title;
    }

    public String getAuthor() {
        return author.get();
    }
    public SimpleStringProperty authorProperty() {
        return author;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate.get();
    }
    public SimpleObjectProperty<LocalDateTime> borrowDateProperty() {
        return borrowDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate.get();
    }
    public SimpleObjectProperty<LocalDateTime> dueDateProperty() {
        return dueDate;
    }
}
