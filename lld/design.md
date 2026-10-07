# Low-Level Design

## Modules / classes

| Package | Class | Responsibility |
|---|---|---|
| `auth` | `AuthController` | `POST /api/v1/auth/login` — the only endpoint credentials are ever sent to. |
| `auth` | `AuthService` | Delegates credential checking to Spring Security's `AuthenticationManager`, extracts the single role from the authenticated principal's authorities, asks `JwtService` for a token. |
| `auth.dto` | `LoginRequest`, `LoginResponse` | Request/response records for login. |
| `common.security` | `JwtService` | Issues and parses HS256 JWTs (`sub`=username, `role` claim, 1h expiry, configurable via `app.jwt.*`). `parse()` returns `Optional.empty()` on any malformed/expired/wrongly-signed token rather than throwing. |
| `common.security` | `JwtAuthenticationFilter` | `OncePerRequestFilter`. Reads `Authorization: Bearer <token>`; on a valid token, populates `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken` carrying a `ROLE_<role>` authority. Never rejects a request itself — an absent/invalid token just means the request proceeds unauthenticated, and `SecurityConfig`'s `authorizeHttpRequests` rules decide whether that matters. |
| `common.security` | `RestAuthenticationEntryPoint` | 401 handler — writes the standard `ApiError` JSON body instead of Spring Security's default. |
| `common.security` | `RestAccessDeniedHandler` | 403 handler — same `ApiError` shape. |
| `common.config` | `SecurityConfig` | `@EnableWebSecurity`. Defines the two in-memory users (`admin`/ROLE_ADMIN, `client`/ROLE_CLIENT), the `PasswordEncoder` (BCrypt), the `AuthenticationManager` bean (`DaoAuthenticationProvider` + `ProviderManager`, exposed so `AuthService` can use it directly), and the `SecurityFilterChain` — stateless sessions, CSRF disabled (no cookies, pure bearer-token API), `JwtAuthenticationFilter` installed before `UsernamePasswordAuthenticationFilter`. |
| `airport` | `Airport`, `AirportRepository` | Entity + Spring Data repo. |
| `airport` | `AirportService` | `createAirport` (uppercases the code before saving), `getAirport` (uppercases the lookup code) — create-and-read only. |
| `airport` | `AirportController` | `POST /api/v1/admin/airports`, `GET /api/v1/admin/airports/{code}` — ADMIN only. |
| `airport.dto` | `CreateAirportRequest`, `AirportResponse` | See **Validation** for the IATA-code pattern and column-length caps. |
| `aircraft` | `Aircraft`, `AircraftRepository` | Entity + Spring Data repo. |
| `aircraft` | `SeatMapCalculator` | Pure utility: derives seat labels/count/validity from an aircraft's `seatRows`/`seatLetters` — no seat state is ever stored per-seat. Shared by search, seat-map, and booking validation. |
| `aircraft` | `AircraftService` | `createAircraft`, `getAircraft` — create-and-read only. |
| `aircraft` | `AircraftController` | `POST /api/v1/admin/aircraft`, `GET /api/v1/admin/aircraft/{id}` — ADMIN only. |
| `aircraft.dto` | `CreateAircraftRequest`, `AircraftResponse` | See **Validation** — `seatRows` is capped, not just positive (see below). |
| `schedule` | `FlightSchedule` | Entity: the reusable template. Immutable after creation (no update endpoint). |
| `schedule` | `DaysOfWeekConverter` | `AttributeConverter<Set<DayOfWeek>, Integer>` — bitmask storage (bit = `DayOfWeek.getValue()-1`), e.g. `{MON,WED,FRI}` = 21. |
| `schedule` | `FlightScheduleRepository` | Spring Data repo. |
| `schedule` | `FlightScheduleService` | Validates referenced airports/aircraft exist and differ, persists the schedule, triggers generation. |
| `schedule` | `FlightScheduleController` | `POST /api/v1/admin/schedules`, `GET /api/v1/admin/schedules/{id}` — ADMIN only. |
| `flight` | `FlightInstance` | Entity: one concrete bookable date. Source/destination codes denormalized from the schedule for search without a join. |
| `flight` | `FlightInstanceRepository` | Spring Data repo; route+date lookup, per-schedule count. |
| `flight` | `FlightInstanceGenerationService` | Eager materialization for `[today, today+364]` matching days-of-operation; arrival-rollover math. |
| `flight` | `FlightSearchService` | Route+date lookup → available-seat count per instance. Public endpoint, no auth. |
| `flight` | `SeatMapService` | Full seat list for one instance with AVAILABLE/BOOKED status. Public endpoint, no auth. |
| `flight` | `FlightController` | `GET /api/v1/flights/search`, `GET /api/v1/flights/{id}/seat-map`. |
| `booking` | `Booking` | Entity: `@Version` for optimistic locking on status transitions. |
| `booking` | `BookingSeat` | Entity: one passenger's seat hold; `releasedAt` nullable (soft release on cancel). `seat_label VARCHAR(5)` is the column that motivated the `seatRows` cap below. |
| `booking` | `BookingRepository`, `BookingSeatRepository` | Spring Data repos. |
| `booking` | `BookingService` | Owns the concurrency-critical create/cancel logic (see README "Concurrency strategy"). |
| `booking` | `BookingController` | `POST /api/v1/bookings`, `GET /api/v1/bookings/{reference}`, `POST /api/v1/bookings/{reference}/cancel` — ADMIN or CLIENT. |
| `common` | `GlobalExceptionHandler` | `@RestControllerAdvice` — maps exceptions to HTTP status (table below), including failed-login `AuthenticationException` → 401. |
| `common` | `BookingWindowPolicy` | Single source of truth for the 365-day window constant. |
| `common.config` | `ClockConfig` | `Clock.systemUTC()` bean — injected everywhere "today" matters, instead of `LocalDate.now()`, so tests can override it with a fixed `Clock`. |

