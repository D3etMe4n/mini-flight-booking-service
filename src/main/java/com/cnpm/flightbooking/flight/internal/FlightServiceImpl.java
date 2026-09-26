package com.cnpm.flightbooking.flight.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
class FlightService {

}
