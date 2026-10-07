package com.airline.booking.aircraft.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAircraftRequest(
        @NotBlank @Size(max = 50) String type,
        // Seat labels are "<row><letter>" (SeatMapCalculator) and must fit booking_seat.seat_label
        // VARCHAR(5) - capping at 4 digits keeps every possible label (e.g. "9999F") within that.
        @NotNull @Positive @Max(9999) Integer seatRows,
        @NotBlank @Size(max = 10)
        @Pattern(regexp = "^[A-Z]+$", message = "must contain only uppercase letters A-Z") String seatLetters) {
}
