package com.airline.booking.booking;

import com.airline.booking.booking.dto.BookingResponse;
import com.airline.booking.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping("/{reference}")
    public BookingResponse get(@PathVariable UUID reference) {
        return bookingService.getBooking(reference);
    }

    @PostMapping("/{reference}/cancel")
    public BookingResponse cancel(@PathVariable UUID reference) {
        return bookingService.cancelBooking(reference);
    }
}
