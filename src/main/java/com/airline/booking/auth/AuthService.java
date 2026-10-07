package com.airline.booking.auth;

import com.airline.booking.auth.dto.LoginRequest;
import com.airline.booking.auth.dto.LoginResponse;
import com.airline.booking.common.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Validates credentials against the users configured in SecurityConfig and issues a JWT carrying
 * the caller's role. Bad credentials raise {@link org.springframework.security.core.AuthenticationException}
 * (via AuthenticationManager), mapped to 401 by GlobalExceptionHandler.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authenticated = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String role = authenticated.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElseThrow();

        String token = jwtService.generateToken(authenticated.getName(), role);
        return new LoginResponse(token, "Bearer", role, jwtService.expirationSeconds());
    }
}
