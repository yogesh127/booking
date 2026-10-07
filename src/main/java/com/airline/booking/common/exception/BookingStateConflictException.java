package com.airline.booking.common.exception;

/** Raised when a booking's status is being changed concurrently (e.g. a double-cancel race). */
public class BookingStateConflictException extends RuntimeException {

    public BookingStateConflictException(String message) {
        super(message);
    }
}
