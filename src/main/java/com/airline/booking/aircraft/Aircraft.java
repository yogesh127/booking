package com.airline.booking.aircraft;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    /** Number of rows in the seat grid, e.g. 30. */
    private Integer seatRows;

    /** Seat letters per row, e.g. "ABCDEF" for a 6-seats-per-row layout. */
    private String seatLetters;
}
