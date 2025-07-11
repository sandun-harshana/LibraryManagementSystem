package com.librarysystem.dao;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);

    public Optional<User> getUserByUsername(String username) {
        String sql = "SELECT user_id, username, password_hash, role FROM Users WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching user by username: {}", username, e);
        }
        return Optional.empty();
    }

    public Optional<User> getUserById(int userId) {
        String sql = "SELECT user_id, username, password_hash, role FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching user by ID: {}", userId, e);
        }
        return Optional.empty();
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, password_hash, role FROM Users";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching all users.", e);
        }
        return users;
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO Users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole().name());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                        LOGGER.info("User added successfully: {}", user.getUsername());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error adding user: {}", user.getUsername(), e);
        }
        return false;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE Users SET username = ?, password_hash = ?, role = ? WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getRole().name());
            pstmt.setInt(4, user.getUserId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("User updated successfully: {}", user.getUsername());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error updating user: {}", user.getUsername(), e);
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("User deleted successfully: ID {}", userId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting user: ID {}", userId, e);
        }
        return false;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                User.Role.valueOf(rs.getString("role"))
        );
    }
}
