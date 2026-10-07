-- GENERATED, do not hand-edit.
-- Derived from the Flyway migrations at src/main/resources/db/migration/V*.sql.
-- This file is a reference dump for reviewers who want the final schema without running
-- Flyway; the migrations are the real source of truth.

-- ==== V1__create_airport_table.sql ====
CREATE TABLE airport (
    code    VARCHAR(3)  PRIMARY KEY,
    name    VARCHAR(200) NOT NULL,
    city    VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL
);

-- ==== V2__create_aircraft_table.sql ====
CREATE TABLE aircraft (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    type         VARCHAR(50) NOT NULL UNIQUE,
    seat_rows    INTEGER     NOT NULL CHECK (seat_rows > 0),
    seat_letters VARCHAR(10) NOT NULL CHECK (seat_letters REGEXP '^[A-Z]+$' COLLATE utf8mb4_0900_as_cs)
);

-- ==== V3__create_flight_schedule_table.sql ====
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

-- ==== V4__create_flight_instance_table.sql ====
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

-- ==== V5__create_booking_table.sql ====
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

-- ==== V6__create_booking_seat_table.sql ====
CREATE TABLE booking_seat (
    id                 BIGINT      PRIMARY KEY AUTO_INCREMENT,
    booking_id         BIGINT      NOT NULL REFERENCES booking (id),
    flight_instance_id BIGINT      NOT NULL REFERENCES flight_instance (id),
    seat_label         VARCHAR(5)  NOT NULL,
    passenger_name     VARCHAR(200) NOT NULL,
    released_at        DATETIME,
    active_seat_label  VARCHAR(5) GENERATED ALWAYS AS (IF(released_at IS NULL, seat_label, NULL)) VIRTUAL
);

-- Only one ACTIVE (non-released) hold on a given seat per flight instance is allowed.
-- MySQL has no partial/filtered index, so this emulates one: active_seat_label is NULL
-- whenever released_at is set, and MySQL (like Postgres) allows unlimited NULLs in a unique
-- index, so released rows never collide with each other or with anything else. An active row's
-- active_seat_label equals its seat_label, so the unique index below collapses to real
-- uniqueness on (flight_instance_id, seat_label) among active rows only. This is the actual
-- concurrency-safety mechanism for seat booking (see BookingService).
CREATE UNIQUE INDEX uq_booking_seat_active
    ON booking_seat (flight_instance_id, active_seat_label);

CREATE INDEX idx_booking_seat_booking_id ON booking_seat (booking_id);

-- ==== V7__seed_airports.sql ====
INSERT INTO airport (code, name, city, country) VALUES
    ('DXB', 'Dubai International Airport', 'Dubai', 'United Arab Emirates'),
    ('LHR', 'London Heathrow Airport', 'London', 'United Kingdom'),
    ('JFK', 'John F. Kennedy International Airport', 'New York', 'United States'),
    ('SIN', 'Singapore Changi Airport', 'Singapore', 'Singapore'),
    ('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India'),
    ('SYD', 'Sydney Kingsford Smith Airport', 'Sydney', 'Australia');

-- ==== V8__seed_aircraft.sql ====
INSERT INTO aircraft (type, seat_rows, seat_letters) VALUES
    ('A320', 30, 'ABCDEF'),
    ('B738', 32, 'ABCDEF');
