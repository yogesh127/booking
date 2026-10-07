package com.airline.booking.airport.dto;

import com.airline.booking.airport.Airport;

public record AirportResponse(String code, String name, String city, String country) {

    public static AirportResponse from(Airport airport) {
        return new AirportResponse(airport.getCode(), airport.getName(), airport.getCity(), airport.getCountry());
    }
}
