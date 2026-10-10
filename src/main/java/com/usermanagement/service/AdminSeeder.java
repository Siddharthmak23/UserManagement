package com.usermanagement.service;

import com.usermanagement.config.AdminProperties;
import com.usermanagement.dao.UserDao;
import com.usermanagement.dto.UserRequest;
import com.usermanagement.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Creates the development administrator once. Later startups leave the existing
 * password hash unchanged. Only the BCrypt hash is stored.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final AdminProperties adminProperties;

    public AdminSeeder(UserDao userDao, PasswordEncoder passwordEncoder, UserValidator userValidator,
                       AdminProperties adminProperties) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAdmin();
    }

    void ensureAdmin() {
        UserRequest request = new UserRequest();
        request.setName(adminProperties.getName());
        request.setEmail(adminProperties.getEmail());
        request.setPhone(adminProperties.getPhone());
        request.setPassword(adminProperties.getPassword());
        request.setRole("ADMIN");
        Map<String, String> errors = userValidator.validateCreate(request);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Development administrator settings are invalid: " + errors.keySet());
        }
        String email = userValidator.normalizeEmail(request.getEmail());
        if (userDao.findByEmail(email).isPresent()) {
            log.info("Development administrator already exists for {}", email);
            return;
        }
        User admin = new User();
        admin.setName(userValidator.normalizeName(request.getName()));
        admin.setEmail(email);
        admin.setPhone(userValidator.normalizePhone(request.getPhone()));
        admin.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        admin.setRole("ADMIN");
        userDao.insert(admin);
        log.info("Created development administrator for {}", email);
    }
}
