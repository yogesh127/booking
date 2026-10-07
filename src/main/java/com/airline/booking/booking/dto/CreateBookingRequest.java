package com.airline.booking.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long flightInstanceId,
        @NotEmpty @Valid List<PassengerSeatRequest> passengers) {
}
