package com.example.SpringBackend.service;

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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;


@Service
public class AuthService {

    @Value("${ADMIN_USERNAME}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    private final SecurityContextRepository securityContextRepository;

    public AuthService(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public boolean loginWithBasicAuth(String authHeader, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            String base64Credentials = authHeader.substring(6);
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decoded, StandardCharsets.UTF_8);

            String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                return false;
            }

            String inputUsername = values[0];
            String inputPassword = values[1];

            if (adminUsername == null || adminPassword == null ||
                    !adminUsername.trim().equals(inputUsername.trim()) ||
                    !adminPassword.trim().equals(inputPassword.trim())) {
                return false;
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    inputUsername,
                    null,
                    List.of(new SimpleGrantedAuthority("ADMIN"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return true;

        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}