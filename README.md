# ATC-Sequencer — Système de séquencement du trafic aérien

Système de séquencement intelligent du trafic aérien en architecture microservices Java/Spring Boot,
inspiré du produit TopSky Sequencer de Thales.

---

## Etat actuel du projet

| Fichier | Etat | Note |
|---|---|---|
| `DemoApplication.java` | Propre | Point d'entrée Spring Boot |
| `pom.xml` | A compléter | Dépendances JPA/MySQL/Lombok manquantes |
| `application.properties` | A compléter | Config MySQL à ajouter |
| `Flight.java` | Supprimé | Fichier cassé, à recréer proprement |
| `HelloController.java` | Supprimé | Contrôleur de test inutile |
| `SalutController.java` | Supprimé | Contrôleur de test inutile |

---

## Stack technique

- Java 21
- Spring Boot 4.1.0
- Maven
- MySQL 8 (via Docker)
- Lombok
- WebSocket
- Springdoc OpenAPI (Swagger)
- JUnit 5 + Mockito
- Docker + Docker Compose
- GitHub Actions (CI/CD)
- Grafana + Prometheus

---

## Structure cible du projet

```
src/main/java/com/example/demo/
├── model/
│   ├── Flight.java
│   ├── FlightStatus.java
│   └── FlightCategory.java
├── repository/
│   └── FlightRepository.java
├── service/
│   ├── FlightService.java
│   └── SequencerService.java
├── controller/
│   ├── FlightController.java
│   └── SequencerController.java
├── websocket/
│   ├── WebSocketConfig.java
│   └── NotificationService.java
└── DemoApplication.java
```

---

## Etapes de développement

### ETAPE 0 — Outils à installer (Jour 0)

```bash
# Vérifier que tout est en place
java -version     # doit afficher 21
docker --version  # doit afficher 20+
git --version     # doit afficher 2+
```

Outils nécessaires :
- JDK 21 (déjà installé si `java -version` répond)
- IntelliJ IDEA Community
- Docker Desktop
- Git
- Postman

---

### ETAPE 1 — Mettre à jour le pom.xml

Ajouter les dépendances manquantes dans `pom.xml` :

```xml
<!-- JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- DevTools -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>

<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Swagger / OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>

<!-- Mockito pour les tests -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

### ETAPE 2 — Lancer MySQL via Docker

```bash
docker run --name atc-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=atc_sequencer \
  -p 3306:3306 \
  -d mysql:8
```

Vérifier que le container tourne :
```bash
docker ps
```

---

### ETAPE 3 — Configurer application.properties

Fichier : `src/main/resources/application.properties`

```properties
spring.application.name=atc-sequencer

# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/atc_sequencer
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

# Port
server.port=8080
```

---

### ETAPE 4 — Créer l'entité Flight

Fichier : `src/main/java/com/example/demo/model/Flight.java`

Champs requis :
- `flightId` (String, clé primaire)
- `callSign` (String)
- `origin` (String)
- `destination` (String)
- `eta` (LocalDateTime)
- `status` (Enum : SCHEDULED, ACTIVE, DELAYED, LANDED, CANCELLED)
- `category` (Enum : COMMERCIAL, CARGO, MEDICAL, MILITARY)
- `sequenceNumber` (Integer)
- `delayMinutes` (Integer, défaut 0)
- `runway` (String)

Utiliser `@Entity`, `@Id`, et les annotations Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`.

---

### ETAPE 5 — Créer le FlightRepository

Fichier : `src/main/java/com/example/demo/repository/FlightRepository.java`

Etend `JpaRepository<Flight, String>` avec ces méthodes personnalisées :
```java
List<Flight> findByStatus(FlightStatus status);
List<Flight> findByCategory(FlightCategory category);
List<Flight> findByDelayMinutesGreaterThan(int minutes);
List<Flight> findByRunway(String runway);
List<Flight> findByStatusOrderBySequenceNumberAsc(FlightStatus status);
```

---

### ETAPE 6 — Créer les Controllers REST

#### FlightController — routes CRUD

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/flights` | Créer un vol |
| GET | `/api/flights` | Lister tous les vols |
| GET | `/api/flights/{id}` | Détail d'un vol |
| PUT | `/api/flights/{id}/status` | Changer le statut |
| DELETE | `/api/flights/{id}` | Supprimer un vol |
| GET | `/api/flights/active` | Vols actifs uniquement |
| GET | `/api/flights/delayed` | Vols en retard |
| GET | `/api/flights/runway/{id}` | Vols par piste |

#### SequencerController — routes séquencement

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/sequence` | Liste triée par priorité |
| GET | `/api/sequence/conflicts` | Conflits détectés |
| POST | `/api/sequence/resequence` | Recalcule et sauvegarde |

---

### ETAPE 7 — Algorithme de séquencement (SequencerService)

Ordre de priorité :
1. Vols MEDICAL
2. Vols MILITARY
3. Vols avec `delayMinutes > 30`
4. Vols avec `delayMinutes > 15`
5. COMMERCIAL et CARGO (tri par ETA)

