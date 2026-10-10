package com.usermanagement.service;

import com.usermanagement.dao.UserDao;
import com.usermanagement.dto.StatsResponse;
import com.usermanagement.dto.UserPageResponse;
import com.usermanagement.dto.UserRequest;
import com.usermanagement.dto.UserResponse;
import com.usermanagement.exception.ApiException;
import com.usermanagement.model.Role;
import com.usermanagement.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;

    public UserService(UserDao userDao, PasswordEncoder passwordEncoder, UserValidator userValidator) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(int page, int size, String search) {
        if (page < 1) {
            throw new ApiException(400, "page must be 1 or greater", Map.of("page", "page must be 1 or greater"));
        }
        if (size < 1 || size > 50) {
            throw new ApiException(400, "size must be between 1 and 50", Map.of("size", "size must be between 1 and 50"));
        }
        long offsetLong = (long) (page - 1) * size;
        if (offsetLong > Integer.MAX_VALUE) {
            throw new ApiException(400, "Page is too large");
        }
        long total = userDao.count(search);
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        List<UserResponse> users = userDao.findPage(search, size, (int) offsetLong).stream()
                .map(UserResponse::from)
                .toList();
        return new UserPageResponse(users, page, size, total, totalPages);
    }

    @Transactional(readOnly = true)
    public UserResponse get(long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (request == null) {
            throw new ApiException(400, "Request body is required");
        }
        Map<String, String> errors = userValidator.validateCreate(request);
        if (!errors.isEmpty()) {
            throw new ApiException(400, "Validation failed", errors);
        }
        String email = userValidator.normalizeEmail(request.getEmail());
        if (userDao.emailExists(email, null)) {
            throw duplicateEmail();
        }
        User user = new User();
        user.setName(userValidator.normalizeName(request.getName()));
        user.setEmail(email);
        user.setPhone(userValidator.normalizePhone(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(userValidator.normalizeRole(request.getRole()));
        try {
            long id = userDao.insert(user);
            return UserResponse.from(requireUser(id));
        } catch (DuplicateKeyException ex) {
            throw duplicateEmail();
        }
    }

    @Transactional
    public UserResponse update(long id, UserRequest request, long actorId) {
        if (request == null) {
            throw new ApiException(400, "Request body is required");
        }
        Map<String, String> errors = userValidator.validateUpdate(request);
        if (!errors.isEmpty()) {
            throw new ApiException(400, "Validation failed", errors);
        }
        User existing = userDao.findByIdForUpdate(id).orElseThrow(this::missingUser);
        String newRole = userValidator.normalizeRole(request.getRole());
        if (existing.getId().equals(actorId) && !existing.getRole().equals(newRole)) {
            throw new ApiException(409, "You cannot change your own role");
        }
        if (Role.ADMIN.name().equals(existing.getRole()) && !Role.ADMIN.name().equals(newRole)
                && userDao.countByRoleForUpdate(Role.ADMIN.name()) <= 1) {
            throw new ApiException(409, "Cannot change the role of the last administrator");
        }
        String email = userValidator.normalizeEmail(request.getEmail());
        if (userDao.emailExists(email, id)) {
            throw duplicateEmail();
        }
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }
        try {
            int updated = userDao.update(
                    id,
                    userValidator.normalizeName(request.getName()),
                    email,
                    userValidator.normalizePhone(request.getPhone()),
                    newRole,
                    passwordHash);
            if (updated == 0) {
                throw missingUser();
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateEmail();
        }
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public void delete(long id, long actorId) {
        User existing = userDao.findByIdForUpdate(id).orElseThrow(this::missingUser);
        if (existing.getId().equals(actorId)) {
            throw new ApiException(409, "You cannot delete the currently logged-in administrator");
        }
        if (Role.ADMIN.name().equals(existing.getRole())
                && userDao.countByRoleForUpdate(Role.ADMIN.name()) <= 1) {
            throw new ApiException(409, "Cannot delete the last administrator");
        }
        int deleted = userDao.deleteById(id);
        if (deleted == 0) {
            throw missingUser();
        }
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        return new StatsResponse(
                userDao.countAll(),
                userDao.countByRole(Role.ADMIN.name()),
                userDao.countByRole(Role.USER.name())
        );
    }

    private User requireUser(long id) {
        return userDao.findById(id).orElseThrow(this::missingUser);
    }

    private ApiException missingUser() {
        return new ApiException(404, "User not found");
    }

    private ApiException duplicateEmail() {
        return new ApiException(409, "A user with this email already exists",
                Map.of("email", "A user with this email already exists"));
    }
}
