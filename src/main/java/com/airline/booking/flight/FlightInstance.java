package com.airline.booking.flight;

import com.airline.booking.schedule.FlightSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A concrete, bookable occurrence of a {@link FlightSchedule} on one date. Source/destination
 * airport codes are denormalized from the schedule onto this row purely so flight search can
 * filter/index on them without a join back to flight_schedule.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class FlightInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private FlightSchedule schedule;

    @Column(name = "flight_date")
    private LocalDate flightDate;

    @Column(name = "departure_at")
    private Instant departureAt;

    @Column(name = "arrival_at")
    private Instant arrivalAt;

    @Column(name = "source_airport_code")
    private String sourceAirportCode;

    @Column(name = "destination_airport_code")
    private String destinationAirportCode;
}
