package com.librarysystem.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    void testPasswordHashingAndVerification() {
        String originalPassword = "mySecretPassword123!";
        String hashedPassword = PasswordUtil.hashPassword(originalPassword);

        assertNotNull(hashedPassword, "Hashed password should not be null.");
        assertNotEquals(originalPassword, hashedPassword, "Hashed password should not be the same as the original.");

        assertTrue(PasswordUtil.verifyPassword(originalPassword, hashedPassword), "Verification of correct password should succeed.");
        assertFalse(PasswordUtil.verifyPassword("wrongPassword", hashedPassword), "Verification of incorrect password should fail.");
    }

    @Test
    void testVerificationWithDifferentSalts() {
        String password = "commonPassword";
        String hashedPassword1 = PasswordUtil.hashPassword(password);
        String hashedPassword2 = PasswordUtil.hashPassword(password);

        // Hashes should be different due to different salts
        assertNotEquals(hashedPassword1, hashedPassword2, "Hashes of the same password with different salts should not be equal.");

        assertTrue(PasswordUtil.verifyPassword(password, hashedPassword1), "Password should verify against its own hash (1).");
        assertTrue(PasswordUtil.verifyPassword(password, hashedPassword2), "Password should verify against its own hash (2).");
    }

    @Test
    void testVerifyNullOrEmptyPasswords() {
        String hashedPassword = PasswordUtil.hashPassword("test");
        assertFalse(PasswordUtil.verifyPassword(null, hashedPassword), "Verification with null plain password should fail.");
        assertFalse(PasswordUtil.verifyPassword("", hashedPassword), "Verification with empty plain password should fail.");
        assertFalse(PasswordUtil.verifyPassword("test", null), "Verification with null stored hash should fail.");
        assertFalse(PasswordUtil.verifyPassword("test", ""), "Verification with empty stored hash should fail.");
    }

    @Test
    void testVerifyMalformedStoredHash() {
        assertFalse(PasswordUtil.verifyPassword("test", "malformedhash"), "Verification with malformed stored hash should fail.");
        assertFalse(PasswordUtil.verifyPassword("test", "saltwithoutcolon"), "Verification with stored hash missing colon should fail.");
        assertFalse(PasswordUtil.verifyPassword("test", ":hashwithoutSalt"), "Verification with stored hash missing salt part should fail.");
        assertFalse(PasswordUtil.verifyPassword("test", "salt:"), "Verification with stored hash missing hash part should fail.");
         // Invalid Base64 characters
        assertFalse(PasswordUtil.verifyPassword("test", "salt:!@#$"), "Verification with invalid Base64 in hash part should fail.");

    }

    @Test
    void testHashPasswordNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null), "Hashing null password should throw IllegalArgumentException.");
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""), "Hashing empty password should throw IllegalArgumentException.");
    }
}
