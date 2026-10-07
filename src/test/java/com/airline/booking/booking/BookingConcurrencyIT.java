package com.airline.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.airline.booking.TestcontainersConfiguration;
import com.airline.booking.aircraft.Aircraft;
import com.airline.booking.aircraft.AircraftRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

/**
 * Must run against real HTTP + a real MySQL connection pool (not a single transactional test
 * connection), otherwise the actual DB-level race this proves correct would never occur in the
 * test itself.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import({TestcontainersConfiguration.class, FixedClockTestConfig.class})
class BookingConcurrencyIT {

    private static final int CONTENDERS = 10;
    private static final String CONTESTED_SEAT = "5A";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AircraftRepository aircraftRepository;

    private String clientToken;

    private Long flightInstanceId;

    @BeforeEach
    void createScheduleAndResolveFlightInstance() {
        String adminToken = AuthTestSupport.login(restTemplate, "admin", "admin123");
        clientToken = AuthTestSupport.login(restTemplate, "client", "client123");

        Long a320Id = aircraftRepository.findAll().stream()
                .filter(a -> a.getType().equals("A320"))
                .map(Aircraft::getId)
                .findFirst()
                .orElseThrow();

        CreateScheduleRequest scheduleRequest = new CreateScheduleRequest(
                "CC100", "DXB", "LHR",
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
                .filter(r -> r.flightNumber().equals("CC100"))
                .findFirst()
                .orElseThrow()
                .flightInstanceId();
    }

    @Test
    void exactlyOneConcurrentRequestWinsTheContestedSeat() throws Exception {
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONTENDERS);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < CONTENDERS; i++) {
            String passengerName = "Passenger-" + i;
            tasks.add(() -> {
                ready.countDown();
                go.await();
                CreateBookingRequest request = new CreateBookingRequest(flightInstanceId,
                        List.of(new PassengerSeatRequest(passengerName, CONTESTED_SEAT)));
                ResponseEntity<String> response = restTemplate.exchange("/api/v1/bookings", HttpMethod.POST,
                        new HttpEntity<>(request, AuthTestSupport.bearer(clientToken)), String.class);
                return response.getStatusCode().value();
            });
        }

        List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();

        List<Integer> outcomes = new ArrayList<>();
        for (Future<Integer> future : futures) {
            outcomes.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long successes = outcomes.stream().filter(s -> s == HttpStatus.CREATED.value()).count();
        long conflicts = outcomes.stream().filter(s -> s == HttpStatus.CONFLICT.value()).count();

        assertThat(successes).isEqualTo(1);
        assertThat(conflicts).isEqualTo(CONTENDERS - 1);

        SeatStatus[] seatMap = restTemplate.getForObject(
                "/api/v1/flights/" + flightInstanceId + "/seat-map", SeatStatus[].class);
        long bookedCount = Arrays.stream(seatMap).filter(s -> s.status() == SeatStatus.Status.BOOKED).count();
        assertThat(bookedCount).isEqualTo(1);
        assertThat(Arrays.stream(seatMap)
                .filter(s -> s.seatLabel().equals(CONTESTED_SEAT))
                .findFirst()
                .orElseThrow()
                .status()).isEqualTo(SeatStatus.Status.BOOKED);
    }
}
