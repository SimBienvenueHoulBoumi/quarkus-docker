# 🚀 E-Commerce Microservices - Kafka Quarkus

Architecture microservices avec **Quarkus**, **Kafka** et **PostgreSQL**.

---

## 📋 Table des Matières

1. [Architecture](#-architecture)
2. [Démarrage Rapide](#-démarrage-rapide)
3. [URLs d'Accès](#-urls-daccès)
4. [API Endpoints](#-api-endpoints)
5. [Authentification JWT](#-authentification-jwt)
6. [Exemples d'Utilisation](#-exemples-dutilisation)
7. [Troubleshooting](#-troubleshooting)

---

## 🏗️ Architecture

```
Client → Traefik (80) → API Gateway (9000) → Users (8081)
                                    → Articles (8082)
                                    → Orders (8083)
                                    → Notifications (8084)
                                    ↓
                    PostgreSQL (5432) + Kafka (9092-9094)
```

**Stack:**
- Backend: Quarkus 3.x (Java 17+)
- Message Broker: Kafka 3.9.1 (3 brokers)
- Database: PostgreSQL 15
- Reverse Proxy: Traefik 2.10
- Container: Docker Compose

---

## 🚀 Démarrage Rapide

**⚠️ Important:** Les services Quarkus doivent être construits avec Maven avant de démarrer Docker Compose.

traefik joue le role de reverse proxy et loadBalancer 

```bash
# 1. Construire les services Quarkus (OBLIGATOIRE)
cd users_service && mvn clean package && cd ..
cd articles_service && mvn clean package && cd ..
cd orders_service && mvn clean package && cd ..
cd notifications_service && mvn clean package && cd ..
cd api-gateway && mvn clean package && cd ..

# 2. Démarrer les conteneurs
sudo docker compose up -d --build

# Vérifier
sudo docker compose ps

# Logs
sudo docker compose logs -f

# Arrêter
sudo docker compose down
```

---

## 🌐 URLs d'Accès

### Depuis la VM (192.168.64.33)

| Service | URL | Swagger UI | OpenAPI |
|---------|-----|-----------|---------|
| **Traefik Dashboard** | http://192.168.64.33:8090/dashboard/#/ | - | - |
| **API Gateway** | http://192.168.64.33 | http://192.168.64.33/q/swagger-ui | http://192.168.64.33/q/openapi |
| **Users** | http://192.168.64.33/users | http://192.168.64.33:8081/swagger/users | http://192.168.64.33/openapi/users |
| **Articles** | http://192.168.64.33/articles | http://192.168.64.33:8082/swagger/articles | http://192.168.64.33/openapi/articles |
| **Orders** | http://192.168.64.33/orders | http://192.168.64.33:8083/swagger/orders | http://192.168.64.33/openapi/orders |
| **Notifications** | http://192.168.64.33/notifications | http://192.168.64.33:8084/swagger/notifications | http://192.168.64.33/openapi/notifications |

### Depuis Machine Locale (Tunnel SSH)

```bash
# Créer le tunnel (avec Traefik)
ssh -L 80:localhost:80 -L 8090:localhost:8090 k8s@192.168.64.33
```

Puis accéder via:
- API Gateway: http://192.168.64.33/q/swagger-ui/
- Traefik Dashboard: http://192.168.64.33:8090/dashboard/

---

## 📡 API Endpoints

### Users Service (8081)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/auth/register` | Créer compte USER | Non |
| POST | `/api/auth/register/admin` | Créer compte ADMIN | Non* |
| POST | `/api/auth/login` | Connexion | Non |
| GET | `/api/users/me` | Mon profil | JWT |
| PUT | `/api/users/me` | Modifier profil | JWT |
| GET | `/api/users` | Liste users | ADMIN |

> \* Protégez cette route en production (ex. token administrateur provisoire ou scripts de migration) pour éviter la création d'administrateurs non autorisés.

### Articles Service (8082)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/articles` | Liste articles | Non |
| GET | `/api/articles/{id}` | Détails article | Non |
| POST | `/api/articles` | Créer article | ADMIN |
| PUT | `/api/articles/{id}` | Modifier article | ADMIN |
| PATCH | `/api/articles/{id}/stock` | Maj stock | ADMIN |
| DELETE | `/api/articles/{id}` | Supprimer | ADMIN |

### Orders Service (8083)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/orders` | Mes commandes | JWT |
| GET | `/api/orders/{id}` | Détails commande | JWT |
| POST | `/api/orders` | Créer commande | JWT |
| PATCH | `/api/orders/{id}/status` | Changer statut | ADMIN |

**Statuts:** PENDING → CONFIRMED → SHIPPED → DELIVERED (ou CANCELLED)

### Notifications Service (8084)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/notifications` | Mes notifications | JWT |
| GET | `/api/notifications/unread` | Non lues | JWT |
| GET | `/api/notifications/unread/count` | Nombre non lues | JWT |
| PATCH | `/api/notifications/{id}/read` | Marquer lue | JWT |

---

## 🔐 Authentification JWT

### 1. Créer un Compte

```bash
curl -X POST http://192.168.64.33/users/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "Password123!"
  }'
```

### 2. Se Connecter

```bash
curl -X POST http://192.168.64.33/users/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "Password123!"
  }'
```

**Réponse:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

### 3. Utiliser le Token

```bash
curl -X GET http://192.168.64.33/articles/api/articles \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Rôles:**
- **USER**: Créer commandes, voir notifications
- **ADMIN**: Gérer articles, voir toutes commandes

---

## 💡 Exemples d'Utilisation

### Scénario Complet

```bash
# 1. Créer compte USER
curl -X POST http://192.168.64.33/users/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"Alice123!"}'

# 2. Se connecter
TOKEN=$(curl -s -X POST http://192.168.64.33/users/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Alice123!"}' | jq -r '.token')

# 3. Créer compte ADMIN
curl -X POST http://192.168.64.33/users/api/auth/register/admin \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"Admin123!"}'

# 4. Se connecter ADMIN
ADMIN_TOKEN=$(curl -s -X POST http://192.168.64.33/users/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' | jq -r '.token')

# 5. Créer article
curl -X POST http://192.168.64.33/articles/api/articles \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop Dell XPS 15","description":"Haute performance","price":1299.99,"stock":10}'

# 6. Voir articles
curl http://192.168.64.33/articles/api/articles

# 7. Créer commande
curl -X POST http://192.168.64.33/orders/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"articleId":1,"quantity":2}]}'

# 8. Voir mes commandes
curl http://192.168.64.33/orders/api/orders \
  -H "Authorization: Bearer $TOKEN"

# 9. Voir notifications
curl http://192.168.64.33/notifications/api/notifications \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔧 Troubleshooting

### Port 9092 déjà utilisé

```bash
# username le processus
sudo lsof -i :9092

# Arrêter tout
sudo docker compose down

# Supprimer conteneurs orphelins
sudo docker compose up -d --remove-orphans
```

### Services ne démarrent pas

```bash
# Vérifier logs
sudo docker compose logs service_name

# Vérifier PostgreSQL
sudo docker compose ps postgres  # Doit être "healthy"

# Redémarrer
sudo docker compose restart service_name
```

### Erreur "target/quarkus-app/quarkus not found"

**Cause:** Le build Maven n'a pas créé la structure de répertoires attendue pour le fast-jar Quarkus.

**Solutions:**
```bash
# Vérifier que le build Maven a réussi
cd orders_service  # ou autre service
mvn clean package
ls -la target/quarkus-app/

# Si le répertoire n'existe pas, forcer le type de package
mvn clean package -Dquarkus.package.type=fast-jar

# Ou utiliser uber-jar (recommandé pour Docker)
mvn clean package -Dquarkus.package.type=uber-jar

# Vérifier la structure créée
ls -la target/
# Doit contenir: quarkus-app/ avec lib/, app/, quarkus/, *.jar

# Nettoyer et reconstruire si nécessaire
mvn clean
mvn package -Dquarkus.package.type=uber-jar
```

### Swagger UI ne charge pas

```bash
# Tester OpenAPI via Traefik
curl http://192.168.64.33/q/openapi

# Redémarrer API Gateway
docker compose down -v --remove-orphans
docker compose rm -fsv   # optionnel, si tu veux vraiment tout forcer
docker volume prune      # seulement si tu veux aussi nettoyer d'autres volumes inutilisés

docker system prune -a --volumes -f
docker compose down -v --rmi all --remove-orphans

docker image rm kafka_quarkus-api_gateway \
                kafka_quarkus-users_service \
                kafka_quarkus-articles_service \
                kafka_quarkus-orders_service \
                kafka_quarkus-notifications_service

docker compose up -d --build
```

### Token JWT invalide

```bash
# Obtenir nouveau token (expire après 30 min)
curl -X POST http://192.168.64.33/users/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!"}'
```

### Accès depuis machine locale bloqué

**Solution 1: Tunnel SSH (Recommandé)**
```bash
ssh -L 80:localhost:80 -L 8090:localhost:8090 k8s@192.168.64.33
```

**Solution 2: Pare-feu**
```bash
sudo ufw allow 80/tcp
sudo ufw allow 8090/tcp
```

**Solution 3: Mode Bridge VM**
```bash
multipass stop master3
multipass set local.master3.network=bridge
multipass start master3
```

---

## 📊 Configuration

### Ports

| Service | Port | Description |
|---------|------|-------------|
| Traefik | 80 | Reverse Proxy |
| Traefik Dashboard | 8090 | Interface d'administration |
| API Gateway | 9000 | Point d'entrée |
| Users | 8081 | Authentification |
| Articles | 8082 | Catalogue |
| Orders | 8083 | Commandes |
| Notifications | 8084 | Notifications |
| Kafka-1 | 9092 | Broker 1 |
| Kafka-2 | 9093 | Broker 2 |
| Kafka-3 | 9094 | Broker 3 |
| PostgreSQL | 5432 | Database |

### Base de Données

```bash
# Connexion
sudo docker exec -it postgres psql -U appuser -d appdb

# Tables
\dt  # Liste tables
SELECT * FROM users;
SELECT * FROM articles;
SELECT * FROM orders;
SELECT * FROM notifications;
```

### Kafka Topics

```bash
# Liste topics
sudo docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Consommer messages
sudo docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic article.events \
  --from-beginning
```

---

## ⚠️ Limitations Production

**Non production-ready:**
- ❌ Pas de circuit breaker
- ❌ Secrets hardcodés
- ❌ Pas de HTTPS/TLS
- ❌ Single point of failure
- ❌ Pas de monitoring
- ❌ 0% tests

**Pour production:**
- Resilience4j (circuit breaker)
- Secrets Manager (Vault)
- Reverse proxy (Nginx + SSL)
- Kubernetes (HA)
- Prometheus + Grafana
- Tests (80%+ coverage)

---

## 📞 Support

```bash
# Logs
sudo docker compose logs -f
sudo docker compose logs -f service_name
sudo docker compose logs --tail=100 kafka

# Debug
sudo docker exec -it service_name /bin/sh
sudo docker inspect service_name

# Nettoyage
sudo docker system prune -a
sudo docker volume prune

# Swagger UI API Gateway:
http://192.168.64.33/q/swagger-ui/

# OpenAPI Spec:
http://192.168.64.33/q/openapi

# Dashboard Traefik:
http://192.168.64.33:8090

# Services individuels:
http://192.168.64.33:8081/swagger/users/
http://192.168.64.33:8082/swagger/articles/
http://192.168.64.33:8083/swagger/orders/
http://192.168.64.33:8084/swagger/notifications/

```

---
docker volume ls -q | xargs -r docker volume rm

## 📄 License

MIT
