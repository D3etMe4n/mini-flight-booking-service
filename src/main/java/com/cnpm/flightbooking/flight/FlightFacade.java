package com.cnpm.flightbooking.flight;

import com.cnpm.flightbooking.flight.dto.FlightResponse;

public interface FlightFacade {
    boolean reserveSeat(Long flightId, String seatNumber);
    void releaseSeat(Long flightId, String seatNumber);
    FlightResponse getFlightById(Long flightId);
}
