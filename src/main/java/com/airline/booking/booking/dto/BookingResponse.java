package com.airline.booking.booking.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID reference,
        String flightNumber,
        LocalDate flightDate,
        int passengerCount,
        List<SeatAssignment> seats,
        String status) {

    public record SeatAssignment(String seatLabel, String passengerName) {
    }
}
