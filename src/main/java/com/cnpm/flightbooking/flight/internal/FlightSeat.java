package com.cnpm.flightbooking.flight.internal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flight_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_seat", columnNames = {"flight_id", "seatNumber"})
})
@Getter
@Setter
@NoArgsConstructor
public class FlightSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @Column(nullable = false, length = 5)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatClass seatClass; // ECONOMY, BUSINESS

    @Column(nullable = false)
    private Boolean isReserved = false;
}