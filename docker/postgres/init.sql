CREATE TABLE IF NOT EXISTS flights (
                                       id BIGSERIAL PRIMARY KEY,
                                       flight_number VARCHAR(10) NOT NULL UNIQUE,
    departure_airport VARCHAR(50) NOT NULL,
    arrival_airport VARCHAR(50) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    version BIGINT DEFAULT 0 --Optimistic lock
    );

CREATE INDEX IF NOT EXISTS idx_flight_number ON flights(flight_number);

CREATE TABLE IF NOT EXISTS flight_seats (
                                            id BIGSERIAL PRIMARY KEY,
                                            flight_id BIGINT NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    seat_number VARCHAR(5) NOT NULL,
    seat_class VARCHAR(20) NOT NULL, -- ECONOMY, BUSINESS
    is_reserved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_flight_seat UNIQUE (flight_id, seat_number)
    );

CREATE TABLE IF NOT EXISTS bookings (
                                        id BIGSERIAL PRIMARY KEY,
                                        booking_code VARCHAR(36) NOT NULL UNIQUE,
    flight_id BIGINT NOT NULL, -- Logical reference to Flight
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    seat_number VARCHAR(5) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_booking_code ON bookings(booking_code);

CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    booking_id BIGINT NOT NULL, -- Logical reference to Booking
                                                    original_price NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    final_amount NUMERIC(12, 2) NOT NULL,
    customer_type VARCHAR(20) NOT NULL, -- STANDARD, VIP, CHILDREN
    payment_method VARCHAR(20) NOT NULL, -- CREDIT_CARD, VNPAY, MOMO
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    transaction_ref VARCHAR(100),
    failure_reason TEXT,
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS notification_logs (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 booking_id BIGINT NOT NULL,
                                                 recipient VARCHAR(100) NOT NULL,
    channel VARCHAR(10) NOT NULL, -- EMAIL, SMS
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(10) NOT NULL, -- SENT, FAILED
    error_message TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );


INSERT INTO flights (id, flight_number, departure_airport, arrival_airport, departure_time, arrival_time, base_price, total_seats, available_seats, version)
VALUES
    (1, 'VN-123', 'HAN', 'SGN', '2026-10-15 08:00:00', '2026-10-15 10:15:00', 1500000.00, 6, 6, 0),
    (2, 'VJ-456', 'SGN', 'DAD', '2026-10-16 14:30:00', '2026-10-16 15:50:00', 1200000.00, 4, 4, 0)
    ON CONFLICT (id) DO NOTHING;

SELECT setval('flights_id_seq', (SELECT MAX(id) FROM flights));

INSERT INTO flight_seats (flight_id, seat_number, seat_class, is_reserved) VALUES
                                                                               (1, '1A', 'BUSINESS', FALSE),
                                                                               (1, '1B', 'BUSINESS', FALSE),
                                                                               (1, '2A', 'ECONOMY', FALSE),
                                                                               (1, '2B', 'ECONOMY', FALSE),
                                                                               (1, '3A', 'ECONOMY', FALSE),
                                                                               (1, '3B', 'ECONOMY', FALSE)
    ON CONFLICT (flight_id, seat_number) DO NOTHING;

INSERT INTO flight_seats (flight_id, seat_number, seat_class, is_reserved) VALUES
                                                                               (2, '1A', 'ECONOMY', FALSE),
                                                                               (2, '1B', 'ECONOMY', FALSE),
                                                                               (2, '2A', 'ECONOMY', FALSE),
                                                                               (2, '2B', 'ECONOMY', FALSE)
    ON CONFLICT (flight_id, seat_number) DO NOTHING;