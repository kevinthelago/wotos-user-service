# WoToS User Service

Authentication microservice for the [WoToS](https://github.com/users/kevinthelago/projects/2) system. Handles user registration and JWT-based login using Spring Security. Issues JWTs that can be validated by other services in the stack.

## Prerequisites

- Java 8 (Temurin recommended)
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

## API Endpoints

All endpoints are under `/users` (configured port: `4646`).

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/users/create` | None | Register a new user |
| `POST` | `/users/login` | None | Authenticate and receive a JWT |
| `GET` | `/users/hello` | None | Health check |

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

Include the JWT as a `Bearer` token in the `Authorization` header for protected endpoints.
