# E-Commerce Microservices Platform

Architecture de microservices basée sur Quarkus, Kafka et PostgreSQL pour une plateforme e-commerce.

## 🏗️ Architecture

### Services

1. **users_service** (Port 8081)
   - Gestion des utilisateurs et authentification
   - Génération de tokens JWT
   - Rôles: USER, ADMIN

2. **taskmanager** (Port 8080)
   - Service de gestion des tâches (service existant)
   - Utilise Kafka pour communiquer avec users_service

3. **articles_service** (Port 8082)
   - Catalogue de produits
   - Gestion du stock
   - CRUD admin pour les articles
   - Publication d'événements Kafka (création, mise à jour, changement de stock)

4. **orders_service** (Port 8083)
   - Gestion des commandes
   - Relation 1->N avec les articles
   - Vérification du stock via REST client
   - Publication d'événements Kafka (création, confirmation, expédition, livraison, annulation)

5. **notifications_service** (Port 8084)
   - Service de notifications événementiel
   - Consomme les événements Kafka des autres services
   - Notifications pour les utilisateurs et admins

### Infrastructure

- **Kafka** (Port 9092): Message broker pour la communication asynchrone
- **PostgreSQL** (Port 5432): Base de données partagée avec séparation par schémas

## 🚀 Démarrage

### Prérequis

- Docker et Docker Compose
- Java 17+
- Maven 3.8+

### Build des services

```bash
# Build articles_service
cd articles_service && ./mvnw clean package -DskipTests && cd ..

# Build orders_service
cd orders_service && ./mvnw clean package -DskipTests && cd ..

# Build notifications_service
cd notifications_service && ./mvnw clean package -DskipTests && cd ..
```

### Lancement avec Docker Compose

```bash
# Démarrer tous les services
sudo docker compose up -d

# Vérifier l'état des services
sudo docker compose ps

# Voir les logs
sudo docker compose logs -f [service_name]

# Arrêter tous les services
sudo docker compose down
```

## 📚 API Documentation (Swagger UI)

### 🌟 Interface Unifiée (Recommandé)

**API Gateway - Toutes les APIs en un seul endroit:**
- **URL:** http://localhost:9000/q/swagger-ui
- **Description:** Interface Swagger centralisée avec menu déroulant pour accéder à tous les services
- **Services disponibles:**
  - Users Service
  - Taskmanager
  - Articles Service
  - Orders Service
  - Notifications Service

### 📋 Interfaces Individuelles

Si vous préférez accéder directement aux Swagger UI de chaque service:

- **users_service**: http://localhost:8081/q/swagger-ui
- **taskmanager**: http://localhost:8080/q/swagger-ui
- **articles_service**: http://localhost:8082/q/swagger-ui
- **orders_service**: http://localhost:8083/q/swagger-ui
- **notifications_service**: http://localhost:8084/q/swagger-ui

### 🔗 Endpoints OpenAPI (JSON)

Pour intégration avec d'autres outils (Postman, Insomnia, etc.):

- **API Gateway (agrégé):**
  - Users: http://localhost:9000/openapi/users
  - Taskmanager: http://localhost:9000/openapi/taskmanager
  - Articles: http://localhost:9000/openapi/articles
  - Orders: http://localhost:9000/openapi/orders
  - Notifications: http://localhost:9000/openapi/notifications

- **Services individuels:**
  - Users: http://localhost:8081/q/openapi
  - Taskmanager: http://localhost:8080/q/openapi
  - Articles: http://localhost:8082/q/openapi
  - Orders: http://localhost:8083/q/openapi
  - Notifications: http://localhost:8084/q/openapi

## 🔐 Authentification

### Obtenir un token JWT

```bash
# Créer un utilisateur
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "password123",
    "role": "USER"
  }'

# Se connecter
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123"
  }'
```

### Utiliser le token

```bash
# Exemple avec Authorization header
curl -X GET http://localhost:8082/api/articles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📊 Flux de données

### Événements Kafka

#### Topics

1. **article.events**
   - Producteur: articles_service
   - Consommateurs: notifications_service
   - Événements: ARTICLE_CREATED, ARTICLE_UPDATED, STOCK_CHANGED

2. **order.events**
   - Producteur: orders_service
   - Consommateurs: notifications_service
   - Événements: ORDER_CREATED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED

### Communication REST

- **orders_service → articles_service**: Vérification du stock et récupération des détails des articles

## 🎯 Cas d'usage

### 1. Créer un article (Admin)

```bash
curl -X POST http://localhost:8082/api/articles \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell XPS 15",
    "description": "Powerful laptop for developers",
    "price": 1299.99,
    "stock": 50
  }'
```

### 2. Passer une commande (User)

```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Authorization: Bearer USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "articleId": 1,
        "quantity": 2
      }
    ]
  }'
```

### 3. Consulter les notifications (User)

```bash
# Toutes les notifications
curl -X GET http://localhost:8084/api/notifications \
  -H "Authorization: Bearer USER_TOKEN"

# Notifications non lues
curl -X GET http://localhost:8084/api/notifications/unread \
  -H "Authorization: Bearer USER_TOKEN"

# Nombre de notifications non lues
curl -X GET http://localhost:8084/api/notifications/unread/count \
  -H "Authorization: Bearer USER_TOKEN"
```

## 🗄️ Base de données

### Structure

Tous les services utilisent la même base PostgreSQL (`appdb`) mais avec des tables séparées:

- **users_service**: table `users`
- **articles_service**: table `articles`
- **orders_service**: tables `orders`, `order_items`
- **notifications_service**: table `notifications`

### Configuration

```yaml
Database: appdb
User: appuser
Password: apppassword
Host: postgres (dans Docker) / localhost (en local)
Port: 5432
```

## 🔧 Configuration

### Variables d'environnement

Chaque service peut être configuré via des variables d'environnement:

```bash
HTTP_PORT=8082
DB_JDBC_URL=jdbc:postgresql://postgres:5432/appdb
DB_USER=appuser
DB_PASSWORD=apppassword
JWT_SECRET=super-secret-change-me-please-change-me-32-bytes
JWT_ISSUER=users-service
KAFKA_BOOTSTRAP_SERVERS=kafka:19092
```

## 📈 Monitoring

### Vérifier la santé des services

```bash
# Kafka topics
sudo docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# PostgreSQL
sudo docker exec -it postgres psql -U appuser -d appdb -c "\dt"

# Logs des services
sudo docker compose logs -f articles_service
sudo docker compose logs -f orders_service
sudo docker compose logs -f notifications_service
```

## 🛠️ Développement

### Mode développement (sans Docker)

```bash
# Terminal 1: Kafka
sudo docker compose up kafka postgres -d

# Terminal 2: articles_service
cd articles_service && ./mvnw quarkus:dev

# Terminal 3: orders_service
cd orders_service && ./mvnw quarkus:dev

# Terminal 4: notifications_service
cd notifications_service && ./mvnw quarkus:dev
```

## 📝 TODO

- [ ] Ajouter des tests unitaires et d'intégration
- [ ] Implémenter la gestion des stocks avec réservation
- [ ] Ajouter un service de paiement
- [ ] Implémenter WebSocket pour les notifications en temps réel
- [ ] Ajouter un API Gateway (Kong, Traefik)
- [ ] Implémenter le circuit breaker (Resilience4j)
- [ ] Ajouter des métriques (Prometheus, Grafana)
- [ ] Implémenter le tracing distribué (Jaeger)

## 📄 License

MIT