## API reference

| Method & path | Request | Response | Status | Auth |
|---|---|---|---|---|
| `POST /api/v1/auth/login` | `LoginRequest` (username, password) | `LoginResponse` (token, tokenType, role, expiresInSeconds) | 200; 401 bad credentials | public |
| `POST /api/v1/admin/schedules` | `CreateScheduleRequest` | `ScheduleResponse` incl. `generatedInstanceCount` | 201; 400 validation; 404 unknown airport/aircraft; 400 source==destination | ADMIN |
| `GET /api/v1/admin/schedules/{id}` | — | `ScheduleResponse` | 200; 404 | ADMIN |
| `POST /api/v1/admin/airports` | `CreateAirportRequest` | `AirportResponse` | 201; 400 validation; 409 duplicate code | ADMIN |
| `GET /api/v1/admin/airports/{code}` | — | `AirportResponse` | 200; 404 | ADMIN |
| `POST /api/v1/admin/aircraft` | `CreateAircraftRequest` | `AircraftResponse` | 201; 400 validation; 409 duplicate type | ADMIN |
| `GET /api/v1/admin/aircraft/{id}` | — | `AircraftResponse` | 200; 404 | ADMIN |
| `GET /api/v1/flights/search?source&destination&date` | query params | `FlightSearchResult[]` | 200 (empty list if none match) | public |
| `GET /api/v1/flights/{flightInstanceId}/seat-map` | — | `SeatStatus[]` | 200; 404 unknown instance | public |
| `POST /api/v1/bookings` | `CreateBookingRequest` | `BookingResponse` | 201; 400 validation/unknown seat/duplicate seat; 404 unknown instance; 409 seat unavailable | ADMIN or CLIENT |
| `GET /api/v1/bookings/{reference}` | — | `BookingResponse` | 200; 404 | ADMIN or CLIENT |
| `POST /api/v1/bookings/{reference}/cancel` | — | `BookingResponse` (status CANCELLED) | 200 (idempotent if already cancelled); 404; 409 concurrent modification | ADMIN or CLIENT |

Every row marked ADMIN/CLIENT returns 401 with no token and 403 with the wrong role, in the same
`ApiError` shape as every other error (see **Error handling matrix**).

## Validation

- Bean validation (`jakarta.validation`) on all request DTOs: required fields (`@NotBlank`,
  `@NotNull`, `@NotEmpty`).
