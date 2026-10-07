package com.airline.booking.common.exception;

/** Raised for request-level validation failures not already covered by bean validation. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
