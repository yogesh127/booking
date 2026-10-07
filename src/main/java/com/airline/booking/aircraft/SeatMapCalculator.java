package com.airline.booking.aircraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Derives an aircraft's seat labels from its fixed {@code seatRows}/{@code seatLetters}
 * configuration. Seat state is never stored per-seat; it is always computed from this label set
 * minus whatever is currently booked.
 */
public final class SeatMapCalculator {

    private SeatMapCalculator() {
    }

    /** All seat labels for an aircraft, in row-major order, e.g. "1A", "1B", ..., "30F". */
    public static List<String> allSeatLabels(Aircraft aircraft) {
        List<String> labels = new ArrayList<>(aircraft.getSeatRows() * aircraft.getSeatLetters().length());
        for (int row = 1; row <= aircraft.getSeatRows(); row++) {
            for (int i = 0; i < aircraft.getSeatLetters().length(); i++) {
                labels.add(row + String.valueOf(aircraft.getSeatLetters().charAt(i)));
            }
        }
        return labels;
    }

    public static int totalSeats(Aircraft aircraft) {
        return aircraft.getSeatRows() * aircraft.getSeatLetters().length();
    }

    public static boolean isValidSeatLabel(Aircraft aircraft, String seatLabel) {
        return allSeatLabelSet(aircraft).contains(seatLabel);
    }

    private static Set<String> allSeatLabelSet(Aircraft aircraft) {
        return Set.copyOf(allSeatLabels(aircraft));
    }
}
