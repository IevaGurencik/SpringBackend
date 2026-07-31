package com.example.SpringBackend.service;

import com.example.SpringBackend.model.LoginEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Value("${ADMIN_USERNAME}")
    private String username;

    @Value("${ADMIN_PASSWORD}")
    private String password;

    private final SecurityContextRepository securityContextRepository;

    public AuthService(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public boolean login(
            LoginEntity request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (!username.equals(request.getUsername())
                || !password.equals(request.getPassword())) {
            return false;
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return true;
    }
}
