package com.airline.booking.common;

/**
 * Single source of truth for how far ahead flight instances may be generated/booked.
 * Per the assignment's assumptions, bookings are supported up to one year from the current date.
 * This is enforced only at flight-instance generation time (see FlightInstanceGenerationService);
 * booking against an already-generated instance never needs to re-check it.
 */
public final class BookingWindowPolicy {

    /** Upper bound in days from "today" (inclusive) that flight instances are generated for. */
    public static final int BOOKING_WINDOW_DAYS = 365;

    private BookingWindowPolicy() {
    }
}
