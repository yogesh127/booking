package com.airline.booking.aircraft;

import com.airline.booking.aircraft.dto.AircraftResponse;
import com.airline.booking.aircraft.dto.CreateAircraftRequest;
import com.airline.booking.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Onboarding for the aircraft reference data that was previously Flyway-seed-only (see V8). */
@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;

    public AircraftService(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    @Transactional
    public AircraftResponse createAircraft(CreateAircraftRequest request) {
        Aircraft aircraft = new Aircraft();
        aircraft.setType(request.type());
        aircraft.setSeatRows(request.seatRows());
        aircraft.setSeatLetters(request.seatLetters());
        return AircraftResponse.from(aircraftRepository.save(aircraft));
    }

    @Transactional(readOnly = true)
    public AircraftResponse getAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unknown aircraft id: " + id));
        return AircraftResponse.from(aircraft);
    }
}
