package com.airline.booking.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * All timestamps are UTC (no timezone conversion, per assumptions). Injecting this bean instead
 * of calling {@code LocalDate.now()}/{@code Instant.now()} directly keeps "today"-dependent logic
 * (flight-instance generation) deterministic and testable via a fixed {@link Clock} override.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
