package com.airline.booking.auth.dto;

public record LoginResponse(String token, String tokenType, String role, long expiresInSeconds) {
}
