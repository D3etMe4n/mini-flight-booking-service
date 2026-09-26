package com.cnpm.flightbooking.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingRepository extends JpaRepository<Booking, Long> {
}
