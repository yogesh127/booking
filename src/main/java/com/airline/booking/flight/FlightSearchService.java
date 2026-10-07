package com.airline.booking.flight;

import com.airline.booking.aircraft.SeatMapCalculator;
import com.airline.booking.booking.BookingSeatRepository;
import com.airline.booking.flight.dto.FlightSearchResult;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlightSearchService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public FlightSearchService(FlightInstanceRepository flightInstanceRepository,
            BookingSeatRepository bookingSeatRepository) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    /**
     * Only flights operating on the requested date are returned — enforced simply by the fact
     * that a {@link FlightInstance} row only exists for dates matching its schedule's
     * days-of-operation (see {@link FlightInstanceGenerationService}).
     */
    @Transactional(readOnly = true)
    public List<FlightSearchResult> search(String sourceAirportCode, String destinationAirportCode, LocalDate date) {
        return flightInstanceRepository
                .findBySourceAirportCodeAndDestinationAirportCodeAndFlightDate(sourceAirportCode, destinationAirportCode, date)
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    private FlightSearchResult toSearchResult(FlightInstance instance) {
        int totalSeats = SeatMapCalculator.totalSeats(instance.getSchedule().getAircraft());
        long bookedSeats = bookingSeatRepository.countByFlightInstanceIdAndReleasedAtIsNull(instance.getId());
        return new FlightSearchResult(
                instance.getId(),
                instance.getSchedule().getFlightNumber(),
                instance.getSourceAirportCode(),
                instance.getDestinationAirportCode(),
                instance.getDepartureAt(),
                instance.getArrivalAt(),
                (int) (totalSeats - bookedSeats));
    }
}
