# ATC-Sequencer — Guide complet pour comprendre le projet

> Ce fichier explique **tout** le projet, de zéro.
> Même si tu n'as jamais fait de Java ou de Spring Boot, tu dois pouvoir comprendre.

---

## C'est quoi ce projet ?

ATC-Sequencer est un système qui gère l'ordre d'atterrissage des avions.

Imagine un contrôleur aérien qui doit décider quel avion atterrit en premier.
Il doit respecter des règles :
- Un vol médical (urgence) passe **toujours** en premier
- Un vol militaire passe en **deuxième**
- Un vol en retard de plus de 30 minutes passe avant les vols normaux
- Les vols commerciaux et cargo passent **en dernier**, triés par heure d'arrivée

Ce projet automatise ce travail avec une API web et un dashboard visuel.

---

## Les technologies utilisées — expliquées simplement

### Java
Le langage de programmation utilisé.
C'est comme l'anglais pour parler à l'ordinateur.

### Spring Boot
Un outil Java qui permet de créer des applications web rapidement.
Sans Spring Boot, il faudrait écrire 10 fois plus de code.
Avec Spring Boot, on écrit `@RestController` et il crée automatiquement un serveur web.

### Maven
L'outil qui gère les dépendances du projet (comme npm pour JavaScript).
Le fichier `pom.xml` liste tous les outils dont on a besoin.
Quand tu fais `./mvnw install`, il télécharge tout automatiquement.

### MySQL
La base de données. C'est là où on stocke les vols.
Pense à une feuille Excel géante, mais accessible depuis le code.

### JPA / Hibernate
Un outil qui fait le lien entre le code Java et la base de données MySQL.
Au lieu d'écrire `SELECT * FROM flights`, on écrit `flightRepository.findAll()`.
C'est beaucoup plus simple.

### WebSocket
Une technologie qui permet au serveur d'envoyer des données **en temps réel** au navigateur.
Avec une API classique, le navigateur doit demander "y a-t-il du nouveau ?" toutes les X secondes.
Avec WebSocket, le serveur dit directement "voilà du nouveau !" dès que ça change.

### Docker
Un outil qui crée des "boîtes" (containers) isolées pour chaque service.
MySQL tourne dans sa boîte, l'application dans sa boîte, Grafana dans sa boîte.
Ainsi, tout le monde peut lancer le projet avec une seule commande, peu importe son ordinateur.

### JUnit / Mockito
Des outils pour tester le code automatiquement.
Au lieu de tester à la main "est-ce que ça marche ?", on écrit des tests qui vérifient tout seuls.

### Grafana
Un outil de visualisation de métriques (graphiques, statistiques en temps réel).

---

## La structure du projet — fichier par fichier

```
demo/
│
├── pom.xml                          ← La liste de courses de Maven
│
├── src/main/java/com/example/demo/
│   │
│   ├── DemoApplication.java         ← Le point de départ (le main)
│   ├── DataInitializer.java         ← Insère 5 vols de test au démarrage
│   │
│   ├── model/                       ← Les "objets" du monde réel
│   │   ├── Flight.java              ← Un avion avec ses infos
│   │   ├── FlightStatus.java        ← L'état d'un vol (SCHEDULED, ACTIVE...)
│   │   └── FlightCategory.java      ← Le type d'un vol (MEDICAL, MILITARY...)
│   │
│   ├── repository/                  ← La couche qui parle à la base de données
│   │   └── FlightRepository.java    ← Les requêtes pour chercher des vols
│   │
│   ├── service/                     ← La logique métier (les vraies règles)
│   │   ├── FlightService.java       ← Créer, modifier, supprimer des vols
│   │   └── SequencerService.java    ← L'algorithme de priorité + détection conflits
│   │
│   ├── controller/                  ← Les "portes d'entrée" de l'API
│   │   ├── FlightController.java    ← Routes pour gérer les vols (CRUD)
│   │   └── SequencerController.java ← Routes pour la séquence et les conflits
│   │
│   └── websocket/                   ← Les notifications temps réel
│       ├── WebSocketConfig.java     ← Configuration du WebSocket
│       └── NotificationService.java ← Envoie des messages au navigateur
│
├── src/main/resources/
│   ├── application.properties       ← La configuration de l'app (base de données, port...)
│   └── static/                      ← Les fichiers du dashboard (HTML/CSS/JS)
│       ├── index.html               ← La page du dashboard
│       ├── css/style.css            ← Le style visuel (fond sombre, couleurs...)
│       └── js/
│           ├── app.js               ← La logique du dashboard (appels API)
│           └── websocket.js         ← La connexion temps réel
│
├── src/test/                        ← Les tests automatisés
│   ├── java/.../SequencerServiceTest.java   ← 8 tests pour l'algorithme
│   ├── java/.../FlightControllerTest.java   ← 3 tests pour l'API
│   └── resources/application.properties    ← Config pour les tests (base H2 en mémoire)
│
├── Dockerfile                       ← La recette pour créer l'image Docker de l'app
├── docker-compose.yml               ← Lance tous les services d'un coup
└── atc-sequencer.postman_collection.json  ← Les requêtes API prêtes à l'emploi
```

