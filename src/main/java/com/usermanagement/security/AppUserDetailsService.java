package com.usermanagement.security;

import com.usermanagement.dao.UserDao;
import com.usermanagement.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

    public AppUserDetailsService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new AppUserPrincipal(user);
    }
}
