package com.cnpm.flightbooking.flight.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface FlightRepository extends JpaRepository<Flight, Long> {
}
