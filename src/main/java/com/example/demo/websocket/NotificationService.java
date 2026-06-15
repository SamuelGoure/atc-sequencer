package com.example.demo.websocket;

import com.example.demo.model.Flight;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendSequenceUpdate(List<Flight> sequence) {
        messagingTemplate.convertAndSend("/topic/sequence", sequence);
    }

    public void sendConflictAlert(List<Flight> conflicts) {
        messagingTemplate.convertAndSend("/topic/conflicts", conflicts);
    }

    public void sendCriticalDelayAlert(Flight flight) {
        messagingTemplate.convertAndSend("/topic/alerts",
                "RETARD CRITIQUE : " + flight.getCallSign()
                + " — " + flight.getDelayMinutes() + " min de retard");
    }
}
