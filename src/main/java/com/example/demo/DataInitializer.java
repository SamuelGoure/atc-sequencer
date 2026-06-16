package com.example.demo;

import com.example.demo.model.Flight;
import com.example.demo.model.FlightCategory;
import com.example.demo.model.FlightStatus;
import com.example.demo.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Insère automatiquement des vols de test au démarrage de l'application.
 *
 * Sans cette classe, la base de données serait vide au premier lancement
 * et le dashboard n'afficherait rien.
 *
 * @Component    → Spring détecte et exécute cette classe automatiquement.
 * ApplicationRunner → l'interface run() est appelée juste après le démarrage de Spring.
 *
 * IMPORTANT : on vérifie d'abord si la base est vide avant d'insérer.
 * Sinon, chaque redémarrage ajouterait les mêmes vols en doublon.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final FlightRepository flightRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Si des vols existent déjà en base → on ne fait rien (évite les doublons)
        if (flightRepository.count() > 0) return;

        // L'heure actuelle sert de base pour calculer les ETAs
        LocalDateTime base = LocalDateTime.now();

        // Insère 5 vols de démonstration, un de chaque priorité
        flightRepository.saveAll(List.of(

            // Vol médical — priorité 1, atterrit dans 20 min, piste A
            new Flight("MED001", "MedAir 01",          "LYS", "CDG", base.plusMinutes(20),
                       FlightStatus.ACTIVE,    FlightCategory.MEDICAL,    1, 0,  "A"),

            // Vol militaire — priorité 2, atterrit dans 35 min, piste B
            new Flight("MIL002", "Armée 02",            "ORY", "NTE", base.plusMinutes(35),
                       FlightStatus.ACTIVE,    FlightCategory.MILITARY,   2, 0,  "B"),

            // Vol commercial avec 35 min de retard — priorité 3, piste C
            new Flight("AF123",  "Air France 123",      "CDG", "ABJ", base.plusMinutes(45),
                       FlightStatus.DELAYED,   FlightCategory.COMMERCIAL, 3, 35, "C"),

            // Vol commercial normal — priorité 5, piste D
            new Flight("BA456",  "British Airways 456", "LHR", "CDG", base.plusMinutes(60),
                       FlightStatus.SCHEDULED, FlightCategory.COMMERCIAL, 4, 0,  "D"),

            // Vol cargo avec léger retard — priorité 5, piste E
            new Flight("DHL078", "DHL Cargo 78",        "CDG", "LYS", base.plusMinutes(90),
                       FlightStatus.SCHEDULED, FlightCategory.CARGO,      5, 10, "E")
        ));
    }
}