---

## Comment fonctionne le code — expliqué simplement

### 1. Le modèle — `Flight.java`

C'est la représentation d'un vol dans le code.
```
Un vol a :
- un ID (ex: "AF123")
- un nom radio (callSign : "Air France 123")
- une origine ("CDG") et une destination ("ABJ")
- une heure d'arrivée prévue (eta)
- un statut (SCHEDULED, ACTIVE, DELAYED, LANDED, CANCELLED)
- une catégorie (COMMERCIAL, CARGO, MEDICAL, MILITARY)
- un numéro de séquence (sa position dans la file)
- un retard en minutes
- une piste d'atterrissage ("A", "B", "C"...)
```

Le `@Entity` au-dessus de la classe dit à Spring : "cette classe correspond à une table en base de données".
Le `@Data` de Lombok génère automatiquement les getters/setters (évite 50 lignes de code répétitif).

---

### 2. Le repository — `FlightRepository.java`

C'est la couche qui parle à MySQL.

```java
// Trouver tous les vols avec un certain statut
List<Flight> findByStatus(FlightStatus status);
```

**La magie :** Spring lit le nom de la méthode et génère automatiquement le SQL correspondant.
`findByStatus` devient `SELECT * FROM flights WHERE status = ?`
Tu n'écris jamais de SQL.

---

### 3. Le service — `FlightService.java`

C'est là où on met les règles métier simples (créer, modifier, supprimer).

```
Créer un vol   → flightRepository.save(flight)
Supprimer      → flightRepository.deleteById(id)
Changer statut → chercher le vol, changer son statut, sauvegarder
```

---

### 4. L'algorithme — `SequencerService.java`

C'est le coeur du projet. Il décide de l'ordre des vols.

```
La règle de priorité (du plus urgent au moins urgent) :
  priorité 1 → catégorie MEDICAL
  priorité 2 → catégorie MILITARY
  priorité 3 → retard > 30 minutes
  priorité 4 → retard > 15 minutes
  priorité 5 → tout le reste (trié par heure d'arrivée)
```

La détection de conflit :
```
Deux vols sont en conflit si :
  → ils sont sur la MÊME piste
  → ET leurs heures d'arrivée sont à moins de 5 minutes d'écart
```

---

### 5. Le controller — `FlightController.java`

C'est la "porte d'entrée" de l'API. Il reçoit les requêtes HTTP et répond.

```
POST   /api/flights          → Crée un vol (reçoit JSON, retourne le vol créé)
GET    /api/flights          → Retourne tous les vols en JSON
GET    /api/flights/AF123    → Retourne le vol AF123 ou erreur 404
PUT    /api/flights/AF123/status?status=ACTIVE → Change le statut
DELETE /api/flights/AF123    → Supprime le vol
```

**Comment ça marche :**
- `@RestController` dit à Spring : "cette classe répond aux requêtes HTTP"
- `@GetMapping("/api/flights")` dit : "cette méthode répond aux GET sur /api/flights"
- Spring convertit automatiquement les objets Java en JSON

---

### 6. Le WebSocket — `NotificationService.java`

Quand on reséquence les vols, le serveur envoie automatiquement :
```
/topic/sequence  → la nouvelle liste triée (le dashboard se met à jour)
/topic/conflicts → les conflits détectés (alerte rouge)
/topic/alerts    → les vols en retard critique (toast notification)
```

Le navigateur est abonné à ces canaux via `websocket.js`.
Dès que le serveur envoie quelque chose, le dashboard se met à jour **sans que tu recharges la page**.

---

### 7. Le dashboard — `index.html` + `app.js`

C'est l'interface visuelle accessible sur `http://localhost:8080`.

```
app.js fait :
  → loadSequence()   : appelle GET /api/sequence et affiche le tableau
  → loadConflicts()  : appelle GET /api/sequence/conflicts et affiche les alertes
  → addFlight()      : appelle POST /api/flights avec les données du formulaire
  → updateStatus()   : appelle PUT /api/flights/{id}/status
  → deleteFlight()   : appelle DELETE /api/flights/{id}
  → resequence()     : appelle POST /api/sequence/resequence
  → Auto-refresh toutes les 10 secondes
```

---

### 8. Les tests — `SequencerServiceTest.java`

Les tests vérifient automatiquement que l'algorithme fonctionne.

```
Test 1 : un vol MEDICAL doit toujours être en position 1
Test 2 : un vol MILITARY doit être en position 2
Test 3 : un vol avec 35 min de retard passe avant un commercial
Test 4 : deux vols sur la même piste à ±3 min → conflit détecté
Test 5 : deux vols sur des pistes différentes → pas de conflit
Test 6 : les vols LANDED et CANCELLED sont exclus de la séquence
Test 7 : une liste vide retourne une séquence vide
```

Ces 7 tests + 3 tests API + 1 test de démarrage = **12 tests au total**.
Tous verts = le projet fonctionne correctement.

---

