package com.airline.booking.schedule;

import com.airline.booking.schedule.dto.CreateScheduleRequest;
import com.airline.booking.schedule.dto.ScheduleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Back-office administrative API. The "/admin" prefix is a naming convention only — no
 * authentication/authorization is added, consistent with "anything not explicitly mentioned is
 * out of scope" in the assignment.
 */
@RestController
@RequestMapping("/api/v1/admin/schedules")
public class FlightScheduleController {

    private final FlightScheduleService flightScheduleService;

    public FlightScheduleController(FlightScheduleService flightScheduleService) {
        this.flightScheduleService = flightScheduleService;
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightScheduleService.createSchedule(request));
    }

    @GetMapping("/{id}")
    public ScheduleResponse get(@PathVariable Long id) {
        return flightScheduleService.getSchedule(id);
    }
}
