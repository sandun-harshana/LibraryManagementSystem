package com.librarysystem.service;

import com.librarysystem.dao.UserDAO;
import com.librarysystem.dao.AccountDAO;
import com.librarysystem.model.User;
import com.librarysystem.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List; // Added import

public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;
    private final AccountDAO accountDAO;
    private final LogService logService; // For logging actions

    public UserService() {
        // In a real app, use dependency injection (e.g., Spring)
        this.userDAO = new UserDAO();
        this.accountDAO = new AccountDAO();
        this.logService = new LogService();
    }

    // Constructor for testing with mocks
    public UserService(UserDAO userDAO, AccountDAO accountDAO, LogService logService) {
        this.userDAO = userDAO;
        this.accountDAO = accountDAO;
        this.logService = logService;
    }

    /**
     * Registers a new user in the system.
     * Validates username and password, checks for existing username, hashes the password,
     * adds the user to the database, and creates an associated account.
     * Logs the registration action.
     *
     * @param username The username for the new user. Must not be null or empty.
     * @param password The plain text password for the new user. Must not be null or empty.
     * @param role The role for the new user (e.g., MEMBER, ADMIN).
     * @return {@code true} if registration is successful, {@code false} otherwise (e.g., username taken, DB error).
     */
    public boolean registerUser(String username, String password, User.Role role) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            LOGGER.warn("Registration attempt with invalid username or password.");
            return false;
        }

        if (userDAO.getUserByUsername(username).isPresent()) {
            LOGGER.warn("Registration attempt for already existing username: {}", username);
            return false;
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(username, passwordHash, role);

        boolean userAdded = userDAO.addUser(newUser);
        if (userAdded && newUser.getUserId() > 0) { // User ID should be populated by DAO
            // Create an associated account
            Account newAccount = new Account(newUser.getUserId());
            newAccount.setFinesDue(BigDecimal.ZERO); // Initialize fines
            boolean accountCreated = accountDAO.createAccount(newAccount);

            if (accountCreated) {
                LOGGER.info("User registered successfully: {}", username);
                logService.recordLog(newUser.getUserId(), com.librarysystem.model.LogEntry.ActionType.ACCOUNT_CREATED, "User registered: " + username);
                return true;
            } else {
                LOGGER.error("User {} registered, but failed to create associated account. Rolling back user creation is recommended.", username);
                // In a real transactional system, user creation would be rolled back.
                // For now, we'll delete the user if account creation fails.
                userDAO.deleteUser(newUser.getUserId());
                LOGGER.info("Rolled back user creation for {} due to account creation failure.", username);
                return false;
            }
        } else {
            LOGGER.error("Failed to register user: {}", username);
            return false;
        }
    }

    public Optional<User> loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            LOGGER.warn("Login attempt with invalid username or password.");
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                LOGGER.info("User login successful: {}", username);
                logService.recordLog(user.getUserId(), com.librarysystem.model.LogEntry.ActionType.LOGIN_SUCCESS, "User login: " + username);
                return Optional.of(user);
            } else {
                LOGGER.warn("User login failed (incorrect password) for username: {}", username);
                logService.recordLog(user.getUserId(), com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE, "Failed login attempt (incorrect password) for username: " + username);
            }
        } else {
            LOGGER.warn("User login failed (user not found) for username: {}", username);
            logService.recordLog(null, com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE, "Failed login attempt (user not found): " + username);
        }
        return Optional.empty();
    }

    /**
     * Attempts to log in a user with the given username and password.
     * Verifies the password against the stored hash.
     * Logs login success or failure.
     *
     * @param username The username of the user attempting to log in.
     * @param password The plain text password.
     * @return An {@code Optional<User>} containing the User object if login is successful,
     *         or an empty Optional if login fails (user not found or password incorrect).
     */
    public Optional<User> loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            LOGGER.warn("Login attempt with invalid username or password.");
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                LOGGER.info("User login successful: {}", username);
                logService.recordLog(user.getUserId(), com.librarysystem.model.LogEntry.ActionType.LOGIN_SUCCESS, "User login: " + username);
                return Optional.of(user);
            } else {
                LOGGER.warn("User login failed (incorrect password) for username: {}", username);
                logService.recordLog(user.getUserId(), com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE, "Failed login attempt (incorrect password) for username: " + username);
            }
        } else {
            LOGGER.warn("User login failed (user not found) for username: {}", username);
            logService.recordLog(null, com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE, "Failed login attempt (user not found): " + username);
        }
        return Optional.empty();
    }

    public Optional<User> findUserByUsername(String username) {
        return userDAO.getUserByUsername(username);
    }

    public Optional<User> findUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    public List<User> getAllUsers() { // Added for Admin UI
        return userDAO.getAllUsers();
    }

    public boolean changeUserPassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            LOGGER.warn("Attempt to change password to an empty password for user ID: {}", userId);
            return false;
        }
        Optional<User> userOpt = userDAO.getUserById(userId);
        if (userOpt.isEmpty()) {
            LOGGER.warn("Attempt to change password for non-existent user ID: {}", userId);
            return false;
        }
        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            LOGGER.warn("Password change attempt failed (old password incorrect) for user ID: {}", userId);
            return false;
        }

        user.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        boolean updated = userDAO.updateUser(user);
        if (updated) {
            LOGGER.info("Password changed successfully for user ID: {}", userId);
            logService.recordLog(userId, com.librarysystem.model.LogEntry.ActionType.ACCOUNT_UPDATED, "Password changed for user ID: " + userId);
        } else {
            LOGGER.error("Failed to update password in database for user ID: {}", userId);
        }
        return updated;
    }

    // Admin function
    public boolean updateUserRole(int userId, User.Role newRole, User adminUser) {
        // Basic check if the acting user is an admin
        if (adminUser == null || adminUser.getRole() != User.Role.ADMIN) {
            LOGGER.warn("Non-admin user (ID: {}) attempted to change role for user ID: {}", adminUser != null ? adminUser.getUserId() : "null", userId);
            return false;
        }

        Optional<User> userOpt = userDAO.getUserById(userId);
        if (userOpt.isEmpty()) {
            LOGGER.warn("Admin (ID: {}) attempted to change role for non-existent user ID: {}", adminUser.getUserId(), userId);
            return false;
        }
        User userToUpdate = userOpt.get();
        userToUpdate.setRole(newRole);
        boolean updated = userDAO.updateUser(userToUpdate);

        if (updated) {
            LOGGER.info("Admin (ID: {}) successfully changed role for user ID: {} to {}", adminUser.getUserId(), userId, newRole);
            logService.recordLog(adminUser.getUserId(), com.librarysystem.model.LogEntry.ActionType.ACCOUNT_UPDATED, "Admin (ID: " + adminUser.getUserId() + ") changed role for user ID: " + userId + " to " + newRole);
        } else {
            LOGGER.error("Admin (ID: {}) failed to update role for user ID: {}", adminUser.getUserId(), userId);
        }
        return updated;
    }
}
