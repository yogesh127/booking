package com.airline.booking.common.exception;

/**
 * Raised when a requested seat is no longer available. This is thrown either by an upfront
 * (best-effort, UX-only) check, or by translating a {@code DataIntegrityViolationException} from
 * the unique-index race on {@code booking_seat} — the latter is what actually guarantees
 * correctness under concurrent requests; the former only produces a nicer error message sooner.
 */
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
