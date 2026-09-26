package com.cnpm.flightbooking.flight.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface FlightSeatRepository extends JpaRepository<FlightSeat, Long> {
     Optional<FlightSeat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

}
