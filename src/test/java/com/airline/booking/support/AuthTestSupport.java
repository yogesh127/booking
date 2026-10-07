package com.airline.booking.support;

import com.airline.booking.auth.dto.LoginRequest;
import com.airline.booking.auth.dto.LoginResponse;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpHeaders;

/** Shared login/bearer-header helpers for ITs, now that auth is JWT-based rather than Basic. */
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static String login(TestRestTemplate restTemplate, String username, String password) {
        LoginResponse response = restTemplate.postForObject(
                "/api/v1/auth/login", new LoginRequest(username, password), LoginResponse.class);
        return response.token();
    }

    public static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
