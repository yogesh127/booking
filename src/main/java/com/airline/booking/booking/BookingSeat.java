package com.airline.booking.booking;

import com.airline.booking.flight.FlightInstance;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One passenger's seat hold against one flight instance. {@code releasedAt} is null while the
 * hold is active; cancellation sets it rather than deleting the row, so a cancelled booking
 * remains inspectable (who had which seat). The unique index {@code uq_booking_seat_active} on
 * {@code (flight_instance_id, active_seat_label)} — where {@code active_seat_label} is a
 * generated column equal to {@code seat_label} when {@code released_at} is null, else null — is
 * what actually prevents two bookings from holding the same seat concurrently — see
 * {@link BookingService}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_instance_id")
    private FlightInstance flightInstance;

    private String seatLabel;

    private String passengerName;

    private Instant releasedAt;
}
