package com.example.demo.service;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Couche "métier" pour la gestion des vols.
 *
 * C'est ici qu'on met la logique de l'application (les règles).
 * Le Controller reçoit les requêtes HTTP et délègue ici.
 * Ce Service appelle ensuite le Repository pour parler à la base de données.
 *
 * Schéma : Controller → Service → Repository → MySQL
 *
 * @Service       → dit à Spring que c'est un service métier.
 * @RequiredArgsConstructor → Lombok injecte automatiquement flightRepository
 *                            (évite d'écrire un constructeur à la main).
 */
@Service
@RequiredArgsConstructor
public class FlightService {

    // Spring injecte automatiquement le repository au démarrage (injection de dépendance)
    private final FlightRepository flightRepository;

    // Crée un nouveau vol et le sauvegarde en base de données
    public Flight create(Flight flight) {
        return flightRepository.save(flight);
    }

    // Retourne tous les vols de la base de données
    public List<Flight> findAll() {
        return flightRepository.findAll();
    }

    /**
     * Cherche un vol par son ID.
     * Retourne Optional<Flight> — c'est une "boîte" qui contient soit le vol, soit rien.
     * Cela évite les NullPointerException si le vol n'existe pas.
     * Le Controller utilise .map() pour retourner 200 si trouvé, 404 sinon.
     */
    public Optional<Flight> findById(String flightId) {
        return flightRepository.findById(flightId);
    }

    /**
     * Change le statut d'un vol (ex: SCHEDULED → ACTIVE).
     * Lance une exception si le vol n'existe pas — le Controller retournera un 404.
     */
    public Flight updateStatus(String flightId, FlightStatus newStatus) {
        // Cherche le vol, et lance une erreur si introuvable
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Vol introuvable : " + flightId));

        flight.setStatus(newStatus); // change le statut
        return flightRepository.save(flight); // sauvegarde en base
    }

    // Supprime un vol de la base de données
    public void delete(String flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new RuntimeException("Vol introuvable : " + flightId);
        }
        flightRepository.deleteById(flightId);
    }

    // Retourne uniquement les vols avec le statut ACTIVE
    public List<Flight> findActive() {
        return flightRepository.findByStatus(FlightStatus.ACTIVE);
    }

    // Retourne tous les vols qui ont un retard (delayMinutes > 0)
    public List<Flight> findDelayed() {
        return flightRepository.findByDelayMinutesGreaterThan(0);
    }

    // Retourne les vols assignés à une piste spécifique (ex: "A")
    public List<Flight> findByRunway(String runway) {
        return flightRepository.findByRunway(runway);
    }

    // Retourne les vols d'une catégorie donnée (ex: MEDICAL)
    public List<Flight> findByCategory(FlightCategory category) {
        return flightRepository.findByCategory(category);
    }
}
