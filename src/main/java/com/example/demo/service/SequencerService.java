package com.example.demo.service;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import com.example.demo.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SequencerService {

    private final FlightRepository flightRepository;
    private final NotificationService notificationService;

    public List<Flight> computeSequence(List<Flight> flights) {
        return flights.stream()
                .sorted(Comparator.comparingInt(this::priority)
                        .thenComparing(f -> f.getEta() != null ? f.getEta() : LocalDateTime.MAX))
                .toList();
    }

    public List<Flight> detectConflicts(List<Flight> flights) {
        List<Flight> conflicts = new ArrayList<>();
        for (int i = 0; i < flights.size(); i++) {
            for (int j = i + 1; j < flights.size(); j++) {
                Flight a = flights.get(i);
                Flight b = flights.get(j);
                if (isConflict(a, b)) {
                    if (!conflicts.contains(a)) conflicts.add(a);
                    if (!conflicts.contains(b)) conflicts.add(b);
                }
            }
        }
        return conflicts;
    }

    public List<Flight> resequence() {
        List<Flight> all = flightRepository.findAll().stream()
                .filter(f -> f.getStatus() != FlightStatus.LANDED
                          && f.getStatus() != FlightStatus.CANCELLED)
                .toList();

        List<Flight> sequenced = computeSequence(all);

        for (int i = 0; i < sequenced.size(); i++) {
            sequenced.get(i).setSequenceNumber(i + 1);
        }

        flightRepository.saveAll(sequenced);

        notificationService.sendSequenceUpdate(sequenced);

        List<Flight> conflicts = detectConflicts(sequenced);
        if (!conflicts.isEmpty()) {
            notificationService.sendConflictAlert(conflicts);
        }

        sequenced.stream()
                .filter(f -> f.getDelayMinutes() != null && f.getDelayMinutes() > 30)
                .forEach(notificationService::sendCriticalDelayAlert);

        return sequenced;
    }

    // Priorité basse = passe en premier
    private int priority(Flight f) {
        if (f.getCategory() == FlightCategory.MEDICAL) return 1;
        if (f.getCategory() == FlightCategory.MILITARY) return 2;
        if (f.getDelayMinutes() != null && f.getDelayMinutes() > 30) return 3;
        if (f.getDelayMinutes() != null && f.getDelayMinutes() > 15) return 4;
        return 5;
    }

    // Conflit = même piste, ETAs à moins de 5 minutes
    private boolean isConflict(Flight a, Flight b) {
        if (a.getRunway() == null || b.getRunway() == null) return false;
        if (!a.getRunway().equals(b.getRunway())) return false;
        if (a.getEta() == null || b.getEta() == null) return false;
        long diffMinutes = Math.abs(java.time.Duration.between(a.getEta(), b.getEta()).toMinutes());
        return diffMinutes <= 5;
    }
}
