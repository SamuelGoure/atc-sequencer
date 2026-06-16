package com.example.demo.controller;

import com.example.demo.model.Flight;
import com.example.demo.service.FlightService;
import com.example.demo.service.SequencerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour l'algorithme de séquencement.
 *
 * Toutes les routes commencent par /api/sequence.
 * Ce controller expose les 3 fonctionnalités principales du projet :
 *   - Calculer la séquence
 *   - Détecter les conflits
 *   - Reséquencer et sauvegarder
 */
@RestController
@RequestMapping("/api/sequence")
@RequiredArgsConstructor
public class SequencerController {

    private final SequencerService sequencerService;
    private final FlightService flightService;

    /**
     * GET /api/sequence
     * Retourne tous les vols triés par ordre de priorité.
     * Ne sauvegarde rien — c'est juste une consultation.
     *
     * Exemple de réponse : MED001 (priorité 1), MIL002 (priorité 2), AF123 (retard 35 min)...
     */
    @GetMapping
    public List<Flight> getSequence() {
        return sequencerService.computeSequence(flightService.findAll());
    }

    /**
     * GET /api/sequence/conflicts
     * Retourne les vols en conflit (même piste, moins de 5 minutes d'écart).
     * Si la liste est vide → aucun conflit, tout va bien.
     * Si elle contient des vols → il faut réaffecter une piste à l'un d'eux.
     */
    @GetMapping("/conflicts")
    public List<Flight> getConflicts() {
        return sequencerService.detectConflicts(flightService.findAll());
    }

    /**
     * POST /api/sequence/resequence
     * Recalcule toute la séquence ET sauvegarde les numéros en base de données.
     * Après cet appel, chaque vol a un sequenceNumber mis à jour (1, 2, 3...).
     * Envoie automatiquement une notification WebSocket au dashboard.
     */
    @PostMapping("/resequence")
    public List<Flight> resequence() {
        return sequencerService.resequence();
    }
}
