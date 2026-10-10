package com.usermanagement;

import com.usermanagement.config.AdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Spring Boot entry point. Extending {@link SpringBootServletInitializer} keeps the
 * same WAR deployable on an external Jakarta Tomcat 10.1 container.
 */
@SpringBootApplication
@EnableConfigurationProperties(AdminProperties.class)
public class UserManagementApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(UserManagementApplication.class);
    }
}
