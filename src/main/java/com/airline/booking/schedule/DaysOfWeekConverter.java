package com.airline.booking.schedule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stores a {@code Set<DayOfWeek>} as a bitmask SMALLINT (bit position = DayOfWeek.getValue() - 1,
 * so Monday=bit0 .. Sunday=bit6). E.g. {MONDAY, WEDNESDAY, FRIDAY} -> 1 + 4 + 16 = 21.
 * A bitmask is sufficient here because days-of-operation are only ever evaluated in-application,
 * once per schedule at flight-instance generation time — never queried at the DB level.
 */
@Converter
public class DaysOfWeekConverter implements AttributeConverter<Set<DayOfWeek>, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Set<DayOfWeek> attribute) {
        int bitmask = 0;
        for (DayOfWeek day : attribute) {
            bitmask |= 1 << (day.getValue() - 1);
        }
        return bitmask;
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(Integer dbData) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            if ((dbData & (1 << (day.getValue() - 1))) != 0) {
                days.add(day);
            }
        }
        return days;
    }
}
