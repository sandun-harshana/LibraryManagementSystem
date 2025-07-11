package com.librarysystem.dao;

import com.librarysystem.db.DatabaseUtil;
import com.librarysystem.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogEntryDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogEntryDAO.class);

    public boolean addLogEntry(LogEntry logEntry) {
        String sql = "INSERT INTO Logging (log_timestamp, user_id, action_type, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(logEntry.getTimestamp()));
            if (logEntry.getUserId() != null) {
                pstmt.setInt(2, logEntry.getUserId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, logEntry.getActionType().name());
            pstmt.setString(4, logEntry.getDetails());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        logEntry.setLogId(generatedKeys.getInt(1));
                        // No need to log the log entry itself to avoid recursion,
                        // but for debugging, one might temporarily enable it.
                        // LOGGER.info("Log entry added successfully: ID {}", logEntry.getLogId());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            // Avoid using LOGGER.error here if this DAO is used by the logger itself,
            // to prevent potential infinite loops if DB connection fails.
            // For now, we assume SLF4J is configured with a non-DB appender (e.g., console, file)
            // for its own internal logging, so this should be safe.
            System.err.println("Error adding log entry: " + e.getMessage());
            // LOGGER.error("Error adding log entry for action: {}", logEntry.getActionType(), e);
        }
        return false;
    }

    public List<LogEntry> getAllLogEntries() {
        List<LogEntry> logEntries = new ArrayList<>();
        String sql = "SELECT log_id, log_timestamp, user_id, action_type, details FROM Logging ORDER BY log_timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logEntries.add(mapRowToLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching all log entries.", e);
        }
        return logEntries;
    }

    public List<LogEntry> getLogEntriesByUserId(int userId) {
        List<LogEntry> logEntries = new ArrayList<>();
        String sql = "SELECT log_id, log_timestamp, user_id, action_type, details FROM Logging WHERE user_id = ? ORDER BY log_timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logEntries.add(mapRowToLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching log entries for user_id: {}", userId, e);
        }
        return logEntries;
    }

    public List<LogEntry> getLogEntriesByActionType(LogEntry.ActionType actionType) {
        List<LogEntry> logEntries = new ArrayList<>();
        String sql = "SELECT log_id, log_timestamp, user_id, action_type, details FROM Logging WHERE action_type = ? ORDER BY log_timestamp DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, actionType.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logEntries.add(mapRowToLogEntry(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Error fetching log entries for action type: {}", actionType, e);
        }
        return logEntries;
    }

    private LogEntry mapRowToLogEntry(ResultSet rs) throws SQLException {
        int logId = rs.getInt("log_id");
        LocalDateTime timestamp = rs.getTimestamp("log_timestamp").toLocalDateTime();
        Integer userId = rs.getInt("user_id");
        if (rs.wasNull()) {
            userId = null;
        }
        LogEntry.ActionType actionType = LogEntry.ActionType.valueOf(rs.getString("action_type"));
        String details = rs.getString("details");

        LogEntry entry = new LogEntry(userId, actionType, details);
        entry.setLogId(logId);
        entry.setTimestamp(timestamp); // Set timestamp from DB as it might have DB-specific precision or default value
        return entry;
    }
}
