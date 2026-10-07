CREATE TABLE booking (
    id               BIGINT      PRIMARY KEY AUTO_INCREMENT,
    reference        CHAR(36)    NOT NULL UNIQUE,
    flight_instance_id BIGINT    NOT NULL REFERENCES flight_instance (id),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    passenger_count  INTEGER     NOT NULL CHECK (passenger_count > 0),
    version          INTEGER     NOT NULL DEFAULT 0,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at     DATETIME
);

CREATE INDEX idx_booking_flight_instance_id ON booking (flight_instance_id);
