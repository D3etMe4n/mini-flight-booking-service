package com.cnpm.flightbooking.booking.internal;

import com.cnpm.flightbooking.booking.dto.BookingRequest;
import com.cnpm.flightbooking.payment.CustomerType;
import com.cnpm.flightbooking.payment.PaymentMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Robust End-to-End Integration Test for Booking Subsystem.

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@SuppressWarnings({"resource", "deprecation", "SqlResolve", "SqlNoDataSourceInspection"})
class BookingIntegrationTest {

    static PostgreSQLContainer postgres;
    static GenericContainer<?> redis;

    static {
        postgres = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("test_flight_db")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(false);
        postgres.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Optimize Hikari connection pooling for integration test stability
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 10);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 10000);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final Long testFlightId = 1L;
    private final String testSeatNumber = "1A";

    @BeforeEach
    void setupDatabaseFixtures() {
        jdbcTemplate.execute("DELETE FROM notification_logs");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM bookings");
        jdbcTemplate.execute("DELETE FROM flight_seats");
        jdbcTemplate.execute("DELETE FROM flights");

        // Clean Modulith event publication registry if present
        try {
            jdbcTemplate.execute("DELETE FROM event_publication");
        } catch (Exception ignored) {
            // Table may not exist if Modulith JDBC event registry is disabled in test profile
        }

        // Seed flight entity
        jdbcTemplate.update("""
            INSERT INTO flights (id, flight_number, departure_airport, arrival_airport, departure_time, arrival_time, base_price, total_seats, available_seats, version)
            VALUES (?, 'VN-101', 'HAN', 'SGN', NOW() + INTERVAL '2 day', NOW() + INTERVAL '2 day 2 hour', 1500000.00, 10, 10, 0)
            """, testFlightId);

        // Seed available seat
        jdbcTemplate.update("""
            INSERT INTO flight_seats (flight_id, seat_number, seat_class, is_reserved)
            VALUES (?, ?, 'ECONOMY', FALSE)
            """, testFlightId, testSeatNumber);
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should create booking and await async notification completion")
    void shouldCreateBookingSuccessfully() throws Exception {
        BookingRequest request = BookingRequest.builder()
                .flightId(testFlightId)
                .seatNumber(testSeatNumber)
                .customerName("John Doe")
                .customerEmail("johndoe@example.com")
                .customerPhone("0987654321")
                .customerType(CustomerType.STANDARD)
                .paymentMethod(PaymentMethod.VNPAY)
                .rewardPoints(0)
                .isHoliday(false)
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingCode", notNullValue()))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.chargedAmount", greaterThan(0.0)));

        // Verify synchronous state in Database
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.getFirst().getCustomerEmail()).isEqualTo("johndoe@example.com");
        assertThat(bookings.getFirst().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        await().atMost(5, SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM notification_logs WHERE recipient = 'johndoe@example.com' AND status = 'SENT'",
                            Integer.class
                    );
                    assertThat(count).isNotNull().isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should reject booking when seat is already reserved")
    void shouldFailWhenSeatIsAlreadyReserved() throws Exception {
        jdbcTemplate.update(
                "UPDATE flight_seats SET is_reserved = TRUE WHERE flight_id = ? AND seat_number = ?",
                testFlightId, testSeatNumber
        );

        BookingRequest request = BookingRequest.builder()
                .flightId(testFlightId)
                .seatNumber(testSeatNumber)
                .customerName("Alice Smith")
                .customerEmail("alice@example.com")
                .customerPhone("0912345678")
                .customerType(CustomerType.STANDARD)
                .paymentMethod(PaymentMethod.VNPAY)
                .rewardPoints(0)
                .isHoliday(false)
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/bookings - Should fail validation when customer email is invalid")
    void shouldFailValidationOnInvalidEmail() throws Exception {
        BookingRequest invalidRequest = BookingRequest.builder()
                .flightId(testFlightId)
                .seatNumber(testSeatNumber)
                .customerName("Invalid User")
                .customerEmail("invalid-email-pattern")
                .customerPhone("0912345678")
                .customerType(CustomerType.STANDARD)
                .paymentMethod(PaymentMethod.VNPAY)
                .rewardPoints(0)
                .isHoliday(false)
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{bookingCode} - Should return booking details")
    void shouldRetrieveBookingDetailsByCode() throws Exception {
        Booking booking = new Booking();
        booking.setBookingCode("PNR-TEST99");
        booking.setFlightId(testFlightId);
        booking.setCustomerName("David Miller");
        booking.setCustomerEmail("david@example.com");
        booking.setCustomerPhone("0981112233");
        booking.setSeatNumber(testSeatNumber);
        booking.setTotalAmount(new BigDecimal("1500000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        mockMvc.perform(get("/api/v1/bookings/{bookingCode}", "PNR-TEST99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode", is("PNR-TEST99")))
                .andExpect(jsonPath("$.customerName", is("David Miller")));
    }

    @Test
    @DisplayName("GET /api/v1/bookings - Should retrieve booking list filtered by customer email")
    void shouldRetrieveBookingsByCustomerEmail() throws Exception {
        Booking booking = new Booking();
        booking.setBookingCode("PNR-EMMA01");
        booking.setFlightId(testFlightId);
        booking.setCustomerName("Emma Watson");
        booking.setCustomerEmail("emma@example.com");
        booking.setCustomerPhone("0977889900");
        booking.setSeatNumber(testSeatNumber);
        booking.setTotalAmount(new BigDecimal("1500000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("email", "emma@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookingCode", is("PNR-EMMA01")));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{bookingId}/cancel - Should cancel booking and await cancellation notification")
    void shouldCancelBookingSuccessfully() throws Exception {
        Booking booking = new Booking();
        booking.setBookingCode("PNR-CANCEL");
        booking.setFlightId(testFlightId);
        booking.setCustomerName("Michael Scott");
        booking.setCustomerEmail("michael@example.com");
        booking.setCustomerPhone("0933445566");
        booking.setSeatNumber(testSeatNumber);
        booking.setTotalAmount(new BigDecimal("1500000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", booking.getId())
                        .param("reason", "Customer schedule changed"))
                .andExpect(status().isNoContent());

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Await background notification for cancellation event to finalize
        await().atMost(5, SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM notification_logs WHERE recipient = 'michael@example.com'",
                            Integer.class
                    );
                    assertThat(count).isNotNull().isGreaterThan(0);
                });
    }
}