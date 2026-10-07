package com.airline.booking.flight;

import com.airline.booking.aircraft.SeatMapCalculator;
import com.airline.booking.booking.BookingSeatRepository;
import com.airline.booking.common.exception.NotFoundException;
import com.airline.booking.flight.dto.SeatStatus;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatMapService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public SeatMapService(FlightInstanceRepository flightInstanceRepository, BookingSeatRepository bookingSeatRepository) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Transactional(readOnly = true)
    public List<SeatStatus> getSeatMap(Long flightInstanceId) {
        FlightInstance instance = flightInstanceRepository.findById(flightInstanceId)
                .orElseThrow(() -> new NotFoundException("Flight instance not found: " + flightInstanceId));

        Set<String> bookedLabels = bookingSeatRepository.findByFlightInstanceIdAndReleasedAtIsNull(flightInstanceId)
                .stream()
                .map(seat -> seat.getSeatLabel())
                .collect(Collectors.toSet());

        return SeatMapCalculator.allSeatLabels(instance.getSchedule().getAircraft()).stream()
                .map(label -> new SeatStatus(label, bookedLabels.contains(label) ? SeatStatus.Status.BOOKED : SeatStatus.Status.AVAILABLE))
                .toList();
    }
}
