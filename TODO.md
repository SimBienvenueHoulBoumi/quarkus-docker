# Plan de Développement - Système E-Commerce Microservices

## Architecture Globale

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  TaskManager    │     │  Users Service   │     │ Articles Service│
│   (Port 8080)   │────▶│   (Port 8081)    │     │   (Port 8082)   │
└─────────────────┘     └────────┬─────────┘     └────────┬────────┘
                                 │                         │
                                 │                         │
                                 ▼                         ▼
                        ┌─────────────────┐      ┌─────────────────┐
                        │     Kafka       │◀─────│  Orders Service │
                        │   (Port 9092)   │      │   (Port 8083)   │
                        └────────┬────────┘      └─────────────────┘
                                 │                         
                                 ▼                         
                        ┌─────────────────┐      
                        │  Notifications  │      
                        │   (Port 8084)   │      
                        └─────────────────┘      
                                 │
                                 ▼
                        ┌─────────────────┐
                        │   PostgreSQL    │
                        │   (Port 5432)   │
                        └─────────────────┘
```

## Services à Créer

### ✅ Services Existants
- [x] users_service (Port 8081)
- [x] taskmanager (Port 8080)

### 🔨 Nouveaux Services à Créer

#### 1. articles_service (Port 8082)
**Responsabilités:**
- Gestion du catalogue de produits
- Gestion des stocks
- Accessible uniquement aux ADMIN pour CRUD
- Publier événements Kafka

**Entités:**
- Article (id, name, description, price, stock, category, createdAt, updatedAt)

**Endpoints:**
- POST /api/articles - Créer article (ADMIN)
- GET /api/articles - Lister tous les articles (PUBLIC)
- GET /api/articles/{id} - Détails article (PUBLIC)
- PUT /api/articles/{id} - Modifier article (ADMIN)
- DELETE /api/articles/{id} - Supprimer article (ADMIN)
- PATCH /api/articles/{id}/stock - Mettre à jour stock (ADMIN)

**Événements Kafka (Producer):**
- article.created
- article.updated
- article.deleted
- article.stock.changed
- article.stock.low (quand stock < 10)

---

#### 2. orders_service (Port 8083)
**Responsabilités:**
- Gestion des commandes utilisateurs
- Vérification de stock avant commande
- Calcul du total
- Historique des commandes

**Entités:**
- Order (id, userId, totalAmount, status, createdAt)
- OrderItem (id, orderId, articleId, articleName, quantity, unitPrice, subtotal)
- OrderStatus: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

**Endpoints:**
- POST /api/orders - Créer commande (USER)
- GET /api/orders - Mes commandes (USER)
- GET /api/orders/{id} - Détails commande (USER/ADMIN)
- GET /api/orders/all - Toutes les commandes (ADMIN)
- PATCH /api/orders/{id}/status - Changer statut (ADMIN)

**Événements Kafka:**
- **Producer:**
  - order.created
  - order.status.changed
  - order.cancelled
- **Consumer:**
  - article.stock.changed (pour validation)

---

#### 3. notifications_service (Port 8084)
**Responsabilités:**
- Écouter les événements Kafka
- Créer des notifications pour les utilisateurs
- Gérer l'état des notifications (lu/non lu)

**Entités:**
- Notification (id, userId, type, title, message, read, createdAt)
- NotificationType: ORDER_CREATED, ORDER_STATUS_CHANGED, STOCK_LOW, ARTICLE_CREATED

**Endpoints:**
- GET /api/notifications - Mes notifications (USER)
- GET /api/notifications/unread - Notifications non lues (USER)
- PATCH /api/notifications/{id}/read - Marquer comme lu (USER)
- DELETE /api/notifications/{id} - Supprimer notification (USER)

**Événements Kafka (Consumer):**
- order.created → Notification "Commande créée"
- order.status.changed → Notification "Statut commande changé"
- article.created → Notification pour ADMIN
- article.stock.low → Notification pour ADMIN

---

## Configuration Technique

### Base de Données PostgreSQL
```sql
-- Schémas séparés
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS articles;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS notifications;
```

### Topics Kafka
- article.events
- order.events
- notification.events

### JWT Configuration
- Secret partagé: `super-secret-change-me-please-change-me-32-bytes`
- Issuer: `users-service`
- Roles: USER, ADMIN

### Docker Compose
Ajouter les 3 nouveaux services avec:
- Build context
- Dépendances (postgres, kafka)
- Variables d'environnement
- Ports exposés

---

## Étapes d'Implémentation

### Phase 1: articles_service ✅
- [ ] Créer structure Maven
- [ ] Configuration (pom.xml, application.yaml)
- [ ] Entité Article
- [ ] Repository
- [ ] Service layer
- [ ] Kafka Producer
- [ ] REST Controllers
- [ ] Swagger UI
- [ ] Dockerfile

### Phase 2: orders_service
- [ ] Créer structure Maven
- [ ] Configuration
- [ ] Entités (Order, OrderItem)
- [ ] Repository
- [ ] Service layer (avec validation stock)
- [ ] Kafka Producer/Consumer
- [ ] REST Controllers
- [ ] Swagger UI
- [ ] Dockerfile

### Phase 3: notifications_service
- [ ] Créer structure Maven
- [ ] Configuration
- [ ] Entité Notification
- [ ] Repository
- [ ] Service layer
- [ ] Kafka Consumer
- [ ] REST Controllers
- [ ] Swagger UI
- [ ] Dockerfile

### Phase 4: Intégration
- [ ] Mettre à jour docker-compose.yml
- [ ] Créer script d'initialisation DB
- [ ] Tests d'intégration
- [ ] Documentation API complète
- [ ] Guide de démarrage

---

## Flux de Données Exemple

### Scénario: Achat d'un article

1. **Admin crée un article**
   ```
   POST /api/articles (articles_service)
   → Article créé en DB
   → Kafka: article.created event
   → notifications_service écoute → Notification pour ADMIN
   ```

2. **User consulte les articles**
   ```
   GET /api/articles (articles_service)
   → Liste des articles disponibles
   ```

3. **User passe commande**
   ```
   POST /api/orders (orders_service)
   → Vérification stock via articles_service
   → Création Order + OrderItems
   → Kafka: order.created event
   → notifications_service écoute → Notification pour USER
   → articles_service met à jour stock
   → Kafka: article.stock.changed event
   ```

4. **Admin change statut commande**
   ```
   PATCH /api/orders/{id}/status (orders_service)
   → Mise à jour statut
   → Kafka: order.status.changed event
   → notifications_service écoute → Notification pour USER
   ```

---

## Prochaines Étapes

1. Commencer par articles_service (le plus simple)
2. Puis orders_service (dépend de articles)
3. Enfin notifications_service (écoute les autres)
4. Intégration et tests

**Estimation:** ~3-4 heures de développement pour les 3 services
