package com.cnpm.flightbooking;

import org.springframework.boot.SpringApplication;

public class TestMiniFlightBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(MiniFlightBookingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