- **DTOs are validated against their target column's actual constraints, not just "is it
  present."** This was a gap found in a later review and fixed across the board:
  - `CreateScheduleRequest.flightNumber` → `@Size(max = 10)` (`flight_schedule.flight_number
    VARCHAR(10)`).
  - `CreateAirportRequest.code` → `@Pattern("^[A-Za-z]{3}$")` (a real IATA code, not just "3
    characters of anything" — previously only length-checked).
  - `CreateAirportRequest.name/city/country` → `@Size(max = 200/100/100)` matching `airport`'s
    columns.
  - `CreateAircraftRequest.type` → `@Size(max = 50)` matching `aircraft.type VARCHAR(50)`.
  - `CreateAircraftRequest.seatLetters` → `@Size(max = 10)` added alongside the existing
    `@Pattern("^[A-Z]+$")`, matching `aircraft.seat_letters VARCHAR(10)`.
  - **`CreateAircraftRequest.seatRows` → `@Max(9999)` added alongside `@Positive` — the one fix
    with real consequences, not just a cleaner error message.** `SeatMapCalculator` builds a seat
    label as `row + letter` (one digit string concatenated with one character). `booking_seat
    .seat_label` is `VARCHAR(5)`. Aircraft used to be Flyway-seed-only with safe values (30, 32
    rows), so this was unreachable. Once admin onboarding accepted *any* positive `seatRows`, an
    aircraft with e.g. `seatRows: 100000` would generate labels like `"100000A"` (7 characters) —
    overflowing the column at insert time with an opaque DB error (or, worse, a silent truncation
    on a misconfigured MySQL that could collide two different rows' seat labels) instead of a
    clean 400 at onboarding time. Capping at 9999 keeps every possible label (`"9999F"`) at exactly
    5 characters, the column's limit.
  - **Deliberately NOT changed**: `PassengerSeatRequest.name` has no length cap. Its docstring
    already records why: "no passenger validation required" was interpreted at assignment-analysis
    time to mean no semantic/format/length rules on passenger names at all. Adding a cap now would
    silently reverse a recorded interpretation, not fix a gap — a genuine oversized name here is
    the one case where "the DB will reject it eventually" was an accepted, documented trade-off
    rather than an accident.
- Service-level validation (not expressible via annotations): source ≠ destination airport;
  requested seat labels exist on the aircraft's configuration; no duplicate seat labels within one
  booking request.
- Deliberately *not* re-validated at booking time: whether the flight date falls within the
  booking window — an instance only exists because generation already enforced that, so an
  unknown/out-of-window id simply 404s instead of needing a second check.

## Error handling matrix (`GlobalExceptionHandler`)

| Exception | HTTP status | Notes |
|---|---|---|
| `NotFoundException` | 404 | Unknown airport/aircraft/schedule/flight instance/booking reference. |
| `AuthenticationException` (Spring Security) | 401 | Bad login credentials. Thrown by `AuthenticationManager.authenticate(...)` inside `AuthService`, caught here same as any other controller-layer exception — this is a *different* 401 path from `RestAuthenticationEntryPoint`, which handles missing/invalid tokens on already-protected endpoints (a filter-chain-level rejection that never reaches a controller or this handler). |
| `InvalidRequestException` | 400 | Source==destination, duplicate/unknown seat label. |
| `MethodArgumentNotValidException` | 400 | Bean validation failures; response includes per-field messages. |
| `SeatUnavailableException` | 409 | Pre-check hit, or translated from a constraint violation. |
| `DataIntegrityViolationException` | 409 | Safety net — also what a duplicate airport `code` or aircraft `type` resolves to, since neither has an app-level uniqueness pre-check (consistent with how `BookingService` relies on the DB constraint rather than a pre-check for seat conflicts). |
| `BookingStateConflictException` / `ObjectOptimisticLockingFailureException` | 409 | Concurrent booking-status modification (double-cancel race). |
| `Exception` (fallback) | 500 | Logged with full stack trace; generic message returned to the client. |

Two *different* mechanisms produce a 401, and it's worth being precise about which is which in an
interview: `RestAuthenticationEntryPoint` fires in the security filter chain, before Spring MVC
dispatch, for a request with no/invalid token hitting a protected path — `GlobalExceptionHandler`
never sees it. The `AuthenticationException` row above fires *inside* a controller method
(`AuthService.login`, via `AuthenticationManager`) for a request that reached the login endpoint
successfully but supplied wrong credentials — that one *does* go through normal Spring MVC
exception handling.

## Transaction boundaries

- `FlightScheduleService.createSchedule` — one transaction covering schedule insert +
  flight-instance generation (so a failure in generation rolls back the schedule too; schedules
  never exist without their instances).
- `FlightInstanceGenerationService.generate` — participates in the caller's transaction
  (`REQUIRED` propagation, the default); not committed independently.
- `AirportService.createAirport`, `AircraftService.createAircraft` — single-statement
  transactions; nothing to coordinate.
- `BookingService.createBooking` — one transaction: booking insert + all seat inserts + explicit
  flush. A conflict on any seat rolls back the whole booking (all-or-nothing).
- `BookingService.cancelBooking` — one transaction: status update (version-checked) + seat
  release updates.
- `FlightSearchService.search`, `SeatMapService.getSeatMap`, `BookingService.getBooking`,
  `AirportService.getAirport`, `AircraftService.getAircraft`, `FlightScheduleService.getSchedule`
  — read-only transactions (`@Transactional(readOnly = true)`), needed because `spring.jpa
  .open-in-view=false` means lazy associations (e.g. `instance.getSchedule().getAircraft()`) must
  be resolved before the method returns and maps to a DTO.
- `AuthService.login` — no transaction. Credential checking and JWT issuance touch no database
  state (`InMemoryUserDetailsManager`, pure in-process signing).
