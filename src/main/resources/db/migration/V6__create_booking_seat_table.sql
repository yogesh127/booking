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
