# 📚 Guide d'Accès aux Interfaces Swagger

## 🌟 Interface Unifiée (Recommandé)

### API Gateway - Toutes les APIs en un seul endroit

**URL:** http://localhost:9000/q/swagger-ui

**Avantages:**
- ✅ Un seul point d'accès pour tous les services
- ✅ Menu déroulant pour basculer entre les services
- ✅ Pas besoin de mémoriser plusieurs URLs
- ✅ Interface cohérente et centralisée

**Services disponibles dans le menu déroulant:**
1. **users** - Gestion des utilisateurs et authentification
2. **taskmanager** - Gestion des tâches
3. **articles** - Catalogue de produits
4. **orders** - Gestion des commandes
5. **notifications** - Service de notifications

### Comment utiliser l'interface unifiée

1. Ouvrez votre navigateur à: http://localhost:9000/q/swagger-ui
2. En haut de la page, vous verrez un menu déroulant avec la liste des services
3. Sélectionnez le service que vous souhaitez explorer
4. L'interface Swagger affichera automatiquement toutes les routes de ce service
5. Vous pouvez tester les endpoints directement depuis l'interface

---

## 📋 Interfaces Individuelles

Si vous préférez accéder directement au Swagger UI de chaque service:

| Service | Port | URL Swagger UI | Description |
|---------|------|----------------|-------------|
| **Users Service** | 8081 | http://localhost:8081/q/swagger-ui | Authentification, gestion utilisateurs |
| **Taskmanager** | 8080 | http://localhost:8080/q/swagger-ui | Gestion des tâches |
| **Articles Service** | 8082 | http://localhost:8082/q/swagger-ui | Catalogue produits, stock |
| **Orders Service** | 8083 | http://localhost:8083/q/swagger-ui | Commandes, order items |
| **Notifications Service** | 8084 | http://localhost:8084/q/swagger-ui | Notifications utilisateurs |

---

## 🔗 Endpoints OpenAPI (Format JSON)

### Via API Gateway (Agrégé)

Pour intégration avec des outils externes (Postman, Insomnia, etc.):

```bash
# Users Service
curl http://localhost:9000/openapi/users

# Taskmanager
curl http://localhost:9000/openapi/taskmanager

# Articles Service
curl http://localhost:9000/openapi/articles

# Orders Service
curl http://localhost:9000/openapi/orders

# Notifications Service
curl http://localhost:9000/openapi/notifications
```

### Services Individuels

```bash
# Users Service
curl http://localhost:8081/q/openapi

# Taskmanager
curl http://localhost:8080/q/openapi

# Articles Service
curl http://localhost:8082/q/openapi

# Orders Service
curl http://localhost:8083/q/openapi

# Notifications Service
curl http://localhost:8084/q/openapi
```

---

## 🔐 Authentification dans Swagger UI

### 1. Obtenir un Token JWT

Utilisez l'endpoint de login dans le **Users Service**:

```bash
# Créer un utilisateur (si nécessaire)
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "role": "USER"
  }'

# Se connecter
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 2. Utiliser le Token dans Swagger UI

1. Copiez le token JWT reçu
2. Dans Swagger UI, cliquez sur le bouton **"Authorize"** (🔒) en haut à droite
3. Entrez: `Bearer VOTRE_TOKEN_ICI`
4. Cliquez sur **"Authorize"**
5. Tous les appels suivants incluront automatiquement le token

---

## 🎯 Exemples d'Utilisation

### Scénario 1: Créer un Article (Admin)

1. Allez sur http://localhost:9000/q/swagger-ui
2. Sélectionnez **"articles"** dans le menu déroulant
3. Authentifiez-vous avec un token ADMIN
4. Trouvez `POST /api/articles`
5. Cliquez sur **"Try it out"**
6. Remplissez le JSON:
```json
{
  "name": "Laptop Dell XPS 15",
  "description": "Ordinateur portable haute performance",
  "price": 1299.99,
  "stock": 10
}
```
7. Cliquez sur **"Execute"**

### Scénario 2: Créer une Commande (User)

1. Sélectionnez **"orders"** dans le menu déroulant
2. Authentifiez-vous avec un token USER
3. Trouvez `POST /api/orders`
4. Essayez de créer une commande avec des articles existants

### Scénario 3: Consulter les Notifications (User)

1. Sélectionnez **"notifications"** dans le menu déroulant
2. Authentifiez-vous avec votre token USER
3. Trouvez `GET /api/notifications`
4. Consultez vos notifications

---

## 🛠️ Dépannage

### Le Swagger UI ne charge pas

```bash
# Vérifier que l'API Gateway est démarré
cd /home/k8s/workstations/infra/projets/kafka_quarkus
sudo docker compose ps api_gateway

# Vérifier les logs
sudo docker logs kafka_quarkus-api_gateway-1
```

### Un service n'apparaît pas dans le menu déroulant

```bash
# Vérifier que le service est démarré
sudo docker compose ps

# Tester l'endpoint OpenAPI directement
curl http://localhost:9000/openapi/SERVICE_NAME
```

### Erreur 401 Unauthorized

- Vérifiez que vous avez bien cliqué sur **"Authorize"**
- Vérifiez que le token commence par `Bearer `
- Vérifiez que le token n'a pas expiré (30 minutes par défaut)
- Générez un nouveau token si nécessaire

---

## 📊 Résumé des Ports

| Service | Port | Swagger UI |
|---------|------|------------|
| **API Gateway** | 9000 | ✅ http://localhost:9000/q/swagger-ui |
| Users Service | 8081 | http://localhost:8081/q/swagger-ui |
| Taskmanager | 8080 | http://localhost:8080/q/swagger-ui |
| Articles Service | 8082 | http://localhost:8082/q/swagger-ui |
| Orders Service | 8083 | http://localhost:8083/q/swagger-ui |
| Notifications Service | 8084 | http://localhost:8084/q/swagger-ui |
| PostgreSQL | 5432 | - |
| Kafka | 9092 | - |

---

## 💡 Conseils

1. **Utilisez l'API Gateway** pour une expérience unifiée
2. **Authentifiez-vous une seule fois** - le token sera utilisé pour tous les services
3. **Explorez les schémas** - Swagger UI affiche les modèles de données
4. **Testez directement** - Pas besoin de curl ou Postman pour les tests rapides
5. **Consultez les exemples** - Chaque endpoint a des exemples de requêtes/réponses

---

**Date de création:** 2025-10-13  
**Version:** 1.0  
**Status:** ✅ Opérationnel
