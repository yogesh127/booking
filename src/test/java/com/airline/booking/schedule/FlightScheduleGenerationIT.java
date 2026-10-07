package com.airline.booking.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.airline.booking.TestcontainersConfiguration;
import com.airline.booking.aircraft.Aircraft;
import com.airline.booking.aircraft.AircraftRepository;
import com.airline.booking.flight.dto.FlightSearchResult;
import com.airline.booking.schedule.dto.CreateScheduleRequest;
import com.airline.booking.schedule.dto.ScheduleResponse;
import com.airline.booking.support.AuthTestSupport;
import com.airline.booking.support.FixedClockTestConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
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
class FlightScheduleGenerationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AircraftRepository aircraftRepository;

    private Long a320Id;
    private String adminToken;

    @BeforeEach
    void lookUpAircraft() {
        a320Id = aircraftRepository.findAll().stream()
                .filter(a -> a.getType().equals("A320"))
                .map(Aircraft::getId)
                .findFirst()
                .orElseThrow();
        adminToken = AuthTestSupport.login(restTemplate, "admin", "admin123");
    }

    @Test
    void creatingScheduleGeneratesExactlyOneInstancePerMatchingDayOverTheBookingWindow() {
        ResponseEntity<ScheduleResponse> response = createMonWedFriSchedule("XY101");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Fixed clock is 2024-01-01 (a Monday). 365 days starting on a Monday contains
        // 53 Mondays, 52 Wednesdays and 52 Fridays = 157.
        assertThat(response.getBody().generatedInstanceCount()).isEqualTo(157);
    }

    @Test
    void searchOnlyReturnsFlightsOperatingOnTheRequestedDate() {
        createMonWedFriSchedule("XY102");

        FlightSearchResult[] monday = search("DXB", "LHR", LocalDate.of(2024, 1, 1));
        FlightSearchResult xy102Monday = Arrays.stream(monday)
                .filter(r -> r.flightNumber().equals("XY102"))
                .findFirst()
                .orElseThrow();
        assertThat(xy102Monday.availableSeatsCount()).isEqualTo(180);

        FlightSearchResult[] tuesday = search("DXB", "LHR", LocalDate.of(2024, 1, 2));
        assertThat(Arrays.stream(tuesday).filter(r -> r.flightNumber().equals("XY102"))).isEmpty();
    }

    private ResponseEntity<ScheduleResponse> createMonWedFriSchedule(String flightNumber) {
        CreateScheduleRequest request = new CreateScheduleRequest(
                flightNumber, "DXB", "LHR",
                LocalTime.of(9, 30), LocalTime.of(13, 45),
                a320Id,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));
        return restTemplate.exchange("/api/v1/admin/schedules", HttpMethod.POST,
                new HttpEntity<>(request, AuthTestSupport.bearer(adminToken)), ScheduleResponse.class);
    }

    private FlightSearchResult[] search(String source, String destination, LocalDate date) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/flights/search")
                .queryParam("source", source)
                .queryParam("destination", destination)
                .queryParam("date", date)
                .toUriString();
        return restTemplate.getForObject(uri, FlightSearchResult[].class);
    }
}
