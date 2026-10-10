package com.usermanagement.config;

import com.usermanagement.security.ApiAccessDeniedHandler;
import com.usermanagement.security.ApiAuthenticationEntryPoint;
import com.usermanagement.security.ApiInvalidSessionStrategy;
import com.usermanagement.security.SecurityResponses;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/favicon.svg", "/error").permitAll()
                        .requestMatchers("/login", "/login.jsp").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forbidden").authenticated()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/", "/home").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation().changeSessionId()
                        .invalidSessionStrategy(new ApiInvalidSessionStrategy()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new ApiAuthenticationEntryPoint())
                        .accessDeniedHandler(new ApiAccessDeniedHandler()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .csrfTokenRequestHandler(csrfHandler))
                .addFilterAfter(csrfTokenLoadingFilter(), CsrfFilter.class);

        return http.build();
    }

    /**
     * Spring Security 6 defers CSRF token creation. Reading the token here stores
     * the session value before the JSP renders it into a meta tag.
     */
    private static OncePerRequestFilter csrfTokenLoadingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (SecurityResponses.isApiRequest(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Object deferred = request.getAttribute(DeferredCsrfToken.class.getName());
                if (deferred instanceof DeferredCsrfToken deferredToken) {
                    deferredToken.get();
                }
                Object current = request.getAttribute(CsrfToken.class.getName());
                if (current instanceof CsrfToken csrfToken) {
                    csrfToken.getToken();
                }
                Object named = request.getAttribute("_csrf");
                if (named instanceof CsrfToken csrfToken) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
