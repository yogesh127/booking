package com.airline.booking.flight.dto;

import java.time.Instant;

public record FlightSearchResult(
        Long flightInstanceId,
        String flightNumber,
        String sourceAirportCode,
        String destinationAirportCode,
        Instant departureAt,
        Instant arrivalAt,
        int availableSeatsCount) {
}
