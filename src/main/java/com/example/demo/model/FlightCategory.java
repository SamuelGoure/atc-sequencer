package com.example.demo.model;

/**
 * Les catégories de vols, utilisées pour déterminer la priorité d'atterrissage.
 * Plus la priorité est haute, plus l'avion passe tôt dans la séquence.
 *
 * Ordre de priorité dans l'algorithme :
 *   MEDICAL    → priorité 1 (toujours en premier, urgence médicale)
 *   MILITARY   → priorité 2
 *   COMMERCIAL → priorité 5 (ordre normal, trié par heure d'arrivée)
 *   CARGO      → priorité 5 (même ordre que commercial)
 */
public enum FlightCategory {
    COMMERCIAL, // Vol commercial classique (Air France, EasyJet...)
    CARGO,      // Vol de fret (DHL, FedEx...)
    MEDICAL,    // Vol médical urgent — passe toujours en premier
    MILITARY    // Vol militaire — passe en deuxième
}
