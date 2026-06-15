package com.example.demo.service;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    public Flight create(Flight flight) {
        return flightRepository.save(flight);
    }

    public List<Flight> findAll() {
        return flightRepository.findAll();
    }

    public Optional<Flight> findById(String flightId) {
        return flightRepository.findById(flightId);
    }

    public Flight updateStatus(String flightId, FlightStatus newStatus) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Vol introuvable : " + flightId));
        flight.setStatus(newStatus);
        return flightRepository.save(flight);
    }

    public void delete(String flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new RuntimeException("Vol introuvable : " + flightId);
        }
        flightRepository.deleteById(flightId);
    }

    public List<Flight> findActive() {
        return flightRepository.findByStatus(FlightStatus.ACTIVE);
    }

    public List<Flight> findDelayed() {
        return flightRepository.findByDelayMinutesGreaterThan(0);
    }

    public List<Flight> findByRunway(String runway) {
        return flightRepository.findByRunway(runway);
    }

    public List<Flight> findByCategory(FlightCategory category) {
        return flightRepository.findByCategory(category);
    }
}
