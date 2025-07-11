package com.librarysystem.model;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

// This class might be more integrated with User or represent a library member's specific account details.
// For now, let's assume an Account is closely tied to a User and holds borrowing-specific info.
public class Account {
    private int accountId; // Could be same as userId or a separate ID
    private int userId; // Foreign key to User table
    // private List<BorrowedBook> borrowedBooks; // We'll handle this via a separate table or join
    private BigDecimal finesDue;

    public Account(int userId) {
        this.userId = userId;
        // this.borrowedBooks = new ArrayList<>();
        this.finesDue = BigDecimal.ZERO;
    }

    public Account(int accountId, int userId, BigDecimal finesDue) {
        this.accountId = accountId;
        this.userId = userId;
        this.finesDue = finesDue;
    }

    // Getters
    public int getAccountId() {
        return accountId;
    }

    public int getUserId() {
        return userId;
    }

    public BigDecimal getFinesDue() {
        return finesDue;
    }

    // Setters
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setFinesDue(BigDecimal finesDue) {
        this.finesDue = finesDue;
    }

    public void addFine(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.finesDue = this.finesDue.add(amount);
        }
    }

    public void payFine(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.finesDue = this.finesDue.subtract(amount);
            if (this.finesDue.compareTo(BigDecimal.ZERO) < 0) {
                this.finesDue = BigDecimal.ZERO;
            }
        }
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", userId=" + userId +
                ", finesDue=" + finesDue +
                '}';
    }
}
