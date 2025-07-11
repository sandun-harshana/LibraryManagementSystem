package com.librarysystem.model;

import java.time.Year;

public class Book {
    private String isbn;
    private String title;
    private String author;
    private Year publicationYear;
    private String genre;
    private boolean isAvailable;
    private int totalCopies;
    private int availableCopies;
    private int bookIdPk; // Database Primary Key

    public Book(String isbn, String title, String author, Year publicationYear, String genre, int totalCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies; // Initially all copies are available
        this.isAvailable = totalCopies > 0;
    }

    // Getters
    public int getBookIdPk() {
        return bookIdPk;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Year getPublicationYear() {
        return publicationYear;
    }

    public String getGenre() {
        return genre;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    // Setters
    public void setBookIdPk(int bookIdPk) {
        this.bookIdPk = bookIdPk;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setPublicationYear(Year publicationYear) {
        this.publicationYear = publicationYear;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
        // Potentially update availableCopies and isAvailable status
        if (this.availableCopies > totalCopies) {
            this.availableCopies = totalCopies;
        }
        this.isAvailable = this.availableCopies > 0;
    }

    public void setAvailableCopies(int availableCopies) {
        if (availableCopies < 0 || availableCopies > this.totalCopies) {
            throw new IllegalArgumentException("Available copies cannot be negative or exceed total copies.");
        }
        this.availableCopies = availableCopies;
        this.isAvailable = this.availableCopies > 0;
    }

    // toString for easy printing
    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publicationYear=" + publicationYear +
                ", genre='" + genre + '\'' +
                ", totalCopies=" + totalCopies +
                ", availableCopies=" + availableCopies +
                ", isAvailable=" + isAvailable +
                '}';
    }
}
