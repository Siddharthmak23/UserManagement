package com.usermanagement.dao;

import com.usermanagement.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access for the users table. SQL text is constant. Every caller-supplied
 * value is bound with a JDBC {@link PreparedStatement} through {@link JdbcTemplate}.
 */
@Repository
public class UserDao {

    private static final String COLUMNS =
            "id, name, email, phone, password_hash, role, created_at, updated_at";

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) -> {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                rs.getString("role"),
                created == null ? null : created.toLocalDateTime(),
                updated == null ? null : updated.toLocalDateTime()
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Builds a LIKE pattern that treats {@code %}, {@code _}, and {@code \} as literals.
     */
    static String likePattern(String search) {
        String escaped = search
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public Optional<User> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", id);
    }

    public Optional<User> findByIdForUpdate(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ? FOR UPDATE", id);
    }

    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE email = ?", email);
    }

    public boolean emailExists(String email, Long excludeId) {
        Integer count;
        if (excludeId == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?",
                    Integer.class, email, excludeId);
        }
        return count != null && count > 0;
    }

    public long count(String search) {
        Long count;
        if (search == null) {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        } else {
            String pattern = likePattern(search);
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE name LIKE ? ESCAPE '\\\\' OR email LIKE ? ESCAPE '\\\\'",
                    Long.class, pattern, pattern);
        }
        return count == null ? 0L : count;
    }

    public List<User> findPage(String search, int limit, int offset) {
        if (search == null) {
            return jdbcTemplate.query(
                    "SELECT " + COLUMNS + " FROM users ORDER BY id DESC LIMIT ? OFFSET ?",
                    USER_MAPPER, limit, offset);
        }
        String pattern = likePattern(search);
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM users WHERE name LIKE ? ESCAPE '\\\\' OR email LIKE ? ESCAPE '\\\\' "
                        + "ORDER BY id DESC LIMIT ? OFFSET ?",
                USER_MAPPER, pattern, pattern, limit, offset);
    }

    public long insert(User user) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPhone());
            statement.setString(4, user.getPasswordHash());
            statement.setString(5, user.getRole());
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return a new user id");
        }
        return key.longValue();
    }

    public int update(long id, String name, String email, String phone, String role, String passwordHashOrNull) {
        if (passwordHashOrNull == null) {
            return jdbcTemplate.update(
                    "UPDATE users SET name = ?, email = ?, phone = ?, role = ? WHERE id = ?",
                    name, email, phone, role, id);
        }
        return jdbcTemplate.update(
                "UPDATE users SET name = ?, email = ?, phone = ?, role = ?, password_hash = ? WHERE id = ?",
                name, email, phone, role, passwordHashOrNull, id);
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0L : count;
    }

    public long countByRole(String role) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ?", Long.class, role);
        return count == null ? 0L : count;
    }

    public long countByRoleForUpdate(String role) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ? FOR UPDATE", Long.class, role);
        return count == null ? 0L : count;
    }

    private Optional<User> queryOne(String sql, Object parameter) {
        try {
            User user = jdbcTemplate.queryForObject(sql, USER_MAPPER, parameter);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
