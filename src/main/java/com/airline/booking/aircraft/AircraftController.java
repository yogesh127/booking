package com.airline.booking.aircraft;

import com.airline.booking.aircraft.dto.AircraftResponse;
import com.airline.booking.aircraft.dto.CreateAircraftRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Back-office admin API (ADMIN role required, see SecurityConfig) for onboarding aircraft types. */
@RestController
@RequestMapping("/api/v1/admin/aircraft")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @PostMapping
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody CreateAircraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftService.createAircraft(request));
    }

    @GetMapping("/{id}")
    public AircraftResponse get(@PathVariable Long id) {
        return aircraftService.getAircraft(id);
    }
}
