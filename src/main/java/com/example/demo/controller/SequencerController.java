package com.example.demo.controller;

import com.example.demo.model.Flight;
import com.example.demo.service.FlightService;
import com.example.demo.service.SequencerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sequence")
@RequiredArgsConstructor
public class SequencerController {

    private final SequencerService sequencerService;
    private final FlightService flightService;

    @GetMapping
    public List<Flight> getSequence() {
        return sequencerService.computeSequence(flightService.findAll());
    }

    @GetMapping("/conflicts")
    public List<Flight> getConflicts() {
        return sequencerService.detectConflicts(flightService.findAll());
    }

    @PostMapping("/resequence")
    public List<Flight> resequence() {
        return sequencerService.resequence();
    }
}
