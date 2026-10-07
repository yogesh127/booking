package com.airline.booking.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CreateScheduleRequest(
        @NotBlank @Size(max = 10) String flightNumber,
        @NotBlank @Size(min = 3, max = 3) String sourceAirportCode,
        @NotBlank @Size(min = 3, max = 3) String destinationAirportCode,
        @NotNull LocalTime departureTime,
        @NotNull LocalTime arrivalTime,
        @NotNull Long aircraftId,
        @NotEmpty Set<DayOfWeek> daysOfOperation) {
}
