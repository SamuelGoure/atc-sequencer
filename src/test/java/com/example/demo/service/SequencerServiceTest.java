package com.example.demo.service;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import com.example.demo.websocket.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SequencerServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SequencerService sequencerService;

    private Flight medicalFlight;
    private Flight militaryFlight;
    private Flight commercialFlight;
    private Flight delayedHeavyFlight;
    private Flight delayedMediumFlight;

    @BeforeEach
    void setUp() {
        LocalDateTime eta = LocalDateTime.now().plusHours(1);

        medicalFlight = new Flight("MED1", "MedAir 1", "CDG", "LYS", eta,
                FlightStatus.SCHEDULED, FlightCategory.MEDICAL, null, 0, "A");

        militaryFlight = new Flight("MIL1", "Armée 1", "ORY", "MRS", eta,
                FlightStatus.SCHEDULED, FlightCategory.MILITARY, null, 0, "B");

        commercialFlight = new Flight("COM1", "Air France 1", "CDG", "NCE", eta,
                FlightStatus.SCHEDULED, FlightCategory.COMMERCIAL, null, 0, "C");

        delayedHeavyFlight = new Flight("DEL1", "Delayed Heavy", "CDG", "NTE", eta,
                FlightStatus.DELAYED, FlightCategory.COMMERCIAL, null, 35, "D");

        delayedMediumFlight = new Flight("DEL2", "Delayed Medium", "CDG", "BOD", eta,
                FlightStatus.DELAYED, FlightCategory.COMMERCIAL, null, 20, "E");
    }

    @Test
    void medical_flight_should_have_highest_priority() {
        List<Flight> flights = List.of(commercialFlight, militaryFlight, medicalFlight);

        List<Flight> result = sequencerService.computeSequence(flights);

        assertThat(result.get(0).getFlightId()).isEqualTo("MED1");
    }

    @Test
    void military_flight_should_have_second_priority() {
        List<Flight> flights = List.of(commercialFlight, militaryFlight, medicalFlight);

        List<Flight> result = sequencerService.computeSequence(flights);

        assertThat(result.get(1).getFlightId()).isEqualTo("MIL1");
    }

    @Test
    void delayed_30min_should_be_reprioritized_before_commercial() {
        List<Flight> flights = List.of(commercialFlight, delayedHeavyFlight);

        List<Flight> result = sequencerService.computeSequence(flights);

        assertThat(result.get(0).getFlightId()).isEqualTo("DEL1");
    }

    @Test
    void delayed_15min_should_be_before_commercial_but_after_heavy_delay() {
        List<Flight> flights = List.of(commercialFlight, delayedHeavyFlight, delayedMediumFlight);

        List<Flight> result = sequencerService.computeSequence(flights);

        assertThat(result.get(0).getFlightId()).isEqualTo("DEL1");
        assertThat(result.get(1).getFlightId()).isEqualTo("DEL2");
    }

    @Test
    void should_detect_conflict_on_same_runway() {
        LocalDateTime eta = LocalDateTime.now().plusHours(2);
        Flight f1 = new Flight("F1", "Vol 1", "CDG", "LYS", eta,
                FlightStatus.ACTIVE, FlightCategory.COMMERCIAL, null, 0, "A");
        Flight f2 = new Flight("F2", "Vol 2", "ORY", "NCE", eta.plusMinutes(3),
                FlightStatus.ACTIVE, FlightCategory.COMMERCIAL, null, 0, "A");

        List<Flight> conflicts = sequencerService.detectConflicts(List.of(f1, f2));

        assertThat(conflicts).hasSize(2);
        assertThat(conflicts).extracting(Flight::getFlightId).containsExactlyInAnyOrder("F1", "F2");
    }

    @Test
    void should_not_detect_conflict_on_different_runways() {
        LocalDateTime eta = LocalDateTime.now().plusHours(2);
        Flight f1 = new Flight("F1", "Vol 1", "CDG", "LYS", eta,
                FlightStatus.ACTIVE, FlightCategory.COMMERCIAL, null, 0, "A");
        Flight f2 = new Flight("F2", "Vol 2", "ORY", "NCE", eta.plusMinutes(3),
                FlightStatus.ACTIVE, FlightCategory.COMMERCIAL, null, 0, "B");

        List<Flight> conflicts = sequencerService.detectConflicts(List.of(f1, f2));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void sequence_should_exclude_landed_and_cancelled_flights() {
        Flight landed = new Flight("LND1", "Landed", "CDG", "LYS", LocalDateTime.now(),
                FlightStatus.LANDED, FlightCategory.COMMERCIAL, null, 0, "A");
        Flight cancelled = new Flight("CAN1", "Cancelled", "ORY", "NCE", LocalDateTime.now(),
                FlightStatus.CANCELLED, FlightCategory.COMMERCIAL, null, 0, "B");

        when(flightRepository.findAll()).thenReturn(List.of(landed, cancelled));

        List<Flight> result = sequencerService.resequence();

        assertThat(result).isEmpty();
        verify(flightRepository).saveAll(List.of());
    }

    @Test
    void empty_flight_list_should_return_empty_sequence() {
        List<Flight> result = sequencerService.computeSequence(List.of());

        assertThat(result).isEmpty();
    }
}
