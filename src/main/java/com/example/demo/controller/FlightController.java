package com.example.demo.controller;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightStatus;
import com.example.demo.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des vols.
 *
 * Un Controller est la "porte d'entrée" de l'API.
 * Il reçoit les requêtes HTTP (GET, POST, PUT, DELETE) et retourne des réponses JSON.
 * Il ne contient PAS de logique métier — il délègue tout au FlightService.
 *
 * @RestController → dit à Spring que cette classe répond aux requêtes HTTP en JSON
 * @RequestMapping → toutes les routes de ce controller commencent par /api/flights
 */
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    /**
     * POST /api/flights
     * Crée un nouveau vol à partir des données JSON reçues dans le corps de la requête.
     *
     * @RequestBody → Spring lit le JSON de la requête et le convertit en objet Flight
     * ResponseEntity.status(CREATED) → retourne le code HTTP 201 (créé avec succès)
     */
    @PostMapping
    public ResponseEntity<Flight> create(@RequestBody Flight flight) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.create(flight));
    }

    /**
     * GET /api/flights
     * Retourne tous les vols en JSON.
     * Code HTTP retourné : 200 (OK)
     */
    @GetMapping
    public List<Flight> findAll() {
        return flightService.findAll();
    }

    /**
     * GET /api/flights/{id}   — exemple : GET /api/flights/AF123
     * Retourne le vol avec cet ID, ou une erreur 404 si introuvable.
     *
     * @PathVariable → Spring extrait "AF123" depuis l'URL et le met dans la variable id
     * .map(ResponseEntity::ok)        → si trouvé, retourne 200 + le vol
     * .orElse(ResponseEntity.notFound()) → si introuvable, retourne 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Flight> findById(@PathVariable String id) {
        return flightService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/flights/{id}/status?status=ACTIVE
     * Change le statut d'un vol. Le nouveau statut est passé en paramètre URL.
     *
     * @RequestParam → Spring lit le paramètre "status" dans l'URL (?status=ACTIVE)
     * Retourne 200 si OK, 404 si le vol n'existe pas.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Flight> updateStatus(@PathVariable String id,
                                               @RequestParam FlightStatus status) {
        try {
            return ResponseEntity.ok(flightService.updateStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // vol introuvable → 404
        }
    }

    /**
     * DELETE /api/flights/{id}
     * Supprime un vol. Retourne 204 (No Content) si OK, 404 si introuvable.
     * 204 = succès mais pas de contenu à retourner (le vol n'existe plus)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            flightService.delete(id);
            return ResponseEntity.noContent().build(); // 204 : supprimé avec succès
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // 404 : vol introuvable
        }
    }

    /**
     * GET /api/flights/active
     * Retourne uniquement les vols avec le statut ACTIVE.
     */
    @GetMapping("/active")
    public List<Flight> findActive() {
        return flightService.findActive();
    }

    /**
     * GET /api/flights/delayed
     * Retourne tous les vols qui ont un retard (delayMinutes > 0).
     */
    @GetMapping("/delayed")
    public List<Flight> findDelayed() {
        return flightService.findDelayed();
    }

    /**
     * GET /api/flights/runway/{runway}   — exemple : GET /api/flights/runway/A
     * Retourne tous les vols assignés à la piste "A".
     */
    @GetMapping("/runway/{runway}")
    public List<Flight> findByRunway(@PathVariable String runway) {
        return flightService.findByRunway(runway);
    }
}
