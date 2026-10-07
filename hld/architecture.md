# High-Level Design

## Overview

A single Spring Boot service fronting MySQL. Package-by-feature layering
(`controller → service → repository`), no separate microservices — the assignment's scope
(single airline, one DB, modest data volumes) doesn't justify the operational cost of splitting
it further; everything here is a Spring `@Service` + Spring Data repository behind a REST
controller. A stateless JWT security layer sits in front of all of it: one login endpoint issues
tokens, a servlet filter validates them on every other request, and role-based access rules decide
what each token may reach — no server-side session state, no per-request database lookup for auth.

## Components

```mermaid
flowchart TB
    subgraph SEC["Security (stateless)"]
        JAF[JwtAuthenticationFilter]
        SCF[SecurityConfig<br/>authorizeHttpRequests]
    end

    subgraph API["REST layer"]
        AC[AuthController<br/>login]
        SC[FlightScheduleController<br/>admin]
        APC[AirportController<br/>admin]
        ACC[AircraftController<br/>admin]
        FC[FlightController<br/>search + seat-map]
        BC[BookingController<br/>book / cancel / get]
    end

    subgraph SVC["Service layer"]
        AS[AuthService]
        JWS[JwtService]
        SS[FlightScheduleService]
        GEN[FlightInstanceGenerationService]
        SRCH[FlightSearchService]
        SMAP[SeatMapService]
        BS[BookingService]
        APS[AirportService]
        ACS[AircraftService]
    end

    subgraph REPO["Repositories (Spring Data JPA)"]
        AR[AirportRepository]
        ACR[AircraftRepository]
        FSR[FlightScheduleRepository]
        FIR[FlightInstanceRepository]
        BR[BookingRepository]
        BSR[BookingSeatRepository]
    end

    DB[(MySQL)]
    UDM[(InMemoryUserDetailsManager<br/>2 seeded users)]

    JAF -.populates SecurityContext.-> SCF
    SCF -.authorizes.-> API

    AC --> AS --> UDM
    AS --> JWS
    SC --> SS --> GEN
    SS --> FSR
    SS --> AR
    SS --> ACR
    GEN --> FIR
    APC --> APS --> AR
    ACC --> ACS --> ACR
    FC --> SRCH --> FIR
    SRCH --> BSR
    FC --> SMAP --> FIR
    SMAP --> BSR
    BC --> BS --> BR
    BS --> BSR
    BS --> FIR

    AR --> DB
    ACR --> DB
    FSR --> DB
    FIR --> DB
    BR --> DB
    BSR --> DB
```

`GlobalExceptionHandler` (`@RestControllerAdvice`) sits across all controllers, translating domain
exceptions and constraint/optimistic-lock failures into HTTP status codes (see `lld/design.md`).
`RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` do the equivalent job for the two auth
failure modes (401/403), which occur in the security filter chain *before* a request ever reaches
a controller, so `GlobalExceptionHandler` can't intercept them directly.

## Security architecture: where the JWT filter sits

```mermaid
flowchart LR
    REQ[Incoming request] --> JAF[JwtAuthenticationFilter<br/>reads Authorization: Bearer]
    JAF -->|valid token| CTX[SecurityContext populated<br/>principal + ROLE_* authority]
    JAF -->|missing/invalid token| ANON[Request proceeds anonymous]
    CTX --> AUTHZ{authorizeHttpRequests rules}
    ANON --> AUTHZ
    AUTHZ -->|permitAll match| CTRL[Controller]
    AUTHZ -->|role matches| CTRL
    AUTHZ -->|no auth present| EP[RestAuthenticationEntryPoint → 401]
    AUTHZ -->|wrong role| ADH[RestAccessDeniedHandler → 403]
```

`JwtAuthenticationFilter` is deliberately permissive by itself — an absent or malformed token never
throws there; it just leaves the request unauthenticated and lets `authorizeHttpRequests` decide
whether that matters for the path being hit. This keeps the filter simple and keeps *all*
authorization decisions in one place (`SecurityConfig`), rather than split across a filter and a
config class.

## ER Diagram

No schema changes for auth — the two users are in-memory (`SecurityConfig`), not a database table.

```mermaid
erDiagram
    AIRPORT ||--o{ FLIGHT_SCHEDULE : "source/destination"
    AIRCRAFT ||--o{ FLIGHT_SCHEDULE : "operates"
    FLIGHT_SCHEDULE ||--o{ FLIGHT_INSTANCE : "generates"
    FLIGHT_INSTANCE ||--o{ BOOKING : "booked on"
    BOOKING ||--o{ BOOKING_SEAT : "holds"
    FLIGHT_INSTANCE ||--o{ BOOKING_SEAT : "seat on"

    AIRPORT {
        varchar code PK
        varchar name
        varchar city
        varchar country
    }
    AIRCRAFT {
        bigint id PK
        varchar type
        int seat_rows
        varchar seat_letters
    }
    FLIGHT_SCHEDULE {
        bigint id PK
        varchar flight_number
        varchar source_airport_code FK
        varchar destination_airport_code FK
        time departure_time
        time arrival_time
        bigint aircraft_id FK
        int days_of_operation "bitmask"
        datetime created_at
    }
    FLIGHT_INSTANCE {
        bigint id PK
        bigint schedule_id FK
        date flight_date
        datetime departure_at
        datetime arrival_at
        varchar source_airport_code
        varchar destination_airport_code
    }
    BOOKING {
        bigint id PK
        char_36 reference UK
        bigint flight_instance_id FK
        varchar status
        int passenger_count
        int version
        datetime created_at
        datetime cancelled_at
    }
    BOOKING_SEAT {
        bigint id PK
        bigint booking_id FK
        bigint flight_instance_id FK
        varchar seat_label
        varchar passenger_name
        datetime released_at
        varchar active_seat_label "generated"
    }
```

