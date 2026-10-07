package com.airline.booking.airport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAirportRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter IATA code") String code,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country) {
}
