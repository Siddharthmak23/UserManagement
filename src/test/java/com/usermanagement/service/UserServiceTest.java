package com.usermanagement.service;

import com.usermanagement.dao.UserDao;
import com.usermanagement.dto.UserRequest;
import com.usermanagement.dto.UserResponse;
import com.usermanagement.exception.ApiException;
import com.usermanagement.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao, passwordEncoder, new UserValidator());
    }

    @Test
    void createStoresABcryptHashAndReturnsASafeUser() {
        when(userDao.emailExists("ada@example.com", null)).thenReturn(false);
        when(userDao.insert(any(User.class))).thenReturn(8L);
        when(userDao.findById(8L)).thenReturn(Optional.of(savedUser(8L, "ada@example.com", "USER", "hash")));

        UserResponse response = userService.create(request("Secret123", "USER"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).insert(captor.capture());
        assertFalse(captor.getValue().getPasswordHash().contains("Secret123"));
        assertTrue(passwordEncoder.matches("Secret123", captor.getValue().getPasswordHash()));
        assertEquals(8L, response.id());
        assertEquals("ada@example.com", response.email());
    }

    @Test
    void createRejectsADuplicateEmail() {
        when(userDao.emailExists("ada@example.com", null)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class,
                () -> userService.create(request("Secret123", "USER")));

        assertEquals(409, exception.getStatus());
        verify(userDao, never()).insert(any());
    }

    @Test
    void updateKeepsTheExistingPasswordWhenTheFieldIsBlank() {
        when(userDao.findByIdForUpdate(4L)).thenReturn(Optional.of(savedUser(4L, "ada@example.com", "USER", "old-hash")));
        when(userDao.emailExists("ada@example.com", 4L)).thenReturn(false);
        when(userDao.update(eq(4L), eq("Ada Lovelace"), eq("ada@example.com"), eq("(555) 010-0199"), eq("USER"), isNull()))
                .thenReturn(1);
        when(userDao.findById(4L)).thenReturn(Optional.of(savedUser(4L, "ada@example.com", "USER", "old-hash")));

        UserRequest update = request("", "USER");
        userService.update(4L, update, 99L);

        verify(userDao).update(eq(4L), eq("Ada Lovelace"), eq("ada@example.com"), eq("(555) 010-0199"), eq("USER"), isNull());
    }

    @Test
    void updateHashesAReplacementPassword() {
        when(userDao.findByIdForUpdate(4L)).thenReturn(Optional.of(savedUser(4L, "ada@example.com", "USER", "old-hash")));
        when(userDao.emailExists("ada@example.com", 4L)).thenReturn(false);
        when(userDao.update(eq(4L), eq("Ada Lovelace"), eq("ada@example.com"), eq("(555) 010-0199"), eq("USER"), any()))
                .thenReturn(1);
        when(userDao.findById(4L)).thenReturn(Optional.of(savedUser(4L, "ada@example.com", "USER", "new-hash")));

        userService.update(4L, request("NewSecret1", "USER"), 99L);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userDao).update(eq(4L), eq("Ada Lovelace"), eq("ada@example.com"), eq("(555) 010-0199"), eq("USER"), hash.capture());
        assertTrue(passwordEncoder.matches("NewSecret1", hash.getValue()));
    }

    @Test
    void administratorCannotChangeOwnRoleOrDeleteOwnAccount() {
        when(userDao.findByIdForUpdate(5L)).thenReturn(Optional.of(savedUser(5L, "admin@example.com", "ADMIN", "hash")));

        ApiException roleChange = assertThrows(ApiException.class,
                () -> userService.update(5L, request("Secret123", "USER"), 5L));
        ApiException selfDelete = assertThrows(ApiException.class, () -> userService.delete(5L, 5L));

        assertEquals(409, roleChange.getStatus());
        assertEquals("You cannot change your own role", roleChange.getMessage());
        assertEquals("You cannot delete the currently logged-in administrator", selfDelete.getMessage());
    }

    @Test
    void lastAdministratorCannotBeDemotedOrDeletedBySomeoneElse() {
        when(userDao.findByIdForUpdate(5L)).thenReturn(Optional.of(savedUser(5L, "admin@example.com", "ADMIN", "hash")));
        when(userDao.countByRoleForUpdate("ADMIN")).thenReturn(1L);

        ApiException demotion = assertThrows(ApiException.class,
                () -> userService.update(5L, request("Secret123", "USER"), 9L));
        ApiException deletion = assertThrows(ApiException.class, () -> userService.delete(5L, 9L));

        assertEquals("Cannot change the role of the last administrator", demotion.getMessage());
        assertEquals("Cannot delete the last administrator", deletion.getMessage());
        verify(userDao, never()).deleteById(5L);
    }

    @Test
    void listPassesTheSearchThroughAndCalculatesPages() {
        when(userDao.count("ada")).thenReturn(25L);
        when(userDao.findPage("ada", 10, 10)).thenReturn(List.of(savedUser(3L, "ada@example.com", "USER", "hash")));

        var page = userService.list(2, 10, "ada");

        assertEquals(25L, page.total());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.users().size());
        assertEquals("ada@example.com", page.users().get(0).email());
    }

    @Test
    void missingUserIsA404() {
        when(userDao.findById(44L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> userService.get(44L));

        assertEquals(404, exception.getStatus());
    }

    private static UserRequest request(String password, String role) {
        UserRequest request = new UserRequest();
        request.setName("Ada Lovelace");
        request.setEmail("Ada@Example.com");
        request.setPhone("(555) 010-0199");
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    private static User savedUser(long id, String email, String role, String hash) {
        return new User(id, "Ada Lovelace", email, "(555) 010-0199", hash, role,
                LocalDateTime.of(2026, 10, 9, 12, 0), LocalDateTime.of(2026, 10, 9, 12, 0));
    }
}
