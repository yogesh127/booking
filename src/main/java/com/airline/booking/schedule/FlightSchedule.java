package com.airline.booking.schedule;

import com.airline.booking.aircraft.Aircraft;
import com.airline.booking.airport.Airport;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A reusable flight template. Immutable once created (per assumptions) — no update endpoint. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class FlightSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_airport_code")
    private Airport sourceAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_code")
    private Airport destinationAirport;

    private LocalTime departureTime;

    private LocalTime arrivalTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @Convert(converter = DaysOfWeekConverter.class)
    @Column(name = "days_of_operation")
    private Set<DayOfWeek> daysOfOperation;

    private Instant createdAt;
}
