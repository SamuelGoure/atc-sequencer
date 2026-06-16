package com.example.demo.websocket;

import com.example.demo.model.Flight;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service d'envoi de notifications en temps réel via WebSocket.
 *
 * Ce service est appelé par SequencerService après chaque reséquencement.
 * Il pousse des données au navigateur sur 3 canaux différents :
 *
 *   /topic/sequence  → nouvelle séquence de vols (met à jour le tableau du dashboard)
 *   /topic/conflicts → vols en conflit (affiche alerte rouge)
 *   /topic/alerts    → vol en retard critique (affiche un toast en haut à droite)
 *
 * SimpMessagingTemplate → c'est l'outil Spring pour envoyer des messages WebSocket.
 * convertAndSend() convertit l'objet Java en JSON et l'envoie sur le canal.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    // Spring injecte automatiquement cet outil d'envoi de messages
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envoie la nouvelle séquence de vols à tous les dashboards connectés.
     * Le dashboard (websocket.js) est abonné à /topic/sequence et met à jour
     * le tableau automatiquement à la réception.
     */
    public void sendSequenceUpdate(List<Flight> sequence) {
        messagingTemplate.convertAndSend("/topic/sequence", sequence);
    }

    /**
     * Envoie la liste des vols en conflit.
     * Le dashboard affiche une alerte rouge dans le panel de droite.
     */
    public void sendConflictAlert(List<Flight> conflicts) {
        messagingTemplate.convertAndSend("/topic/conflicts", conflicts);
    }

    /**
     * Envoie une alerte texte pour un vol avec plus de 30 min de retard.
     * Le dashboard affiche un toast (notification temporaire) en haut à droite.
     */
    public void sendCriticalDelayAlert(Flight flight) {
        messagingTemplate.convertAndSend("/topic/alerts",
                "RETARD CRITIQUE : " + flight.getCallSign()
                + " — " + flight.getDelayMinutes() + " min de retard");
    }
}
