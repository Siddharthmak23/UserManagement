package com.usermanagement.service;

import com.usermanagement.config.AdminProperties;
import com.usermanagement.dao.UserDao;
import com.usermanagement.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserDao userDao;

    @Test
    void seedsAHashedAdministratorWhenMissing() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AdminSeeder seeder = new AdminSeeder(userDao, encoder, new UserValidator(), properties());
        when(userDao.findByEmail("admin@usermanagement.local")).thenReturn(Optional.empty());

        seeder.ensureAdmin();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).insert(captor.capture());
        User admin = captor.getValue();
        assertEquals("admin@usermanagement.local", admin.getEmail());
        assertEquals("ADMIN", admin.getRole());
        assertTrue(encoder.matches("Admin@12345", admin.getPasswordHash()));
    }

    @Test
    void doesNotResetAnExistingAdministrator() {
        AdminSeeder seeder = new AdminSeeder(userDao, new BCryptPasswordEncoder(), new UserValidator(), properties());
        when(userDao.findByEmail("admin@usermanagement.local")).thenReturn(Optional.of(new User()));

        seeder.ensureAdmin();

        verify(userDao, never()).insert(any());
    }

    @Test
    void rejectsInvalidAdministratorSettings() {
        AdminProperties properties = properties();
        properties.setPassword("weak");
        AdminSeeder seeder = new AdminSeeder(userDao, new BCryptPasswordEncoder(), new UserValidator(), properties);

        assertThrows(IllegalStateException.class, seeder::ensureAdmin);
        verify(userDao, never()).insert(any());
    }

    private static AdminProperties properties() {
        AdminProperties properties = new AdminProperties();
        properties.setEmail("Admin@UserManagement.local");
        properties.setPassword("Admin@12345");
        properties.setName("System Administrator");
        properties.setPhone("5550100199");
        return properties;
    }
}
