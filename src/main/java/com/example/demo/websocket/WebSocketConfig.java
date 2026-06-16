package com.example.demo.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration du WebSocket.
 *
 * WebSocket = une connexion permanente entre le navigateur et le serveur.
 * Contrairement à HTTP (le navigateur demande → le serveur répond),
 * WebSocket permet au SERVEUR d'envoyer des données quand il veut,
 * sans que le navigateur ait besoin de demander.
 *
 * Ici on utilise le protocole STOMP (Simple Text Oriented Message Protocol)
 * par-dessus WebSocket. C'est comme un "système de canaux" :
 * le serveur publie sur /topic/sequence, le navigateur est abonné et reçoit.
 *
 * @Configuration           → dit à Spring de lire cette classe au démarrage
 * @EnableWebSocketMessageBroker → active le système de messages WebSocket
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure le "broker" — le système de distribution des messages.
     *
     * enableSimpleBroker("/topic") → crée un canal de diffusion.
     *   Le serveur publie sur /topic/sequence, /topic/conflicts, /topic/alerts.
     *   Tous les navigateurs abonnés reçoivent le message instantanément.
     *
     * setApplicationDestinationPrefixes("/app") → préfixe pour les messages
     *   envoyés DU navigateur VERS le serveur (non utilisé ici mais bonne pratique).
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Définit le point de connexion WebSocket.
     *
     * addEndpoint("/ws") → l'URL à laquelle le navigateur se connecte.
     *   Dans websocket.js : new SockJS('/ws')
     *
     * withSockJS() → ajoute un fallback pour les navigateurs qui ne supportent
     *   pas WebSocket (utilise alors du HTTP long-polling à la place).
     *
     * setAllowedOriginPatterns("*") → autorise toutes les origines (développement).
     *   En production, il faudrait restreindre à l'URL de l'app.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
