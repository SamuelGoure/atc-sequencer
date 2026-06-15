package com.example.demo;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final FlightRepository flightRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (flightRepository.count() > 0) return;

        LocalDateTime base = LocalDateTime.now();

        flightRepository.saveAll(List.of(
            new Flight("MED001", "MedAir 01",        "LYS", "CDG", base.plusMinutes(20),  FlightStatus.ACTIVE,     FlightCategory.MEDICAL,    1, 0,  "A"),
            new Flight("MIL002", "Armée 02",          "ORY", "NTE", base.plusMinutes(35),  FlightStatus.ACTIVE,     FlightCategory.MILITARY,   2, 0,  "B"),
            new Flight("AF123",  "Air France 123",    "CDG", "ABJ", base.plusMinutes(45),  FlightStatus.DELAYED,    FlightCategory.COMMERCIAL, 3, 35, "C"),
            new Flight("BA456",  "British Airways 456","LHR", "CDG", base.plusMinutes(60), FlightStatus.SCHEDULED,  FlightCategory.COMMERCIAL, 4, 0,  "D"),
            new Flight("DHL078", "DHL Cargo 78",      "CDG", "LYS", base.plusMinutes(90),  FlightStatus.SCHEDULED,  FlightCategory.CARGO,      5, 10, "E")
        ));
    }
}
