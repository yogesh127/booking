package com.airline.booking.schedule.dto;

import com.airline.booking.schedule.FlightSchedule;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record ScheduleResponse(
        Long id,
        String flightNumber,
        String sourceAirportCode,
        String destinationAirportCode,
        LocalTime departureTime,
        LocalTime arrivalTime,
        String aircraftType,
        Set<DayOfWeek> daysOfOperation,
        int generatedInstanceCount) {

    public static ScheduleResponse from(FlightSchedule schedule, int generatedInstanceCount) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getFlightNumber(),
                schedule.getSourceAirport().getCode(),
                schedule.getDestinationAirport().getCode(),
                schedule.getDepartureTime(),
                schedule.getArrivalTime(),
                schedule.getAircraft().getType(),
                schedule.getDaysOfOperation(),
                generatedInstanceCount);
    }
}
