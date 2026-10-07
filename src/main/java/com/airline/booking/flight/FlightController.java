package com.airline.booking.flight;

import com.airline.booking.flight.dto.FlightSearchResult;
import com.airline.booking.flight.dto.SeatStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private final FlightSearchService flightSearchService;
    private final SeatMapService seatMapService;

    public FlightController(FlightSearchService flightSearchService, SeatMapService seatMapService) {
        this.flightSearchService = flightSearchService;
        this.seatMapService = seatMapService;
    }

    @GetMapping("/search")
    public List<FlightSearchResult> search(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return flightSearchService.search(source.toUpperCase(), destination.toUpperCase(), date);
    }

    @GetMapping("/{flightInstanceId}/seat-map")
    public List<SeatStatus> seatMap(@PathVariable Long flightInstanceId) {
        return seatMapService.getSeatMap(flightInstanceId);
    }
}
