# Guide de test — API de Conversion de Devises

> Projet 3 sur 5. Ce guide décrit, pas à pas, comment lancer l'API depuis un
> terminal et vérifier qu'elle répond bien à chaque fonctionnalité demandée par
> le cahier des charges.

## 1. Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Java (JDK) | 17 ou supérieur | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker + Compose | v2 | `docker compose version` |
| curl | — | `curl --version` |

## 2. Ports utilisés

| Service | Port hôte |
|---|---|
| API REST | **8080** |
| PostgreSQL | **5438** |
| Redis | **6380** |

> **Important — les 5 projets utilisent tous le port 8080.**
> Testez-les **un seul à la fois** et arrêtez toujours le précédent
> (`docker compose down -v`) avant de démarrer le suivant.

> Le port PostgreSQL hôte est volontairement décalé : le port 5432 est
> fréquemment occupé par une installation locale de PostgreSQL. Le conteneur de
> l'API, lui, joint la base par `postgres:5432` sur le réseau Docker interne —
> ce décalage ne concerne donc que vos propres connexions depuis la machine.

## 3. Lancement

### Méthode A — tout en Docker (au plus proche du rendu)

```bash
cd currency-api
docker compose up -d --build
```

Le premier build télécharge l'image Maven et les dépendances : comptez
plusieurs minutes. Ensuite :

```bash
docker compose ps                 # les services doivent être "healthy"
docker compose logs -f currency-api  # suivre le démarrage
```

### Méthode B — base en Docker, application en local (itération rapide)

```bash
cd currency-api
docker compose up -d postgres redis

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5438/currency_db \
  SPRING_DATA_REDIS_PORT=6380 \
  mvn spring-boot:run -Dspring-boot.run.fork=false
```

> `-Dspring-boot.run.fork=false` exécute l'application dans le processus Maven :
> un simple **Ctrl+C** l'arrête proprement. Sans cette option, Maven lance une
> JVM fille qui survit à l'arrêt de Maven et garde le port 8080 occupé.
>
> Attention : avec `fork=false`, l'option `-Dspring-boot.run.jvmArguments` est
> ignorée. Les réglages doivent passer par des **variables d'environnement**,
> comme ci-dessus.

### Vérifier que l'API est prête

```bash
curl -s http://localhost:8080/api/actuator/health
# {"status":"UP", ...}
```

## 4. Documentation Swagger (livrable exigé)

Ouvrez dans un navigateur :

    http://localhost:8080/api/swagger-ui.html

Le contrat OpenAPI brut est disponible sur `http://localhost:8080/api/v3/api-docs`.

## 5. Vérification de conformité au cahier des charges

Définissez d'abord l'URL de base :

```bash
B=http://localhost:8080/api
```

### Conversion (devise source, devise cible, montant)

```bash
curl -s -X POST $B/convert -H 'Content-Type: application/json' \
  -d '{"fromCurrency":"EUR","toCurrency":"USD","amount":100}'
```
Attendu : **200 OK** avec le montant converti, le taux appliqué et l'horodatage.
Le taux provient d'un **appel réel** à l'API externe via Spring WebClient.

```bash
# Devise dont le taux peut être un entier
curl -s -X POST $B/convert -H 'Content-Type: application/json' \
  -d '{"fromCurrency":"EUR","toCurrency":"JPY","amount":100}'
```

### Gestion des erreurs (exigée par le cahier des charges)

```bash
# Devise invalide (pas 3 lettres) -> 400
curl -s -X POST $B/convert -H 'Content-Type: application/json' \
  -d '{"fromCurrency":"EURO","toCurrency":"USD","amount":100}'

# Montant négatif -> 400
curl -s -X POST $B/convert -H 'Content-Type: application/json' \
  -d '{"fromCurrency":"EUR","toCurrency":"USD","amount":-5}'

# Devise inconnue du fournisseur -> 404
curl -s -X POST $B/convert -H 'Content-Type: application/json' \
  -d '{"fromCurrency":"EUR","toCurrency":"XYZ","amount":10}'
```

Panne de l'API externe : le service se replie sur le dernier taux connu en base ;
si aucun n'existe, il répond **503**. Pour le simuler :

```bash
APP_EXCHANGE_API_BASE_URL=https://adresse-invalide.example \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5438/currency_db \
SPRING_DATA_REDIS_PORT=6380 mvn spring-boot:run -Dspring-boot.run.fork=false
```

### Taux et historique

```bash
curl -s $B/convert/rates              # tous les taux mémorisés
curl -s $B/convert/rates/EUR          # taux depuis l'euro
curl -s $B/convert/rates/EUR/USD      # taux d'un couple précis
curl -s -X PUT $B/convert/rates/EUR/USD   # forcer un rafraîchissement

curl -s "$B/history?limit=10"         # historique des conversions
curl -s -X DELETE $B/history          # purger l'historique (204)
```

### Vérifier que le cache Redis fonctionne

```bash
docker exec currency-redis redis-cli --scan --pattern 'exchangeRates*'
# exchangeRates::EUR_USD  -> le taux a bien été mis en cache
```

## 6. Tests automatisés

La suite complète s'exécute sans Docker ni réseau (base H2 en mémoire) :

```bash
mvn test
```

Résultat attendu : **26 tests, 0 échec**.

## 7. Arrêt et nettoyage

```bash
# Méthode A
docker compose down -v

# Méthode B : Ctrl+C sur l'application, puis
docker compose down -v
```

`-v` supprime aussi le volume de données : le projet suivant repart d'une base
vierge. **À faire systématiquement avant de tester un autre projet.**

## 8. En cas de problème

| Symptôme | Cause probable | Solution |
|---|---|---|
| `port is already allocated` | Un autre projet tourne encore | `docker compose down -v` dans le projet précédent |
| `Web server failed to start. Port 8080 was already in use` | Application précédente non arrêtée | `ss -ltnp \| grep :8080` puis arrêter le processus |
| `Connection refused` vers la base | Base pas encore prête | `docker compose ps` — attendre l'état `healthy` |
| L'application ignore vos réglages | `jvmArguments` avec `fork=false` | Utiliser des variables d'environnement |
| Swagger renvoie 404 | URL incomplète | Le contexte est `/api` : `/api/swagger-ui.html` |