Méthodes à implémenter :
- `computeSequence(List<Flight>)` — tri selon les priorités
- `detectConflicts(List<Flight>)` — 2 vols sur la même piste à ±5 minutes
- `resequence()` — recalcule et met à jour `sequenceNumber` en base

---

### ETAPE 8 — Notifications WebSocket

Configuration : endpoint `/ws`, broker `/topic`

Canaux :
- `/topic/sequence` — nouvelle séquence calculée
- `/topic/conflicts` — conflit détecté
- `/topic/alerts` — vol en retard critique (> 30 min)

---

### ETAPE 9 — Tests JUnit 5

Tests unitaires pour `SequencerService` :
1. `medical_flight_should_have_highest_priority()`
2. `military_flight_should_have_second_priority()`
3. `delayed_30min_should_be_reprioritized()`
4. `should_detect_conflict_on_same_runway()`
5. `should_not_detect_conflict_different_runways()`
6. `sequence_should_update_after_cancellation()`
7. `empty_flight_list_should_return_empty_sequence()`

Tests d'intégration pour `FlightController` :
8. `should_create_flight_and_return_201()`
9. `should_return_404_for_unknown_flight()`
10. `should_return_400_for_invalid_status()`

Lancer les tests :
```bash
mvn test
```
Objectif : 10/10 tests passent, couverture > 80%

---

### ETAPE 10 — Docker

#### Dockerfile (multi-stage build)

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### docker-compose.yml

Services :
- `flight-service` (port 8080)
- `mysql` (port 3306)
- `grafana` (port 3000)

```bash
# Lancer tout le projet
docker-compose up --build

# Voir les logs
docker-compose logs -f flight-service

# Arrêter et supprimer les volumes
docker-compose down -v
```

---

### ETAPE 11 — CI/CD GitHub Actions

Fichier : `.github/workflows/ci-cd.yml`

Jobs :
1. `build` — `mvn clean package`
2. `test` — `mvn test` avec rapport de couverture
3. `docker` — `docker-compose build`

Déclenché sur push et pull_request sur `main`.

```bash
# Pousser sur GitHub
git init
git add .
git commit -m "feat: initial ATC-Sequencer implementation"
git remote add origin https://github.com/VOTRE_USERNAME/atc-sequencer.git
git push -u origin main
```

---

### ETAPE 12 — Monitoring Grafana

Accès : `http://localhost:3000` (login: admin / admin)

Métriques à configurer :
1. Nombre de vols actifs en temps réel
2. Nombre de conflits détectés par heure
3. Taux de vols en retard (%)
4. Temps moyen de réponse API (ms)
5. Nombre de reséquencements par heure

Ajouter Spring Boot Actuator et Micrometer pour exposer les métriques sur `/actuator/metrics`.

---

## Tester avec Postman

Créer un vol :
```
POST http://localhost:8080/api/flights
Content-Type: application/json

{
  "flightId": "AF123",
  "callSign": "Air France 123",
  "origin": "CDG",
  "destination": "ABJ",
  "category": "COMMERCIAL",
  "runway": "A",
  "delayMinutes": 0
}
```

Lister les vols :
```
GET http://localhost:8080/api/flights
```

Changer le statut :
```
PUT http://localhost:8080/api/flights/AF123/status?status=ACTIVE
```

Calculer la séquence :
```
GET http://localhost:8080/api/sequence
```

---

## Checklist

### Jour 1
- [ ] Dépendances pom.xml mises à jour
- [ ] MySQL lancé via Docker
- [ ] application.properties configuré
- [ ] Entité Flight + enums créés
- [ ] FlightRepository créé
- [ ] Routes REST CRUD fonctionnelles
- [ ] Algorithme de séquencement opérationnel
- [ ] Testé avec Postman

### Jour 2
- [ ] WebSocket notifications configuré
- [ ] 10 tests JUnit écrits et passants
- [ ] Dockerfile + docker-compose fonctionnels
- [ ] Pipeline GitHub Actions qui passe
- [ ] Code pushé sur GitHub

### Jour 3
- [ ] Dashboard Grafana configuré
- [ ] README professionnel rédigé
- [ ] Démo vidéo enregistrée (3 min)

---

## Commandes utiles

```bash
# Voir les logs MySQL
docker logs atc-mysql

# Redémarrer les containers
docker-compose restart

# Reconstruire depuis zéro
docker-compose down -v
docker-compose up --build

# Lancer les tests
mvn test

# Builder le projet
mvn clean package -DskipTests

# Voir les ports utilisés
docker-compose ps
```

---

## Pitch entretien (45 secondes)

> "J'ai développé ATC-Sequencer, un système de séquencement du trafic aérien en microservices Java/Spring Boot.
>
> Le projet implémente un algorithme de priorisation basé sur des règles métier aéronautiques — vols médicaux, militaires, retards critiques — avec notification WebSocket en temps réel.
>
> Côté qualité : 10 tests automatisés JUnit avec 85% de couverture, pipeline CI/CD GitHub Actions, déploiement Docker complet et monitoring Grafana.
>
> C'est exactement l'approche DevOps et la culture qualité que vous décrivez sur TopSky Sequencer."
