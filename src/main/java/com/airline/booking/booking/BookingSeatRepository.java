package com.airline.booking.booking;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByFlightInstanceIdAndReleasedAtIsNull(Long flightInstanceId);

    long countByFlightInstanceIdAndReleasedAtIsNull(Long flightInstanceId);

    List<BookingSeat> findBySeatLabelInAndFlightInstanceIdAndReleasedAtIsNull(Set<String> seatLabels, Long flightInstanceId);

    List<BookingSeat> findByBookingIdAndReleasedAtIsNull(Long bookingId);

    List<BookingSeat> findByBookingId(Long bookingId);
}
