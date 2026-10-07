CREATE TABLE flight_instance (
    id                        BIGINT      PRIMARY KEY AUTO_INCREMENT,
    schedule_id               BIGINT      NOT NULL REFERENCES flight_schedule (id),
    flight_date               DATE        NOT NULL,
    departure_at              DATETIME    NOT NULL,
    arrival_at                DATETIME    NOT NULL,
    source_airport_code       VARCHAR(3)  NOT NULL REFERENCES airport (code),
    destination_airport_code  VARCHAR(3)  NOT NULL REFERENCES airport (code),
    UNIQUE (schedule_id, flight_date)
);

CREATE INDEX idx_flight_instance_route_date
    ON flight_instance (source_airport_code, destination_airport_code, flight_date);
