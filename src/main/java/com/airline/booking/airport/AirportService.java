package com.airline.booking.airport;

import com.airline.booking.airport.dto.AirportResponse;
import com.airline.booking.airport.dto.CreateAirportRequest;
import com.airline.booking.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Onboarding for the airport reference data that was previously Flyway-seed-only (see V7). */
@Service
public class AirportService {

    private final AirportRepository airportRepository;

    public AirportService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    @Transactional
    public AirportResponse createAirport(CreateAirportRequest request) {
        Airport airport = new Airport();
        airport.setCode(request.code().toUpperCase());
        airport.setName(request.name());
        airport.setCity(request.city());
        airport.setCountry(request.country());
        return AirportResponse.from(airportRepository.save(airport));
    }

    @Transactional(readOnly = true)
    public AirportResponse getAirport(String code) {
        Airport airport = airportRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Unknown airport: " + code));
        return AirportResponse.from(airport);
    }
}
