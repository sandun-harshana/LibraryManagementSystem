package com.librarysystem.dao;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;
import java.math.BigDecimal;

public class AccountDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDAO.class);

    public boolean createAccount(Account account) {
        // Accounts might be created automatically when a User is created.
        // This method assumes an Account object is ready to be persisted.
        String sql = "INSERT INTO Accounts (user_id, fines_due) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, account.getUserId());
            pstmt.setBigDecimal(2, account.getFinesDue());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setAccountId(generatedKeys.getInt(1));
                        LOGGER.info("Account created successfully for user_id: {}", account.getUserId());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error creating account for user_id: {}", account.getUserId(), e);
        }
        return false;
    }

    public Optional<Account> getAccountByUserId(int userId) {
        String sql = "SELECT account_id, user_id, fines_due FROM Accounts WHERE user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToAccount(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching account by user_id: {}", userId, e);
        }
        return Optional.empty();
    }

    public Optional<Account> getAccountByAccountId(int accountId) {
        String sql = "SELECT account_id, user_id, fines_due FROM Accounts WHERE account_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToAccount(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching account by account_id: {}", accountId, e);
        }
        return Optional.empty();
    }

    public boolean updateAccount(Account account) {
        String sql = "UPDATE Accounts SET fines_due = ? WHERE account_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, account.getFinesDue());
            pstmt.setInt(2, account.getAccountId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("Account updated successfully for account_id: {}", account.getAccountId());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error updating account for account_id: {}", account.getAccountId(), e);
        }
        return false;
    }

    // Deleting an account might be tied to deleting a user (due to ON DELETE CASCADE)
    // So, an explicit deleteAccount might not always be needed if user deletion handles it.
    // However, providing it for completeness or specific scenarios.
    public boolean deleteAccount(int accountId) {
        String sql = "DELETE FROM Accounts WHERE account_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.info("Account deleted successfully: account_id {}", accountId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.error("Error deleting account: account_id {}", accountId, e);
        }
        return false;
    }

    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        return new Account(
                rs.getInt("account_id"),
                rs.getInt("user_id"),
                rs.getBigDecimal("fines_due")
        );
    }
}
