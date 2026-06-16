package com.example.demo.model;

/**
 * Les états possibles d'un vol.
 * Un vol passe par ces états dans l'ordre logique suivant :
 *   SCHEDULED → ACTIVE → LANDED
 * Il peut aussi devenir DELAYED ou CANCELLED à tout moment.
 */
public enum FlightStatus {

    SCHEDULED,  // Vol planifié, en attente — il n'est pas encore parti
    ACTIVE,     // Vol en approche — l'avion arrive, il faut lui attribuer une séquence
    DELAYED,    // Vol en retard — il sera prioritaire si le retard dépasse 15 ou 30 min
    LANDED,     // Vol atterri — terminé, exclu du calcul de séquence
    CANCELLED   // Vol annulé — exclu du calcul de séquence
}
