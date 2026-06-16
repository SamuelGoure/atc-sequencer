package com.example.demo.repository;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Couche d'accès à la base de données pour les vols.
 *
 * JpaRepository<Flight, String> fournit déjà gratuitement :
 *   save(flight)         → INSERT ou UPDATE dans MySQL
 *   findById(id)         → SELECT WHERE id = ?
 *   findAll()            → SELECT * FROM flights
 *   deleteById(id)       → DELETE WHERE id = ?
 *   count()              → SELECT COUNT(*) FROM flights
 *   existsById(id)       → SELECT COUNT(*) WHERE id = ? > 0
 *
 * MAGIE Spring Data : les méthodes ci-dessous sont générées automatiquement
 * par Spring à partir de leur NOM. Pas besoin d'écrire le SQL.
 * Exemple : findByStatus → SELECT * FROM flights WHERE status = ?
 *
 * @Repository → dit à Spring que cette interface gère la base de données.
 */
@Repository
public interface FlightRepository extends JpaRepository<Flight, String> {

    // SELECT * FROM flights WHERE status = ?
    List<Flight> findByStatus(FlightStatus status);

    // SELECT * FROM flights WHERE category = ?
    List<Flight> findByCategory(FlightCategory category);

    // SELECT * FROM flights WHERE delay_minutes > ?
    List<Flight> findByDelayMinutesGreaterThan(int minutes);

    // SELECT * FROM flights WHERE runway = ?
    List<Flight> findByRunway(String runway);

    // SELECT * FROM flights WHERE status = ? ORDER BY sequence_number ASC
    List<Flight> findByStatusOrderBySequenceNumberAsc(FlightStatus status);
}
