package com.airline.booking.flight.dto;

public record SeatStatus(String seatLabel, Status status) {

    public enum Status {
        AVAILABLE,
        BOOKED
    }
}
