# 🚀 E-Commerce Microservices - Kafka Quarkus

Avant de démarrer : quand et comment utiliser un POM central (parent)

## 🧭 Quand et comment utiliser un POM central (parent)

Si vous gérez plusieurs modules Quarkus dans le même dépôt (users_service, articles_service, orders_service, notifications_service, etc.), un POM parent (packaging = "pom") est très utile pour :

- Centraliser les versions (java, quarkus, dépendances communes) via <dependencyManagement> et propriétés.
- Centraliser les plugins Maven (build, surefire, jacoco, spotbugs) via <pluginManagement> pour éviter de dupliquer la configuration.
- Partager des propriétés et des profiles (ex. quarkus.package.type, java.version, versions de librairies).
- Commander la construction multi-modules depuis la racine : `mvn -T1C clean package` (ou `mvn -T1C -pl . -am package`).

Exemple minimal de `pom.xml` parent (à placer à la racine) :

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>kafka-quarkus-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>17</java.version>
        <quarkus.version>3.0.0</quarkus.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Dépendances communes avec versions ici -->
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- Plugins partagés ici (maven-compiler-plugin, quarkus-maven-plugin, etc.) -->
            </plugins>
        </pluginManagement>
    </build>

    <modules>
        <module>users_service</module>
        <module>articles_service</module>
        <module>orders_service</module>
        <module>notifications_service</module>
        <!-- ajouter les autres modules si nécessaire -->
    </modules>
</project>
```

Bonnes pratiques :
- Gardez le `dependencyManagement` pour fixer les versions, mais déclarez les dépendances dans chaque module normalement.
- Utilisez `pluginManagement` pour unifier la configuration des plugins tout en permettant des ajustements module par module.
- Versionnez le parent pour reproduire un état précis des modules.

Important — base de données et Docker

La compilation Maven et la création des artefacts (target/quarkus-app) ne nécessitent pas que Docker soit en cours d'exécution. En revanche, pour exécuter les services Quarkus (via Docker Compose) et qu'ils puissent se connecter à PostgreSQL et à Kafka, vous devez démarrer l'infrastructure Docker (Postgres, Kafka, Traefik, etc.).

Flux recommandé :

1. Construire les services (local build obligatoire avant de lancer les conteneurs qui s'attendent aux artéfacts) :

```bash
cd /chemin/vers/kafka_quarkus
mvn -T1C clean package -Dquarkus.package.type=uber-jar
```

2. Démarrer uniquement l'infrastructure requise (si vous ne souhaitez pas tout rebuild) :

```bash
# Démarrer Postgres et Kafka (noms des services selon le docker-compose)
sudo docker compose up -d postgres kafka
```

3. Démarrer l'ensemble (recommandé si vous modifiez les services et voulez rebuild les images) :

```bash
sudo docker compose up -d --build
```

4. Vérifier que les services peuvent atteindre la base de données avant d'exécuter des scénarios ou des tests d'intégration :

```bash
sudo docker compose ps
sudo docker compose logs -f postgres
```

Remarque : si vous souhaitez exécuter les services sans Docker (par exemple en pointant vers une base PostgreSQL distante), adaptez les variables d'environnement (URL, utilisateur, mot de passe) de chaque service et assurez-vous que la DB/Kafka externe est accessible depuis votre machine de développement.

---

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
mvn clean package 

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
## 🔐 Utilisation du script `scripts/run-scenario.sh`

La suite ci-dessous remplace les exemples précédents et explique comment utiliser le script `scripts/run-scenario.sh` fourni dans ce dépôt. Ce script exécute un scénario de bout en bout (création d'utilisateurs, article, commandes, mise à jour des statuts et vérification des notifications) pour tester l'intégration des microservices.

Pré-requis
- Le projet doit être construit (Quarkus services) : `mvn clean package` pour chaque service ou au niveau racine si un parent gère les modules.
- Docker et Docker Compose installés sur la machine hôte.
- (Optionnel) `jq` installé pour extraire des champs JSON dans les exemples.

Emplacement
- Script : `./scripts/run-scenario.sh`

Usage simple

```bash
# Se placer à la racine du repo
cd /path/to/kafka_quarkus

# Construire les services Quarkus (si nécessaire)
# cd dans chaque module ou exécuter mvn depuis la racine si configuré
mvn -T1C clean package

# Démarrer l'infrastructure (Traefik, Kafka, Postgres, API Gateway, services)
sudo docker compose up -d --build

# Lancer le scénario automatisé
./scripts/run-scenario.sh
```

Ce script attend que l'API Gateway et les services soient joignables, puis exécute le scénario complet. La sortie affiche chaque étape et les IDs créés (users, article, orders, notifications).

Options courantes
- Exécuter en mode verbeux :

```bash
VERBOSE=1 ./scripts/run-scenario.sh
```

- Forcer la récréation des comptes (si le script supporte une variable) :

```bash
FORCE=true ./scripts/run-scenario.sh
```

(Remarque : ces variables sont des exemples — vérifiez le début du script `scripts/run-scenario.sh` pour les variables d'environnement supportées.)

Exemple d'utilisation avancée (reconstruire puis tester)

```bash
# Rebuild & run scenario in one command
mvn -T1C clean package && sudo docker compose up -d --build && ./scripts/run-scenario.sh | tee run-scenario.log
```

Que vérifie le script ?
- Création et authentification d'un compte ADMIN et d'un compte USER
- Création d'un article par l'ADMIN
- Création d'une commande par l'USER
- Progression des statuts d'une commande (PENDING → CONFIRMED → SHIPPED → DELIVERED)
- Réception des notifications correspondantes et opérations de lecture
- Annulation et gestion des stocks

Sortie et indicateurs importants
- Recherchez dans la sortie : `token acquired`, `article id:`, `order id:`, `notification received`.
- Le script affiche le nombre de notifications non lues et marque des notifications comme lues pour tester le service `notifications`.

Dépannage rapide
- Si le script bloque sur "Waiting for API Gateway" : vérifiez que Traefik et l'API Gateway sont accessibles (cf. `http://<host>/q/swagger-ui`).
- Si un service Quarkus renvoie "target/quarkus-app not found" : reconstruisez le service avec `mvn package -Dquarkus.package.type=uber-jar`.
- Pour voir les logs des services :

```bash
sudo docker compose logs -f api_gateway
sudo docker compose logs -f users_service
sudo docker compose logs -f notifications_service
```

Adaptations possibles
- Intégrer le script dans une pipeline CI pour exécuter un smoke-test après un déploiement.
- Exposer des options supplémentaires au script (timeout, retry counts, base URL) si vous en avez besoin — contributions bienvenues.

Sécurité
- Les routes de création d'administrateur ne doivent pas rester ouvertes en production. Utilisez des mécanismes sécurisés pour initialiser les administrateurs (secrets, scripts de migration ou tokens d'activation).

Fin

Cette section remplace les exemples précédents et est dédiée à l'utilisation et au dépannage du script `scripts/run-scenario.sh`.

## 📄 License

MIT
