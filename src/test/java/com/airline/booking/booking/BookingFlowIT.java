package com.airline.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.airline.booking.TestcontainersConfiguration;
import com.airline.booking.aircraft.Aircraft;
import com.airline.booking.aircraft.AircraftRepository;
import com.airline.booking.booking.dto.BookingResponse;
import com.airline.booking.booking.dto.CreateBookingRequest;
import com.airline.booking.booking.dto.PassengerSeatRequest;
import com.airline.booking.flight.dto.FlightSearchResult;
import com.airline.booking.flight.dto.SeatStatus;
import com.airline.booking.schedule.dto.CreateScheduleRequest;
import com.airline.booking.schedule.dto.ScheduleResponse;
import com.airline.booking.support.AuthTestSupport;
import com.airline.booking.support.FixedClockTestConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import({TestcontainersConfiguration.class, FixedClockTestConfig.class})
class BookingFlowIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AircraftRepository aircraftRepository;

    private String adminToken;
    private String clientToken;

    private Long flightInstanceId;

    @BeforeEach
    void createScheduleAndResolveFlightInstance() {
        adminToken = AuthTestSupport.login(restTemplate, "admin", "admin123");
        clientToken = AuthTestSupport.login(restTemplate, "client", "client123");

        Long a320Id = aircraftRepository.findAll().stream()
                .filter(a -> a.getType().equals("A320"))
                .map(Aircraft::getId)
                .findFirst()
                .orElseThrow();

        CreateScheduleRequest scheduleRequest = new CreateScheduleRequest(
                "BF100", "DXB", "LHR",
                LocalTime.of(9, 30), LocalTime.of(13, 45),
                a320Id,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        restTemplate.exchange("/api/v1/admin/schedules", HttpMethod.POST,
                new HttpEntity<>(scheduleRequest, AuthTestSupport.bearer(adminToken)), ScheduleResponse.class);

        String uri = UriComponentsBuilder.fromPath("/api/v1/flights/search")
                .queryParam("source", "DXB")
                .queryParam("destination", "LHR")
                .queryParam("date", LocalDate.of(2024, 1, 1))
                .toUriString();
        FlightSearchResult[] results = restTemplate.getForObject(uri, FlightSearchResult[].class);
        flightInstanceId = Arrays.stream(results)
                .filter(r -> r.flightNumber().equals("BF100"))
                .findFirst()
                .orElseThrow()
                .flightInstanceId();
    }

    @Test
    void bookingThenCancellingReleasesTheSeatForRebooking() {
        assertThat(seatStatus("1A")).isEqualTo(SeatStatus.Status.AVAILABLE);
        assertThat(seatStatus("1B")).isEqualTo(SeatStatus.Status.AVAILABLE);

        CreateBookingRequest bookingRequest = new CreateBookingRequest(flightInstanceId, List.of(
                new PassengerSeatRequest("Alice", "1A"),
                new PassengerSeatRequest("Bob", "1B")));
        ResponseEntity<BookingResponse> created = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(bookingRequest, AuthTestSupport.bearer(clientToken)), BookingResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookingResponse booking = created.getBody();
        assertThat(booking.status()).isEqualTo("CONFIRMED");
        assertThat(booking.passengerCount()).isEqualTo(2);
        assertThat(booking.flightNumber()).isEqualTo("BF100");
        assertThat(booking.seats()).hasSize(2);

        assertThat(seatStatus("1A")).isEqualTo(SeatStatus.Status.BOOKED);
        assertThat(seatStatus("1B")).isEqualTo(SeatStatus.Status.BOOKED);
        assertThat(seatStatus("1C")).isEqualTo(SeatStatus.Status.AVAILABLE);

        BookingResponse fetched = restTemplate.exchange(
                "/api/v1/bookings/" + booking.reference(), HttpMethod.GET,
                new HttpEntity<>(AuthTestSupport.bearer(clientToken)), BookingResponse.class).getBody();
        assertThat(fetched.status()).isEqualTo("CONFIRMED");

        BookingResponse cancelled = restTemplate.exchange(
                "/api/v1/bookings/" + booking.reference() + "/cancel", HttpMethod.POST,
                new HttpEntity<>(AuthTestSupport.bearer(clientToken)), BookingResponse.class).getBody();
        assertThat(cancelled.status()).isEqualTo("CANCELLED");

        assertThat(seatStatus("1A")).isEqualTo(SeatStatus.Status.AVAILABLE);
        assertThat(seatStatus("1B")).isEqualTo(SeatStatus.Status.AVAILABLE);

        // The released seat can be booked again by someone else.
        CreateBookingRequest rebooking = new CreateBookingRequest(flightInstanceId,
                List.of(new PassengerSeatRequest("Carol", "1A")));
        ResponseEntity<BookingResponse> rebooked = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(rebooking, AuthTestSupport.bearer(clientToken)), BookingResponse.class);
        assertThat(rebooked.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // The cancelled booking still shows its original seats/passengers for audit purposes.
        BookingResponse stillInspectable = restTemplate.exchange(
                "/api/v1/bookings/" + booking.reference(), HttpMethod.GET,
                new HttpEntity<>(AuthTestSupport.bearer(clientToken)), BookingResponse.class).getBody();
        assertThat(stillInspectable.seats()).extracting(BookingResponse.SeatAssignment::seatLabel)
                .containsExactlyInAnyOrder("1A", "1B");
    }

    @Test
    void bookingADuplicateOrUnknownSeatIsRejected() {
        CreateBookingRequest duplicateSeat = new CreateBookingRequest(flightInstanceId, List.of(
                new PassengerSeatRequest("Alice", "2A"),
                new PassengerSeatRequest("Bob", "2A")));
        ResponseEntity<String> duplicateResponse = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(duplicateSeat, AuthTestSupport.bearer(clientToken)), String.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CreateBookingRequest unknownSeat = new CreateBookingRequest(flightInstanceId,
                List.of(new PassengerSeatRequest("Alice", "99Z")));
        ResponseEntity<String> unknownResponse = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(unknownSeat, AuthTestSupport.bearer(clientToken)), String.class);
        assertThat(unknownResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private SeatStatus.Status seatStatus(String seatLabel) {
        SeatStatus[] seatMap = restTemplate.getForObject(
                "/api/v1/flights/" + flightInstanceId + "/seat-map", SeatStatus[].class);
        return Arrays.stream(seatMap)
                .filter(s -> s.seatLabel().equals(seatLabel))
                .findFirst()
                .orElseThrow()
                .status();
    }
}
