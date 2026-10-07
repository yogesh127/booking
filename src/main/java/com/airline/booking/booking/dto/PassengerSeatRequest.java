package com.airline.booking.booking.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Passenger info is intentionally minimal (name only) per the assignment's assumptions. "No
 * passenger validation required" is interpreted as no semantic/format checks on the name (no
 * length/charset/identity rules) — but the field must still be present, since a blank name is a
 * malformed request, not a judgment about what names are valid.
 */
public record PassengerSeatRequest(@NotBlank String name, @NotBlank String seatLabel) {
}
