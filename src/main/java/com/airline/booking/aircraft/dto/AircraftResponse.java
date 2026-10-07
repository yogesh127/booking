package com.airline.booking.aircraft.dto;

import com.airline.booking.aircraft.Aircraft;

public record AircraftResponse(Long id, String type, Integer seatRows, String seatLetters) {

    public static AircraftResponse from(Aircraft aircraft) {
        return new AircraftResponse(aircraft.getId(), aircraft.getType(), aircraft.getSeatRows(), aircraft.getSeatLetters());
    }
}
