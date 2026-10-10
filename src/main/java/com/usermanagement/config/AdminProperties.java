package com.usermanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Development administrator settings. Values come from application properties
 * or environment variables and are never written into the SQL seed script.
 */
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private String email = "admin@usermanagement.local";
    private String password = "Admin@12345";
    private String name = "System Administrator";
    private String phone = "5550100199";

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