## Request flow: login → authenticated request

```mermaid
sequenceDiagram
    actor User
    participant AC as AuthController
    participant AS as AuthService
    participant AM as AuthenticationManager
    participant JWS as JwtService
    participant JAF as JwtAuthenticationFilter
    participant X as Any protected controller

    User->>AC: POST /auth/login {username, password}
    AC->>AS: login(request)
    AS->>AM: authenticate(username, password)
    AM-->>AS: Authentication (or AuthenticationException → 401)
    AS->>JWS: generateToken(username, role)
    JWS-->>AS: signed JWT (HS256, 1h expiry)
    AS-->>AC: LoginResponse{token, role, expiresInSeconds}
    AC-->>User: 200 OK

    User->>JAF: any later request, Authorization: Bearer <token>
    JAF->>JWS: parse(token)
    JWS-->>JAF: username + role (or empty if invalid/expired)
    JAF->>JAF: populate SecurityContext with ROLE_<role>
    JAF->>X: forward request
    X-->>User: 200 (if role authorized) / 403 (if not)
```

## Request flow: schedule creation (admin)

```mermaid
sequenceDiagram
    actor Admin
    participant SC as FlightScheduleController
    participant SS as FlightScheduleService
    participant GEN as FlightInstanceGenerationService
    participant DB as MySQL

    Admin->>SC: POST /admin/schedules (Authorization: Bearer <ADMIN JWT>)
    SC->>SS: createSchedule(request)
    SS->>DB: validate source/destination airports + aircraft exist
    SS->>DB: INSERT flight_schedule
    SS->>GEN: generate(schedule)
    loop day 0..364
        GEN->>GEN: day-of-week matches days_of_operation?
    end
    GEN->>DB: bulk INSERT matching flight_instance rows
    GEN-->>SS: generated count
    SS-->>SC: ScheduleResponse
    SC-->>Admin: 201 Created
```

## Request flow: search → seat map → booking (including the concurrent-conflict path)

```mermaid
sequenceDiagram
    actor Customer A
    actor Customer B
    participant FC as FlightController
    participant BC as BookingController
    participant BS as BookingService
    participant DB as MySQL

    Customer A->>FC: GET /flights/search?source&destination&date (no auth)
    FC-->>Customer A: flightInstanceId, availableSeatsCount
    Customer A->>FC: GET /flights/{id}/seat-map (no auth)
    FC-->>Customer A: seat "1A" = AVAILABLE

    par Both try to book seat 1A at the same time
        Customer A->>BC: POST /bookings {flightInstanceId, seat 1A} (Bearer CLIENT JWT)
        BC->>BS: createBooking(...)
        BS->>DB: INSERT booking_seat(1A) + flush
        Customer B->>BC: POST /bookings {flightInstanceId, seat 1A} (Bearer CLIENT JWT)
        BC->>BS: createBooking(...)
        BS->>DB: INSERT booking_seat(1A) + flush
    end

    DB-->>BS: Customer A's insert commits first
    BS-->>BC: 201 Created
    BC-->>Customer A: booking reference, status CONFIRMED

    DB--xBS: Customer B's insert violates uq_booking_seat_active
    BS-->>BC: SeatUnavailableException
    BC-->>Customer B: 409 Conflict
```

## Request flow: cancellation

```mermaid
sequenceDiagram
    actor Customer
    participant BC as BookingController
    participant BS as BookingService
    participant DB as MySQL

    Customer->>BC: POST /bookings/{reference}/cancel (Bearer CLIENT JWT)
    BC->>BS: cancelBooking(reference)
    BS->>DB: SELECT booking WHERE reference = ?
    alt already CANCELLED
        BS-->>BC: current state (idempotent)
    else CONFIRMED
        BS->>DB: UPDATE booking SET status=CANCELLED (optimistic @Version check)
        BS->>DB: UPDATE booking_seat SET released_at=now() WHERE booking_id=? AND released_at IS NULL
        BS-->>BC: updated BookingResponse
    end
    BC-->>Customer: 200 OK
```

## Major design decisions, assumptions, and the concurrency/flight-generation strategy

Documented in full in `README.md` (Design decisions section) rather than duplicated here.

## Known limitations (carried over from the latest review)

- **Auth is coarse-grained by design**: two fixed roles, no per-customer booking ownership — any
  CLIENT-authenticated caller can read/cancel *any* booking by reference, not just their own. A
  `booked_by` column plus an ownership check at the service layer is the natural next step, out of
  scope for this assignment.
- **No refresh tokens or revocation**: a leaked JWT stays valid until its 1-hour expiry; there's no
  server-side session to invalidate early. Acceptable for a demo/assignment scope with two static
  accounts; not something to carry into a real multi-tenant system unchanged.
- **Flight number is not unique**: `flight_schedule.flight_number` is indexed but not constrained
  unique, so two schedules can share a flight number. Not addressed — the assignment never
  specifies uniqueness, and real airlines do occasionally reuse numbers across routes/days, so this
  was left as a modeling choice rather than "fixed."
