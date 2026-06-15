package com.example.demo.controller;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightStatus;
import com.example.demo.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public ResponseEntity<Flight> create(@RequestBody Flight flight) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.create(flight));
    }

    @GetMapping
    public List<Flight> findAll() {
        return flightService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flight> findById(@PathVariable String id) {
        return flightService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Flight> updateStatus(@PathVariable String id,
                                               @RequestParam FlightStatus status) {
        try {
            return ResponseEntity.ok(flightService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            flightService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/active")
    public List<Flight> findActive() {
        return flightService.findActive();
    }

    @GetMapping("/delayed")
    public List<Flight> findDelayed() {
        return flightService.findDelayed();
    }

    @GetMapping("/runway/{runway}")
    public List<Flight> findByRunway(@PathVariable String runway) {
        return flightService.findByRunway(runway);
    }
}
