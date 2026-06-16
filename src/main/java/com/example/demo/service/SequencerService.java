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

/**
 * LE COEUR DU PROJET — l'algorithme de séquencement.
 *
 * Ce service décide dans quel ordre les avions atterrissent.
 * Il contient 3 méthodes principales :
 *   1. computeSequence() → trie les vols par priorité
 *   2. detectConflicts() → détecte les conflits de piste
 *   3. resequence()      → recalcule tout et sauvegarde en base
 */
@Service
@RequiredArgsConstructor
public class SequencerService {

    private final FlightRepository flightRepository;
    private final NotificationService notificationService; // pour envoyer des alertes WebSocket

    /**
     * Trie une liste de vols selon les règles de priorité ATC.
     *
     * Comment ça marche :
     *   - .stream()  → transforme la liste en flux de données
     *   - .sorted()  → trie selon notre règle de priorité
     *   - .toList()  → reconvertit en liste
     *
     * Le tri utilise deux critères dans l'ordre :
     *   1. La priorité (chiffre retourné par priority())
     *   2. L'ETA (heure d'arrivée) en cas d'égalité de priorité
     */
    public List<Flight> computeSequence(List<Flight> flights) {
        return flights.stream()
                .sorted(Comparator.comparingInt(this::priority)
                        // si deux vols ont la même priorité, celui qui arrive le plus tôt passe en premier
                        .thenComparing(f -> f.getEta() != null ? f.getEta() : LocalDateTime.MAX))
                .toList();
    }

    /**
     * Détecte les conflits : deux vols sur la même piste à moins de 5 minutes d'écart.
     *
     * L'algorithme compare chaque paire de vols possible (boucle double).
     * Si deux vols sont en conflit, ils sont tous les deux ajoutés à la liste des conflits.
     *
     * Exemple : AF123 sur piste A à 14h00 et BA456 sur piste A à 14h03 → CONFLIT
     */
    public List<Flight> detectConflicts(List<Flight> flights) {
        List<Flight> conflicts = new ArrayList<>();

        // On compare chaque vol avec tous les vols qui suivent dans la liste
        for (int i = 0; i < flights.size(); i++) {
            for (int j = i + 1; j < flights.size(); j++) {
                Flight a = flights.get(i);
                Flight b = flights.get(j);

                if (isConflict(a, b)) {
                    // Évite d'ajouter le même vol deux fois
                    if (!conflicts.contains(a)) conflicts.add(a);
                    if (!conflicts.contains(b)) conflicts.add(b);
                }
            }
        }
        return conflicts;
    }

    /**
     * Recalcule toute la séquence et sauvegarde les numéros en base de données.
     * Envoie ensuite les notifications WebSocket au dashboard.
     *
     * Étapes :
     *   1. Récupère tous les vols actifs (exclut LANDED et CANCELLED)
     *   2. Les trie par priorité avec computeSequence()
     *   3. Assigne les numéros 1, 2, 3... à chaque vol
     *   4. Sauvegarde en base de données
     *   5. Notifie le dashboard en temps réel
     */
    public List<Flight> resequence() {
        // Étape 1 : récupère uniquement les vols qui ont encore besoin d'atterrir
        List<Flight> all = flightRepository.findAll().stream()
                .filter(f -> f.getStatus() != FlightStatus.LANDED
                          && f.getStatus() != FlightStatus.CANCELLED)
                .toList();

        // Étape 2 : trie les vols par priorité
        List<Flight> sequenced = computeSequence(all);

        // Étape 3 : assigne les numéros de séquence (i+1 car on commence à 1, pas 0)
        for (int i = 0; i < sequenced.size(); i++) {
            sequenced.get(i).setSequenceNumber(i + 1);
        }

        // Étape 4 : sauvegarde tous les changements en base d'un seul coup
        flightRepository.saveAll(sequenced);

        // Étape 5 : envoie la nouvelle séquence au dashboard via WebSocket
        notificationService.sendSequenceUpdate(sequenced);

        // Détecte et signale les conflits de piste
        List<Flight> conflicts = detectConflicts(sequenced);
        if (!conflicts.isEmpty()) {
            notificationService.sendConflictAlert(conflicts);
        }

        // Envoie une alerte pour chaque vol avec plus de 30 min de retard
        sequenced.stream()
                .filter(f -> f.getDelayMinutes() != null && f.getDelayMinutes() > 30)
                .forEach(notificationService::sendCriticalDelayAlert);

        return sequenced;
    }

    /**
     * Calcule la priorité d'un vol (nombre bas = passe en premier).
     *
     *   1 → MEDICAL    (urgence médicale, toujours prioritaire)
     *   2 → MILITARY   (vol militaire)
     *   3 → retard > 30 min (vol très en retard)
     *   4 → retard > 15 min (vol modérément en retard)
     *   5 → tout le reste (commercial et cargo, triés par ETA)
     */
    private int priority(Flight f) {
        if (f.getCategory() == FlightCategory.MEDICAL)                          return 1;
        if (f.getCategory() == FlightCategory.MILITARY)                         return 2;
        if (f.getDelayMinutes() != null && f.getDelayMinutes() > 30)            return 3;
        if (f.getDelayMinutes() != null && f.getDelayMinutes() > 15)            return 4;
        return 5; // COMMERCIAL et CARGO → classement par heure d'arrivée
    }

    /**
     * Vérifie si deux vols sont en conflit.
     * Deux vols sont en conflit si :
     *   → ils utilisent la MÊME piste (ex: "A")
     *   → ET leurs ETAs sont à 5 minutes ou moins d'écart
     *
     * Math.abs() → prend la valeur absolue (ignore si A arrive avant B ou après)
     * Duration.between().toMinutes() → calcule la différence en minutes
     */
    private boolean isConflict(Flight a, Flight b) {
        // Si l'un des deux n'a pas de piste assignée, pas de conflit possible
        if (a.getRunway() == null || b.getRunway() == null) return false;

        // Si les pistes sont différentes, aucun conflit
        if (!a.getRunway().equals(b.getRunway())) return false;

        // Si l'un des deux n'a pas d'ETA, on ne peut pas calculer
        if (a.getEta() == null || b.getEta() == null) return false;

        // Calcule la différence en minutes entre les deux ETAs
        long diffMinutes = Math.abs(java.time.Duration.between(a.getEta(), b.getEta()).toMinutes());

        // Conflit si moins de 5 minutes d'écart
        return diffMinutes <= 5;
    }
}
