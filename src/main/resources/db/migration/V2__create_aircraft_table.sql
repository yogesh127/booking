CREATE TABLE aircraft (
    id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
    type         VARCHAR(50) NOT NULL UNIQUE,
    seat_rows    INTEGER     NOT NULL CHECK (seat_rows > 0),
    seat_letters VARCHAR(10) NOT NULL CHECK (seat_letters REGEXP '^[A-Z]+$' COLLATE utf8mb4_0900_as_cs)
);
