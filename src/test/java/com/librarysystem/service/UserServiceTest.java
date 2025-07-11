package com.librarysystem.service;

import com.librarysystem.dao.UserDAO;
import com.librarysystem.dao.AccountDAO;
import com.librarysystem.model.User;
import com.librarysystem.model.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private AccountDAO accountDAO;

    @Mock
    private LogService logService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private String rawPassword = "password123";
    private String hashedPassword = "salt:hashedPassword"; // Dummy hashed password

    @BeforeEach
    void setUp() {
        sampleUser = new User("testUser", hashedPassword, User.Role.MEMBER);
        sampleUser.setUserId(1); // Assume DAO sets this after adding
    }

    @Test
    void testRegisterUser_Success() {
        when(userDAO.getUserByUsername("newUser")).thenReturn(Optional.empty());
        // Mock PasswordUtil.hashPassword
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(rawPassword)).thenReturn(hashedPassword);

            // Capture the User argument to addUser to simulate ID generation
            doAnswer(invocation -> {
                User userArg = invocation.getArgument(0);
                userArg.setUserId(2); // Simulate DB generating an ID
                return true;
            }).when(userDAO).addUser(any(User.class));

            when(accountDAO.createAccount(any(Account.class))).thenReturn(true);

            boolean result = userService.registerUser("newUser", rawPassword, User.Role.MEMBER);

            assertTrue(result, "User registration should succeed.");
            verify(userDAO).addUser(argThat(user -> "newUser".equals(user.getUsername()) && hashedPassword.equals(user.getPasswordHash())));
            verify(accountDAO).createAccount(argThat(acc -> acc.getUserId() == 2 && acc.getFinesDue().equals(BigDecimal.ZERO)));
            verify(logService).recordLog(eq(2), eq(com.librarysystem.model.LogEntry.ActionType.ACCOUNT_CREATED), anyString());
        }
    }

    @Test
    void testRegisterUser_UsernameExists() {
        when(userDAO.getUserByUsername("testUser")).thenReturn(Optional.of(sampleUser));
        boolean result = userService.registerUser("testUser", rawPassword, User.Role.MEMBER);
        assertFalse(result, "Registration with existing username should fail.");
        verify(logService, never()).recordLog(any(), any(), anyString());
    }

    @Test
    void testRegisterUser_AccountCreationFails() {
         when(userDAO.getUserByUsername("newUser")).thenReturn(Optional.empty());
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.hashPassword(rawPassword)).thenReturn(hashedPassword);

            doAnswer(invocation -> {
                User userArg = invocation.getArgument(0);
                userArg.setUserId(2);
                return true;
            }).when(userDAO).addUser(any(User.class));

            when(accountDAO.createAccount(any(Account.class))).thenReturn(false); // Simulate account creation failure

            boolean result = userService.registerUser("newUser", rawPassword, User.Role.MEMBER);

            assertFalse(result, "Registration should fail if account creation fails.");
            verify(userDAO).deleteUser(2); // Verify rollback (user deletion)
            verify(logService, never()).recordLog(any(), eq(com.librarysystem.model.LogEntry.ActionType.ACCOUNT_CREATED), anyString());
        }
    }

    @Test
    void testRegisterUser_InvalidInput() {
        assertFalse(userService.registerUser(null, "pass", User.Role.MEMBER));
        assertFalse(userService.registerUser("", "pass", User.Role.MEMBER));
        assertFalse(userService.registerUser("user", null, User.Role.MEMBER));
        assertFalse(userService.registerUser("user", "", User.Role.MEMBER));
        verify(logService, never()).recordLog(any(), any(), anyString());
    }


    @Test
    void testLoginUser_Success() {
        when(userDAO.getUserByUsername("testUser")).thenReturn(Optional.of(sampleUser));
        try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword(rawPassword, hashedPassword)).thenReturn(true);
            Optional<User> result = userService.loginUser("testUser", rawPassword);
            assertTrue(result.isPresent(), "Login should succeed with correct credentials.");
            assertEquals("testUser", result.get().getUsername());
            verify(logService).recordLog(eq(sampleUser.getUserId()), eq(com.librarysystem.model.LogEntry.ActionType.LOGIN_SUCCESS), anyString());
        }
    }

    @Test
    void testLoginUser_UserNotFound() {
        when(userDAO.getUserByUsername("unknownUser")).thenReturn(Optional.empty());
        Optional<User> result = userService.loginUser("unknownUser", rawPassword);
        assertFalse(result.isPresent(), "Login should fail if user not found.");
        verify(logService).recordLog(eq(null), eq(com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE), contains("user not found"));
    }

    @Test
    void testLoginUser_IncorrectPassword() {
        when(userDAO.getUserByUsername("testUser")).thenReturn(Optional.of(sampleUser));
         try (MockedStatic<PasswordUtil> mockedPasswordUtil = Mockito.mockStatic(PasswordUtil.class)) {
            mockedPasswordUtil.when(() -> PasswordUtil.verifyPassword("wrongPassword", hashedPassword)).thenReturn(false);
            Optional<User> result = userService.loginUser("testUser", "wrongPassword");
            assertFalse(result.isPresent(), "Login should fail with incorrect password.");
            verify(logService).recordLog(eq(sampleUser.getUserId()), eq(com.librarysystem.model.LogEntry.ActionType.LOGIN_FAILURE), contains("incorrect password"));
        }
    }

     @Test
    void testLoginUser_InvalidInput() {
        assertFalse(userService.loginUser(null, "pass").isPresent());
        assertFalse(userService.loginUser("", "pass").isPresent());
        assertFalse(userService.loginUser("user", null).isPresent());
        assertFalse(userService.loginUser("user", "").isPresent());
        verify(logService, never()).recordLog(any(), eq(com.librarysystem.model.LogEntry.ActionType.LOGIN_SUCCESS), anyString());
        // LOGIN_FAILURE might be logged by the service for these cases based on its internal checks.
    }
}
