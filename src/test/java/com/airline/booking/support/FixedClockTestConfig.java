package com.airline.booking.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fixes "now" to 2024-01-01T00:00:00Z (a Monday) so flight-instance generation counts are
 * deterministic in tests instead of depending on the day the test suite happens to run.
 */
@TestConfiguration
public class FixedClockTestConfig {

    public static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    // Named distinctly from ClockConfig's "clock" bean: Spring Boot disallows overriding a bean
    // definition by name by default, and @Primary only breaks ties between same-typed beans at
    // injection points — it doesn't let two differently-defined beans share one name.
    @Bean("fixedClock")
    @Primary
    public Clock clock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
