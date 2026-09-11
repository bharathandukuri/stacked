<p align="center">
  <img src="client/public/logo.svg" width="96" height="96" alt="Stacked Logo" />
</p>

<h1 align="center">Stacked</h1>

<p align="center">
  A clean, production-ready full-stack application built with <strong>Spring Boot 4</strong> (Java 25) and <strong>React 19</strong> (TanStack Router & Query, Tailwind CSS v4, shadcn/ui).
</p>

---

## Tech Stack

- **Backend**: Java 25, Spring Boot 4.1.1, Spring Data Redis, Spring Data MongoDB, Jackson 3, Dotenv
- **Frontend**: React 19, Vite 8, TanStack Router, TanStack Query, Axios, Tailwind CSS v4, shadcn/ui
- **Testing**: JUnit 5, Mockito, Testcontainers (MongoDB & Redis)
- **Infrastructure**: Docker Compose (MongoDB & Redis)

---

## Quickstart

### 1. Start Databases (Docker Compose)

```bash
docker compose up -d
```

- **MongoDB**: `localhost:27017`
- **Redis**: `localhost:6379`

### 2. Configure Environment

```bash
cp .env.example .env
cp server/.env.example server/.env
```

### 3. Start Backend

```bash
cd server
./mvnw spring-boot:run
```

API runs at `http://localhost:8080`.

### 4. Start Frontend

```bash
cd client
npm install
npm run dev
```

App runs at `http://localhost:3000` (proxies `/api` to the backend).

---

## Testing

### Backend Tests (Unit + Testcontainers)

```bash
cd server

# Run all tests (unit + live Testcontainers integration tests)
./mvnw clean test

# Run only unit tests
./mvnw test -Dtest=RedisServiceTest

# Run only integration tests
./mvnw test -Dtest=RedisServiceIntegrationTest,MongoIntegrationTest,StackedApplicationIntegrationTest
```

### Frontend Checks

```bash
cd client
npm run lint         # ESLint (0 warnings/errors)
npm run typecheck    # TypeScript compiler check
npm run build        # Production Vite build
```

---

## Project Layout

```
Stacked/
├── client/              # React 19 + TanStack Router/Query frontend
├── server/              # Spring Boot 4.1.1 backend
│   ├── src/main/java/   # API response, config, exceptions, services
│   └── src/test/java/   # Unit tests & Testcontainers integration tests
├── docker-compose.yaml  # Redis & MongoDB containers
├── .env.example         # Environment template
└── README.md
```
