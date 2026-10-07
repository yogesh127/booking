package com.airline.booking.flight;

import com.airline.booking.common.BookingWindowPolicy;
import com.airline.booking.schedule.FlightSchedule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes bookable {@link FlightInstance} rows for a schedule, eagerly, for every date in
 * {@code [today, today + BOOKING_WINDOW_DAYS - 1]} whose day-of-week matches the schedule's
 * days-of-operation.
 *
 * <p><b>Design decision (materialize vs. generate dynamically vs. hybrid):</b> instances are
 * materialized eagerly at schedule-creation time rather than computed on the fly at search time.
 * A concrete row per date is required anyway so that bookings/seats have a stable foreign key to
 * attach to, and the row count this produces (at most 365, typically far fewer since only
 * matching days-of-week are generated — e.g. a 3-day/week schedule yields ~156 rows) is trivial
 * for MySQL. This is simpler and more query-efficient at search time than recomputing candidate
 * dates on every search request.
 *
 * <p><b>Known limitation (documented, not solved here):</b> the window is fixed at the schedule's
 * creation time, not rolling. A schedule created today will have no instances beyond
 * {@code today + 364} even if queried a year later. Rolling the window forward would require a
 * scheduled job to generate the next day's instances for every active schedule — a reasonable
 * next step, out of scope for this assignment.
 */
@Service
public class FlightInstanceGenerationService {

    private static final Logger log = LoggerFactory.getLogger(FlightInstanceGenerationService.class);

    private final FlightInstanceRepository flightInstanceRepository;
    private final Clock clock;

    public FlightInstanceGenerationService(FlightInstanceRepository flightInstanceRepository, Clock clock) {
        this.flightInstanceRepository = flightInstanceRepository;
        this.clock = clock;
    }

    @Transactional
    public int generate(FlightSchedule schedule) {
        LocalDate today = LocalDate.now(clock);
        List<FlightInstance> instances = new ArrayList<>();

        for (int offset = 0; offset < BookingWindowPolicy.BOOKING_WINDOW_DAYS; offset++) {
            LocalDate flightDate = today.plusDays(offset);
            if (!schedule.getDaysOfOperation().contains(flightDate.getDayOfWeek())) {
                continue;
            }
            instances.add(buildInstance(schedule, flightDate));
        }

        flightInstanceRepository.saveAll(instances);
        log.info("Generated {} flight instances for schedule {} ({})", instances.size(),
                schedule.getId(), schedule.getFlightNumber());
        return instances.size();
    }

    private FlightInstance buildInstance(FlightSchedule schedule, LocalDate flightDate) {
        // Arrival rolls over to the next day when its time-of-day is not strictly after
        // departure's time-of-day (<=, not <, to avoid a zero-duration edge case).
        boolean rollsOverToNextDay = !schedule.getArrivalTime().isAfter(schedule.getDepartureTime());
        LocalDate arrivalDate = rollsOverToNextDay ? flightDate.plusDays(1) : flightDate;

        Instant departureAt = flightDate.atTime(schedule.getDepartureTime()).toInstant(ZoneOffset.UTC);
        Instant arrivalAt = arrivalDate.atTime(schedule.getArrivalTime()).toInstant(ZoneOffset.UTC);

        FlightInstance instance = new FlightInstance();
        instance.setSchedule(schedule);
        instance.setFlightDate(flightDate);
        instance.setDepartureAt(departureAt);
        instance.setArrivalAt(arrivalAt);
        instance.setSourceAirportCode(schedule.getSourceAirport().getCode());
        instance.setDestinationAirportCode(schedule.getDestinationAirport().getCode());
        return instance;
    }
}