## Comment lancer le projet

### Prérequis
- Java 21 installé (`java -version`)
- MySQL qui tourne en local
- Docker Desktop ouvert (pour Grafana)

### Étape 1 — Créer la base de données (une seule fois)
```bash
mysql -u root -e "CREATE DATABASE IF NOT EXISTS atc_sequencer;"
```

### Étape 2 — Lancer l'application
```bash
./mvnw spring-boot:run
```
Attendre de voir : `Started DemoApplication in X seconds`

### Étape 3 — Ouvrir dans le navigateur
```
Dashboard  → http://localhost:8080
Swagger    → http://localhost:8080/swagger-ui.html
```

### Étape 4 — Lancer les tests
```bash
./mvnw test
```
Résultat attendu : `Tests run: 12, Failures: 0, Errors: 0`

### Étape 5 — Avec Docker (optionnel, pour tout lancer d'un coup)
```bash
docker-compose up --build
```
Cela lance MySQL + l'application + Grafana en une commande.

---

## Les routes API — tableau complet

| Méthode | URL | Ce que ça fait | Réponse |
|---|---|---|---|
| POST | `/api/flights` | Créer un vol | 201 + le vol |
| GET | `/api/flights` | Tous les vols | 200 + liste JSON |
| GET | `/api/flights/{id}` | Un vol précis | 200 ou 404 |
| PUT | `/api/flights/{id}/status?status=ACTIVE` | Changer le statut | 200 ou 404 |
| DELETE | `/api/flights/{id}` | Supprimer | 204 ou 404 |
| GET | `/api/flights/active` | Vols actifs | 200 + liste |
| GET | `/api/flights/delayed` | Vols en retard | 200 + liste |
| GET | `/api/flights/runway/{piste}` | Vols par piste | 200 + liste |
| GET | `/api/sequence` | Vols triés par priorité | 200 + liste |
| GET | `/api/sequence/conflicts` | Conflits de piste | 200 + liste |
| POST | `/api/sequence/resequence` | Recalcule tout | 200 + liste |

---

## Exemple de vol en JSON

```json
{
  "flightId": "AF123",
  "callSign": "Air France 123",
  "origin": "CDG",
  "destination": "ABJ",
  "eta": "2026-06-16T14:00:00",
  "category": "COMMERCIAL",
  "status": "SCHEDULED",
  "runway": "A",
  "delayMinutes": 0
}
```

---

## Les 5 vols de test insérés au démarrage

| ID | Nom | Catégorie | Retard | Priorité |
|---|---|---|---|---|
| MED001 | MedAir 01 | MEDICAL | 0 min | 1 (le premier) |
| MIL002 | Armée 02 | MILITARY | 0 min | 2 |
| AF123 | Air France 123 | COMMERCIAL | 35 min | 3 (retard critique) |
| BA456 | British Airways | COMMERCIAL | 0 min | 4 |
| DHL078 | DHL Cargo 78 | CARGO | 10 min | 5 |

---

## Commandes utiles

```bash
# Lancer l'application
./mvnw spring-boot:run

# Lancer les tests
./mvnw test

# Builder le JAR (fichier exécutable)
./mvnw clean package -DskipTests

# Voir les logs MySQL Docker
docker logs atc-mysql

# Arrêter tous les containers Docker
docker-compose down

# Tout reconstruire depuis zéro
docker-compose down -v
docker-compose up --build
```

---

## Les erreurs fréquentes et leurs solutions

| Erreur | Cause | Solution |
|---|---|---|
| `Access denied for user 'root'` | Mauvais mot de passe MySQL | Vérifier `application.properties` |
| `Port 8080 already in use` | Une app tourne déjà sur 8080 | `kill $(lsof -t -i:8080)` |
| `Port 3306 already in use` | MySQL local déjà lancé | Utiliser ce MySQL directement |
| `Tests run: 0` | Mauvais dossier | Se placer dans `/IdeaProjects/demo` |
| `BUILD FAILURE` sur compile | Erreur de code | Lire le message d'erreur en rouge |

---

## L'essentiel à retenir de ce projet

**L'architecture en couches** est le principe fondamental.
Chaque couche a un rôle unique et ne fait que ça :
```
Controller  → reçoit la requête HTTP et retourne une réponse
Service     → applique les règles métier (l'algorithme de priorité)
Repository  → parle à la base de données
```

**L'algorithme de séquencement** est le coeur du projet.
Il répond à une vraie problématique : dans quel ordre les avions atterrissent ?
```
MEDICAL > MILITARY > retard > 30 min > retard > 15 min > ETA
```

**Les tests automatisés** garantissent que le code est correct.
12 tests écrits une fois, exécutés à chaque modification.
Si un test échoue, on sait immédiatement ce qui est cassé.

**Le WebSocket** rend l'application vivante.
Sans lui, le dashboard serait statique.
Avec lui, chaque reséquencement met à jour tous les écrans connectés instantanément.

**Docker** résout le problème "ça marche sur ma machine".
Une seule commande `docker-compose up` et tout le monde a le même environnement.
