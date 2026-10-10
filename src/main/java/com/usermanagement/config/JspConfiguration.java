package com.usermanagement.config;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Embedded Tomcat only compiles JSPs from the webapp directory. Point it at
 * {@code src/main/webapp} when the application is launched from the project,
 * including tests and {@code mvn spring-boot:run}. A packaged WAR keeps the
 * container default when that directory is not present.
 */
@Configuration
public class JspConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> jspDocumentRoot() {
        return factory -> {
            File webapp = new File("src/main/webapp");
            if (webapp.isDirectory()) {
                factory.setDocumentRoot(webapp.getAbsoluteFile());
            }
        };
    }
}
