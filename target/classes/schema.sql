-- Database schema for Library Management System

-- Users Table
CREATE TABLE IF NOT EXISTS Users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('MEMBER', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Books Table
CREATE TABLE IF NOT EXISTS Books (
    book_id INT PRIMARY KEY AUTO_INCREMENT,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publication_year YEAR,
    genre VARCHAR(100),
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_available_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- Accounts Table
-- Stores user-specific library account information like fines.
-- Assumes a one-to-one relationship with Users.
CREATE TABLE IF NOT EXISTS Accounts (
    account_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    fines_due DECIMAL(10, 2) DEFAULT 0.00,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

-- BorrowingHistory Table
-- Tracks current and past book borrowings.
CREATE TABLE IF NOT EXISTS BorrowingHistory (
    borrow_id INT PRIMARY KEY AUTO_INCREMENT,
    book_id INT NOT NULL,
    user_id INT NOT NULL,
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP NULL, -- NULL if the book has not been returned yet
    FOREIGN KEY (book_id) REFERENCES Books(book_id) ON DELETE RESTRICT, -- Prevent deleting a book if it's part of borrowing history (or use ON DELETE SET NULL if preferred)
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE -- If user is deleted, their borrowing history is also deleted (alternatively, could be ON DELETE RESTRICT or SET NULL)
);

-- Logging Table
-- Stores logs for various system and user actions.
CREATE TABLE IF NOT EXISTS Logging (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    log_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id INT NULL, -- Can be NULL for system-level logs not tied to a specific user
    action_type VARCHAR(50) NOT NULL, -- e.g., 'BOOK_BORROW', 'LOGIN_SUCCESS', 'ACCOUNT_CREATED', 'SYSTEM_ERROR'
    details TEXT, -- Can store relevant information like ISBN, username, error messages, etc.
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL -- If a user is deleted, their logs are kept but user_id becomes NULL
);

-- Indexes for performance (optional but good practice)
CREATE INDEX IF NOT EXISTS idx_books_title ON Books(title);
CREATE INDEX IF NOT EXISTS idx_books_author ON Books(author);
CREATE INDEX IF NOT EXISTS idx_borrowing_book ON BorrowingHistory(book_id);
CREATE INDEX IF NOT EXISTS idx_borrowing_user ON BorrowingHistory(user_id);
CREATE INDEX IF NOT EXISTS idx_logging_action ON Logging(action_type);
CREATE INDEX IF NOT EXISTS idx_logging_user ON Logging(user_id);

-- Note:
-- The ENUM type for Users.role is MySQL specific.
-- The YEAR type for Books.publication_year is also MySQL specific.
-- `CONSTRAINT chk_available_copies` for Books table requires MySQL 8.0.16+.
--      For older versions, this check would need to be handled at the application level or via triggers.
-- Foreign Key `ON DELETE` actions (CASCADE, RESTRICT, SET NULL) are chosen based on desired referential integrity behavior.
-- `IF NOT EXISTS` is used for table creation to make the script idempotent.
-- `CREATE INDEX IF NOT EXISTS` requires MySQL 5.7.4+ or MariaDB 10.1.2+. For older versions, remove `IF NOT EXISTS`.

-- Example of inserting an admin user (password should be hashed in application)
-- INSERT INTO Users (username, password_hash, role) VALUES ('admin', 'hashed_password_example', 'ADMIN');
-- INSERT INTO Accounts (user_id) SELECT user_id FROM Users WHERE username = 'admin';
