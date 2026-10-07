package com.airline.booking.booking;

import com.airline.booking.aircraft.SeatMapCalculator;
import com.airline.booking.booking.dto.BookingResponse;
import com.airline.booking.booking.dto.CreateBookingRequest;
import com.airline.booking.booking.dto.PassengerSeatRequest;
import com.airline.booking.common.exception.BookingStateConflictException;
import com.airline.booking.common.exception.InvalidRequestException;
import com.airline.booking.common.exception.NotFoundException;
import com.airline.booking.common.exception.SeatUnavailableException;
import com.airline.booking.flight.FlightInstance;
import com.airline.booking.flight.FlightInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the seat-booking/cancellation concurrency strategy.
 *
 * <p><b>Why this is race-free for multi-seat bookings:</b> all of a booking's
 * {@code booking_seat} rows are inserted and explicitly flushed inside one transaction. The
 * unique index {@code uq_booking_seat_active(flight_instance_id, active_seat_label)} — where
 * {@code active_seat_label} is a generated column equal to {@code seat_label} only while
 * {@code released_at} is null — is checked by MySQL at each row insert, independent of isolation
 * level — if a concurrent transaction already holds one of the requested seats, the second inserter blocks
 * on that key until the first transaction resolves, then fails with a constraint violation. The
 * explicit {@code flush()} is essential: without it, Hibernate would defer the INSERTs to
 * commit-time, and the constraint violation would surface only after this method has already
 * returned — too late for the {@code catch} below to ever run. Because every seat in one request
 * shares a single transaction, a conflict on any one seat aborts the entire booking (all seats or
 * none) — there is no partial-success case. This also means no explicit row locking
 * (e.g. {@code SELECT ... FOR UPDATE} on the flight instance) is needed, and — unlike locking the
 * whole flight row — two requests for two different seats on the same flight never contend with
 * each other at all.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            FlightInstanceRepository flightInstanceRepository,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.flightInstanceRepository = flightInstanceRepository;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        FlightInstance instance = flightInstanceRepository.findById(request.flightInstanceId())
                .orElseThrow(() -> new NotFoundException("Flight instance not found: " + request.flightInstanceId()));

        validateSeatRequests(request.passengers(), instance);

        Booking booking = new Booking();
        booking.setReference(UUID.randomUUID());
        booking.setFlightInstance(instance);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPassengerCount(request.passengers().size());
        booking.setCreatedAt(Instant.now(clock));
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> seats = request.passengers().stream()
                .map(p -> toBookingSeat(savedBooking, instance, p))
                .toList();

        try {
            bookingSeatRepository.saveAllAndFlush(seats);
        } catch (DataIntegrityViolationException e) {
            log.info("Seat conflict booking flight instance {} seats {}", instance.getId(),
                    request.passengers().stream().map(PassengerSeatRequest::seatLabel).toList());
            throw new SeatUnavailableException("One or more requested seats are no longer available");
        }

        log.info("Booking {} created for flight instance {} with {} seat(s)", savedBooking.getReference(),
                instance.getId(), seats.size());
        return toResponse(savedBooking, instance, seats);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID reference) {
        Booking booking = findByReference(reference);
        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getId());
        return toResponse(booking, booking.getFlightInstance(), seats);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID reference) {
        Booking booking = findByReference(reference);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            // Cancelling an already-cancelled booking is idempotent, not an error.
            List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getId());
            return toResponse(booking, booking.getFlightInstance(), seats);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now(clock));
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BookingStateConflictException("Booking was modified concurrently, please retry");
        }

        List<BookingSeat> activeSeats = bookingSeatRepository.findByBookingIdAndReleasedAtIsNull(booking.getId());
        Instant releasedAt = Instant.now(clock);
        activeSeats.forEach(seat -> seat.setReleasedAt(releasedAt));
        bookingSeatRepository.saveAll(activeSeats);

        log.info("Booking {} cancelled, {} seat(s) released", booking.getReference(), activeSeats.size());

        List<BookingSeat> allSeats = bookingSeatRepository.findByBookingId(booking.getId());
        return toResponse(booking, booking.getFlightInstance(), allSeats);
    }

    private Booking findByReference(UUID reference) {
        return bookingRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + reference));
    }

    private void validateSeatRequests(List<PassengerSeatRequest> passengers, FlightInstance instance) {
        Set<String> requestedLabels = new HashSet<>();
        for (PassengerSeatRequest p : passengers) {
            if (!requestedLabels.add(p.seatLabel())) {
                throw new InvalidRequestException("Duplicate seat requested: " + p.seatLabel());
            }
        }
        for (String label : requestedLabels) {
            if (!SeatMapCalculator.isValidSeatLabel(instance.getSchedule().getAircraft(), label)) {
                throw new InvalidRequestException("Seat does not exist on this aircraft: " + label);
            }
        }

        // Best-effort, UX-only pre-check: produces a clearer error sooner than waiting for the
        // DB constraint. It is NOT what prevents the race — see class-level javadoc.
        List<BookingSeat> alreadyHeld = bookingSeatRepository
                .findBySeatLabelInAndFlightInstanceIdAndReleasedAtIsNull(requestedLabels, instance.getId());
        if (!alreadyHeld.isEmpty()) {
            String held = alreadyHeld.stream().map(BookingSeat::getSeatLabel).collect(Collectors.joining(", "));
            throw new SeatUnavailableException("Seat(s) already booked: " + held);
        }
    }

    private BookingSeat toBookingSeat(Booking booking, FlightInstance instance, PassengerSeatRequest passenger) {
        BookingSeat seat = new BookingSeat();
        seat.setBooking(booking);
        seat.setFlightInstance(instance);
        seat.setSeatLabel(passenger.seatLabel());
        seat.setPassengerName(passenger.name());
        return seat;
    }

    private BookingResponse toResponse(Booking booking, FlightInstance instance, List<BookingSeat> seats) {
        List<BookingResponse.SeatAssignment> assignments = seats.stream()
                .map(s -> new BookingResponse.SeatAssignment(s.getSeatLabel(), s.getPassengerName()))
                .toList();
        return new BookingResponse(
                booking.getReference(),
                instance.getSchedule().getFlightNumber(),
                instance.getFlightDate(),
                booking.getPassengerCount(),
                assignments,
                booking.getStatus().name());
    }
}
