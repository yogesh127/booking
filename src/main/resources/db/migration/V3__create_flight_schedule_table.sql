CREATE TABLE flight_schedule (
    id                     BIGINT       PRIMARY KEY AUTO_INCREMENT,
    flight_number          VARCHAR(10)  NOT NULL,
    source_airport_code    VARCHAR(3)   NOT NULL REFERENCES airport (code),
    destination_airport_code VARCHAR(3) NOT NULL REFERENCES airport (code),
    departure_time         TIME         NOT NULL,
    arrival_time           TIME         NOT NULL,
    aircraft_id            BIGINT       NOT NULL REFERENCES aircraft (id),
    days_of_operation      INTEGER      NOT NULL CHECK (days_of_operation BETWEEN 1 AND 127),
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (source_airport_code <> destination_airport_code)
);

CREATE INDEX idx_flight_schedule_flight_number ON flight_schedule (flight_number);
