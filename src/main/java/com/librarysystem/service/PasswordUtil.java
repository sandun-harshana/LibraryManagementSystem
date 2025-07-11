package com.librarysystem.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16; // 128 bits

    // In a real application, use a well-tested library like BCrypt, SCrypt, or Argon2.
    // This is a simplified example for demonstration.

    /**
     * Generates a salt and hashes the password with it.
     * @param password The plain text password.
     * @return A string containing the salt and hash, typically concatenated or in a specific format.
     *         Format here: salt:hash (Base64 encoded)
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt); // Apply salt first
            byte[] hashedPassword = md.digest(password.getBytes());

            // Store salt and hash together. Common practice is to prepend salt to the hash.
            // Or store them in separate columns in the database.
            // For simplicity, we'll combine them as Base64 strings.
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashedPasswordBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            return saltBase64 + ":" + hashedPasswordBase64;
        } catch (NoSuchAlgorithmException e) {
            // This should ideally not happen if SHA-256 is available.
            throw new RuntimeException("Failed to hash password due to missing algorithm.", e);
        }
    }

    /**
     * Verifies a plain text password against a stored salt and hash.
     * @param plainPassword The plain text password to verify.
     * @param storedPasswordHash The stored string containing salt and hash (e.g., from the database).
     *                           Expected format: salt:hash (Base64 encoded)
     * @return True if the password matches, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String storedPasswordHash) {
        if (plainPassword == null || plainPassword.isEmpty() || storedPasswordHash == null || storedPasswordHash.isEmpty()) {
            return false;
        }
        try {
            String[] parts = storedPasswordHash.split(":");
            if (parts.length != 2) {
                // Invalid stored hash format
                System.err.println("Invalid stored password hash format.");
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt); // Apply the same salt
            byte[] actualHash = md.digest(plainPassword.getBytes());

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to verify password due to missing algorithm.", e);
        } catch (IllegalArgumentException e) {
            // Base64 decoding error
            System.err.println("Error decoding stored password hash: " + e.getMessage());
            return false;
        }
    }

    // Simple test
    public static void main(String[] args) {
        String pass = "password123";
        String hashed = hashPassword(pass);
        System.out.println("Stored hash: " + hashed);
        System.out.println("Verification (correct): " + verifyPassword(pass, hashed));
        System.out.println("Verification (incorrect): " + verifyPassword("wrongpassword", hashed));
    }
}
