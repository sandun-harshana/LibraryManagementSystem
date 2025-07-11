package com.librarysystem.service;

import com.librarysystem.dao.LogEntryDAO;
import com.librarysystem.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogService.class);
    private final LogEntryDAO logEntryDAO;

    public LogService() {
        // In a real app, use dependency injection
        this.logEntryDAO = new LogEntryDAO();
    }

    // Constructor for testing with mocks
    public LogService(LogEntryDAO logEntryDAO) {
        this.logEntryDAO = logEntryDAO;
    }

    /**
     * Records a log entry into the database.
     * If saving to the database fails, an error is logged to the application's fallback logger (e.g., console).
     *
     * @param userId      The ID of the user performing the action. Can be null for system actions.
     * @param actionType  The type of action performed.
     * @param details     Additional details about the action.
     */
    public void recordLog(Integer userId, LogEntry.ActionType actionType, String details) {
        if (actionType == null) {
            LOGGER.warn("Log attempt with null actionType. Details: {}", details);
            // Optionally log this failed attempt to a fallback logger if critical
            return;
        }

        LogEntry logEntry = new LogEntry(userId, actionType, details);
        boolean success = logEntryDAO.addLogEntry(logEntry);

        if (!success) {
            // If DB logging fails, log to application log as a fallback.
            // This is important so that log information isn't completely lost.
            LOGGER.error("Failed to save log to database! Log details - UserID: {}, Action: {}, Details: {}",
                         userId, actionType, details);
        } else {
            // Optional: Could log to application log that DB log was successful, but might be too verbose.
            // LOGGER.debug("Action logged to database: UserID: {}, Action: {}, Details: {}", userId, actionType, details);
        }
    }

    /**
     * Retrieves all log entries from the database.
     * Primarily for admin use.
     * @return A list of all log entries.
     */
    public List<LogEntry> getAllLogs() {
        // Add security check here if needed: only admins should call this.
        // For now, assuming it's called from a context where admin rights are verified.
        return logEntryDAO.getAllLogEntries();
    }

    /**
     * Retrieves log entries for a specific user.
     * @param userId The ID of the user.
     * @return A list of log entries for the specified user.
     */
    public List<LogEntry> getLogsByUserId(int userId) {
        // Security check: User might only be ableto see their own logs, or admin can see any.
        return logEntryDAO.getLogEntriesByUserId(userId);
    }

    /**
     * Retrieves log entries for a specific action type.
     * @param actionType The type of action.
     * @return A list of log entries for the specified action type.
     */
    public List<LogEntry> getLogsByActionType(LogEntry.ActionType actionType) {
        // Security check.
        return logEntryDAO.getLogEntriesByActionType(actionType);
    }
}
