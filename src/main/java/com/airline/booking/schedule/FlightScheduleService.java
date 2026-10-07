package com.airline.booking.schedule;

import com.airline.booking.aircraft.Aircraft;
import com.airline.booking.aircraft.AircraftRepository;
import com.airline.booking.airport.Airport;
import com.airline.booking.airport.AirportRepository;
import com.airline.booking.common.exception.InvalidRequestException;
import com.airline.booking.common.exception.NotFoundException;
import com.airline.booking.flight.FlightInstanceGenerationService;
import com.airline.booking.flight.FlightInstanceRepository;
import com.airline.booking.schedule.dto.CreateScheduleRequest;
import com.airline.booking.schedule.dto.ScheduleResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlightScheduleService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightInstanceGenerationService flightInstanceGenerationService;
    private final FlightInstanceRepository flightInstanceRepository;
    private final Clock clock;

    public FlightScheduleService(
            FlightScheduleRepository flightScheduleRepository,
            AirportRepository airportRepository,
            AircraftRepository aircraftRepository,
            FlightInstanceGenerationService flightInstanceGenerationService,
            FlightInstanceRepository flightInstanceRepository,
            Clock clock) {
        this.flightScheduleRepository = flightScheduleRepository;
        this.airportRepository = airportRepository;
        this.aircraftRepository = aircraftRepository;
        this.flightInstanceGenerationService = flightInstanceGenerationService;
        this.flightInstanceRepository = flightInstanceRepository;
        this.clock = clock;
    }

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        Airport source = airportRepository.findById(request.sourceAirportCode())
                .orElseThrow(() -> new NotFoundException("Unknown source airport: " + request.sourceAirportCode()));
        Airport destination = airportRepository.findById(request.destinationAirportCode())
                .orElseThrow(() -> new NotFoundException("Unknown destination airport: " + request.destinationAirportCode()));
        if (source.getCode().equals(destination.getCode())) {
            throw new InvalidRequestException("Source and destination airports must differ");
        }
        Aircraft aircraft = aircraftRepository.findById(request.aircraftId())
                .orElseThrow(() -> new NotFoundException("Unknown aircraft id: " + request.aircraftId()));

        FlightSchedule schedule = new FlightSchedule();
        schedule.setFlightNumber(request.flightNumber());
        schedule.setSourceAirport(source);
        schedule.setDestinationAirport(destination);
        schedule.setDepartureTime(request.departureTime());
        schedule.setArrivalTime(request.arrivalTime());
        schedule.setAircraft(aircraft);
        schedule.setDaysOfOperation(request.daysOfOperation());
        schedule.setCreatedAt(Instant.now(clock));

        schedule = flightScheduleRepository.save(schedule);
        int generatedCount = flightInstanceGenerationService.generate(schedule);

        return ScheduleResponse.from(schedule, generatedCount);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long id) {
        FlightSchedule schedule = flightScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));
        int instanceCount = (int) flightInstanceRepository.countByScheduleId(id);
        return ScheduleResponse.from(schedule, instanceCount);
    }
}
