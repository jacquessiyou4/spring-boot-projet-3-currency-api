# Currency API — Conversion de Devises

API Spring Boot 3 / Java 17 qui convertit une somme d'une devise vers une autre en utilisant des taux de change récupérés dynamiquement (API externe via **Spring WebClient**).

> 📘 **Procédure de test pas à pas :** voir [GUIDE_DE_TEST.md](GUIDE_DE_TEST.md)
> — lancement depuis le terminal et vérification de chaque fonctionnalité
> exigée par le cahier des charges.

> 🔗 **Lien du dépôt GitHub :** voir [LIEN_GITHUB.md](LIEN_GITHUB.md)

## Prérequis
- Java 17, Maven 3.9+
- PostgreSQL 15 + Redis 7 (ou les conteneurs fournis par Docker Compose)
- Une clé/URL d'API externe de taux de change (configurable)

## Configuration de l'API externe
L'appel se fait via `WebClient` vers une API de taux (`/v4/latest/{currency}`).
Pour pointer vers un instant de test local, vous pouvez définir la base de l'URL dans la configuration WebClient (voir `WebClientConfig`) et fournir une réponse factice.

## Démarrage rapide (Docker)
```bash
cd currency-api
docker-compose up -d
# Swagger UI : http://localhost:8080/api/swagger-ui.html
```

## Démarrage local
```bash
mvn spring-boot:run
```

## Comment tester l'API
1. **Convertir** — `POST /api/convert`
   ```json
   { "fromCurrency": "EUR", "toCurrency": "USD", "amount": 100.0 }
   ```
   → `200 OK` avec `{ "fromCurrency": "EUR", "toCurrency": "USD", "amountFrom": 100.0, "amountTo": ..., "rate": ..., "source": "API" }`.

2. **Taux / historique**
   - `GET /api/convert/rates`, `GET /api/convert/rates/{from}`, `GET /api/convert/rates/{from}/{to}`
   - `PUT /api/convert/rates/{from}/{to}` — rafraîchir depuis l'API externe
   - `GET /api/history?limit=10` — historique des conversions
   - `DELETE /api/history` — vider l'historique

## Gestion des erreurs
- Code devise invalide (différent de 3 lettres, dans le corps **ou** dans l'URL) → `400`.
- Erreur de validation (montant absent ou négatif) → `400`.
- Taux introuvable (devise inconnue du fournisseur et absente du cache) → `404`.
- Panne de l'API externe : le service **replie** d'abord sur la dernière valeur en base ; si elle n'existe pas, `503` (fournisseur indisponible).

## Cache
Les taux sont mis en cache dans Redis (`exchangeRates`, TTL 24 h) par `ExchangeRateService`, un bean distinct : une méthode `@Cacheable` appelée depuis le même bean ne passe pas par le proxy Spring et ne serait jamais mise en cache.
L'URL du fournisseur est configurable via `APP_EXCHANGE_API_BASE_URL`.

## Tests (sans appels réseau)
Les tests mockent l'API externe → la suite passe **sans clé ni réseau** :
```bash
mvn test
```

## Schéma de base de données

Le schéma est versionné avec **Flyway** : les scripts se trouvent dans
`src/main/resources/db/migration` et sont appliqués automatiquement au
démarrage, dans l'ordre des versions.

`spring.jpa.hibernate.ddl-auto` est réglé sur **`validate`** : Hibernate vérifie
que les entités correspondent au schéma migré, sans jamais le modifier lui-même.
Toute divergence fait échouer le démarrage, au lieu de passer inaperçue.

Pour faire évoluer le schéma, ajoutez un nouveau fichier (`V2__...sql`) — ne
modifiez jamais une migration déjà appliquée.

Les tests s'exécutent sur H2 en mémoire, avec Flyway désactivé et un schéma
généré depuis les entités (`create-drop`).
