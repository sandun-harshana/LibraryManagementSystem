package com.library;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Library {
    private List<Book> books;
    private static final String FILE_NAME = "books.txt";

    public Library() {
        books = new ArrayList<>();
        loadBooksFromFile();
    }

    public void addBook(String title, String author) {
        books.add(new Book(title, author));
        saveBooksToFile();
    }

    public void viewBooks() {
        if (books.isEmpty()) {
            System.out.println("No books available.");
        } else {
            for (int i = 0; i < books.size(); i++) {
                System.out.println((i + 1) + ". " + books.get(i));
            }
        }
    }

    public void borrowBook(int index) {
        if (index >= 1 && index <= books.size()) {
            Book book = books.get(index - 1);
            if (book.isAvailable()) {
                book.setAvailable(false);
                saveBooksToFile();
                System.out.println("Book borrowed: " + book.getTitle());
            } else {
                System.out.println("Book is already borrowed.");
            }
        } else {
            System.out.println("Invalid book number.");
        }
    }

    public void returnBook(int index) {
        if (index >= 1 && index <= books.size()) {
            Book book = books.get(index - 1);
            if (!book.isAvailable()) {
                book.setAvailable(true);
                saveBooksToFile();
                System.out.println("Book returned: " + book.getTitle());
            } else {
                System.out.println("Book is already available.");
            }
        } else {
            System.out.println("Invalid book number.");
        }
    }

    private void saveBooksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Book book : books) {
                writer.write(book.getTitle() + "," + book.getAuthor() + "," + book.isAvailable());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving books: " + e.getMessage());
        }
    }

    private void loadBooksFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    Book book = new Book(parts[0], parts[1]);
                    book.setAvailable(Boolean.parseBoolean(parts[2]));
                    books.add(book);
                }
            }
        } catch (FileNotFoundException e) {
            // File doesn't exist yet; start with empty list
        } catch (IOException e) {
            System.out.println("Error loading books: " + e.getMessage());
        }
    }
}