# WoToS User Service

Authentication microservice for the [WoToS](https://github.com/users/kevinthelago/projects/2) system. Handles user registration and JWT-based login using Spring Security. Issues JWTs that can be validated by other services in the stack.

## Prerequisites

- Java 17 (Temurin recommended)
- Maven or the included `./mvnw` wrapper
- MySQL 8 running at `localhost:3306`, user `root`, password `root`
- Database `wotos_users_database` (created automatically by Hibernate on first run)
- `wotos-eureka-server` running (service registry)
- `wotos-config-server` running at `localhost:4040`

## Running Locally

### Command Line

```bash
./mvnw spring-boot:run
```

### IntelliJ

1. Open the project root in IntelliJ IDEA.
2. Run `WotosUserServiceApplication` — no additional environment variables required beyond a running MySQL instance.

## Building

```bash
./mvnw clean package        # build JAR, skip tests
./mvnw clean install        # build JAR + run all tests
```

## Container

The service ships as a multi-stage image (JDK 17 build → JRE 17 runtime, run as a
non-root user). CI publishes it to GHCR on every push to `develop`.

```bash
docker build -t ghcr.io/kevinthelago/wotos-user-service:dev .
docker run --rm -p 4646:4646 \
  -e JWT_PRIVATE_KEY_PEM="$(cat dev-private-key.pem)" \
  ghcr.io/kevinthelago/wotos-user-service:dev
```

The published image is `ghcr.io/kevinthelago/wotos-user-service:dev`.

## API Endpoints

Configured port: `4646`.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/users/create` | None | Register a new user |
| `POST` | `/users/login` | None | Authenticate and receive a JWT |
| `GET` | `/users/hello` | None | Health check |
| `GET` | `/.well-known/jwks.json` | None | RS256 public verification key (JWK set) |
| `GET` | `/users/me/preferences` | JWT | Read the authenticated user's preferences |
| `PUT` | `/users/me/preferences` | JWT | Replace the authenticated user's preferences |

### Registration

`POST /users/create` takes `{ "username", "password" }`. The `roles` and
`active` fields are set server-side (new users are always created as a normal
`user`), and the password is BCrypt-encoded (cost factor 12) before storage.

Validation rules:

- `username` — 3–32 characters, unique
- `password` — at least 10 characters

Failures return the standard error envelope:

```json
{ "error": { "code": "validation_error", "message": "password must be at least 10 characters" } }
```

| Status | `error.code`       | When                              |
|--------|--------------------|-----------------------------------|
| `400`  | `validation_error` | username/password fail the rules  |
| `409`  | `username_taken`   | the username already exists       |

### Login request body

```json
{
  "username": "string",
  "password": "string"
}
```

### Login response

```json
{
  "jwt": "eyJ..."
}
```

`401 Unauthorized` is returned for an unknown username or a wrong password (the
two are not distinguished, to avoid user enumeration).

Include the JWT as a `Bearer` token in the `Authorization` header for protected
endpoints:

```
Authorization: Bearer eyJ...
```

## Authentication & JWT

Tokens are **RS256**-signed. The 2048-bit RSA signing key is loaded from the
`JWT_PRIVATE_KEY_PEM` environment variable (an unencrypted **PKCS#8** PEM,
`-----BEGIN PRIVATE KEY-----`), supplied as a Spring Cloud Config `{cipher}`
value in production. **If no key is configured, an ephemeral key pair is
generated at startup** — fine for local dev, tests, and a single-instance
`docker compose up`, but configure a stable key for any multi-instance
deployment, since each instance would otherwise sign with a different key.

### JWT claims (contract for downstream validators)

| Claim   | Type           | Notes                                            |
|---------|----------------|--------------------------------------------------|
| `sub`   | string         | Username                                         |
| `iat`   | epoch seconds  | Issued-at                                        |
| `exp`   | epoch seconds  | Expiry — 30 minutes after `iat` by default       |
| `roles` | array<string>  | Authorities, e.g. `["user"]`                     |

The JWS header carries `alg: RS256` and a `kid` matching the published JWK.

### JWKS endpoint

`GET /.well-known/jwks.json` (no auth) returns the public verification key as an
RFC 7517 JWK set. Downstream services (e.g. the edge gateway) fetch this to
validate tokens without sharing a secret. The `kid` in the set matches the `kid`
in each issued token's header.

To rotate the key, replace `JWT_PRIVATE_KEY_PEM` and restart; the JWKS endpoint
then publishes the new public key (and a new `kid`) for validators to pick up.

## User preferences

`GET`/`PUT /users/me/preferences` read and replace the authenticated user's
preferences. Both require a valid `Bearer` JWT; the owning user is taken from
the token, never the request body. `PUT` is a full replacement.

```json
{
  "darkTheme": true,
  "savedNicknames": ["Tankzilla", "BushWookie"],
  "lastGarageVehicleId": 42
}
```

- `darkTheme` — boolean
- `savedNicknames` — array of strings, **at most 25** (exceeding it returns a
  `400` `validation_error` envelope)
- `lastGarageVehicleId` — number or `null`

A user who has never saved preferences reads the defaults
(`darkTheme: false`, empty `savedNicknames`, `lastGarageVehicleId: null`).
