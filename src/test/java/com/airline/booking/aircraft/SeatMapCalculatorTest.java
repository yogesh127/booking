package com.airline.booking.aircraft;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SeatMapCalculatorTest {

    private Aircraft aircraft(int rows, String letters) {
        Aircraft aircraft = new Aircraft();
        aircraft.setSeatRows(rows);
        aircraft.setSeatLetters(letters);
        return aircraft;
    }

    @Test
    void generatesRowMajorSeatLabels() {
        Aircraft aircraft = aircraft(2, "AB");

        assertThat(SeatMapCalculator.allSeatLabels(aircraft)).containsExactly("1A", "1B", "2A", "2B");
    }

    @Test
    void totalSeatsIsRowsTimesLetters() {
        Aircraft aircraft = aircraft(30, "ABCDEF");

        assertThat(SeatMapCalculator.totalSeats(aircraft)).isEqualTo(180);
    }

    @Test
    void validatesSeatLabelsAgainstConfig() {
        Aircraft aircraft = aircraft(30, "ABCDEF");

        assertThat(SeatMapCalculator.isValidSeatLabel(aircraft, "1A")).isTrue();
        assertThat(SeatMapCalculator.isValidSeatLabel(aircraft, "30F")).isTrue();
        assertThat(SeatMapCalculator.isValidSeatLabel(aircraft, "31A")).isFalse();
        assertThat(SeatMapCalculator.isValidSeatLabel(aircraft, "1G")).isFalse();
    }
}
