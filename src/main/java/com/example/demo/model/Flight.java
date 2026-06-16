package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Représente un vol dans le système ATC.
 *
 * @Entity     → dit à Spring que cette classe correspond à une TABLE en base de données.
 *               Spring va automatiquement créer la table "flights" dans MySQL.
 *
 * @Data       → Lombok génère automatiquement tous les getters, setters,
 *               equals(), hashCode() et toString(). Évite 100 lignes de code répétitif.
 *
 * @NoArgsConstructor → génère un constructeur vide : new Flight()
 * @AllArgsConstructor → génère un constructeur avec tous les champs : new Flight(id, callSign, ...)
 */
@Entity
@Table(name = "flights") // nom de la table dans MySQL
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    /**
     * Identifiant unique du vol (ex: "AF123").
     * @Id → c'est la clé primaire de la table MySQL (comme un numéro de sécurité sociale unique).
     */
    @Id
    private String flightId;

    // Nom radio utilisé par le contrôleur (ex: "Air France 123")
    private String callSign;

    // Aéroport de départ (ex: "CDG" pour Charles de Gaulle)
    private String origin;

    // Aéroport d'arrivée (ex: "ABJ" pour Abidjan)
    private String destination;

    // Estimated Time of Arrival — heure d'arrivée prévue
    private LocalDateTime eta;

    /**
     * État actuel du vol (SCHEDULED, ACTIVE, DELAYED, LANDED, CANCELLED).
     * @Enumerated(EnumType.STRING) → stocke le texte "SCHEDULED" en base
     * au lieu d'un chiffre (0, 1, 2...). Plus lisible dans la base de données.
     */
    @Enumerated(EnumType.STRING)
    private FlightStatus status = FlightStatus.SCHEDULED; // par défaut : planifié

    /**
     * Catégorie du vol (MEDICAL, MILITARY, COMMERCIAL, CARGO).
     * Utilisée par l'algorithme de priorité dans SequencerService.
     */
    @Enumerated(EnumType.STRING)
    private FlightCategory category;

    // Position du vol dans la file d'attente (1 = passe en premier)
    private Integer sequenceNumber;

    // Nombre de minutes de retard (0 = pas de retard)
    private Integer delayMinutes = 0;

    // Lettre ou numéro de la piste d'atterrissage (ex: "A", "B", "27L")
    private String runway;
}
