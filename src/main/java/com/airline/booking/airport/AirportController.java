package com.airline.booking.airport;

import com.airline.booking.airport.dto.AirportResponse;
import com.airline.booking.airport.dto.CreateAirportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Back-office admin API (ADMIN role required, see SecurityConfig) for onboarding airports. */
@RestController
@RequestMapping("/api/v1/admin/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @PostMapping
    public ResponseEntity<AirportResponse> create(@Valid @RequestBody CreateAirportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createAirport(request));
    }

    @GetMapping("/{code}")
    public AirportResponse get(@PathVariable String code) {
        return airportService.getAirport(code);
    }
}
