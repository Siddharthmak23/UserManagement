-- User Management schema for MySQL 8.
-- Creates the database and the users table.
-- The development administrator is NOT inserted here. The application seeds that
-- account on startup and stores only a BCrypt hash. See README.md for the
-- development login and the seeding process.

CREATE DATABASE IF NOT EXISTS user_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE user_management;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
