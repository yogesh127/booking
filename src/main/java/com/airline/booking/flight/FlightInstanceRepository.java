package com.airline.booking.flight;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightInstanceRepository extends JpaRepository<FlightInstance, Long> {

    List<FlightInstance> findBySourceAirportCodeAndDestinationAirportCodeAndFlightDate(
            String sourceAirportCode, String destinationAirportCode, LocalDate flightDate);

    long countByScheduleId(Long scheduleId);
}
