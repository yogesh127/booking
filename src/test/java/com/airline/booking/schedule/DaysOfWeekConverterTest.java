package com.airline.booking.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class DaysOfWeekConverterTest {

    private final DaysOfWeekConverter converter = new DaysOfWeekConverter();

    @Test
    void encodesMondayWednesdayFridayAs21() {
        var days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        // Mon = bit0 (1), Wed = bit2 (4), Fri = bit4 (16) -> 1 + 4 + 16 = 21
        assertThat(converter.convertToDatabaseColumn(days)).isEqualTo(21);
    }

    @Test
    void roundTripsThroughTheBitmask() {
        var days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

        Integer encoded = converter.convertToDatabaseColumn(days);

        assertThat(converter.convertToEntityAttribute(encoded)).isEqualTo(days);
    }

    @Test
    void handlesEveryDayOfTheWeek() {
        var allDays = EnumSet.allOf(DayOfWeek.class);

        Integer encoded = converter.convertToDatabaseColumn(allDays);

        assertThat(encoded).isEqualTo(127);
        assertThat(converter.convertToEntityAttribute(encoded)).isEqualTo(allDays);
    }
}
