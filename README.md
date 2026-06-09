<div align="center">
  <h1>🔗 URL Shortener</h1>
  <p><strong>A production-grade URL shortening service built with Spring Boot 3.2, featuring real-time analytics, Redis caching, JWT authentication, QR code generation, and rate limiting.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=flat&logo=springboot&logoColor=white" alt="Spring Boot 3.2">
    <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white" alt="MySQL 8.0">
    <img src="https://img.shields.io/badge/Redis-7-FF4438?style=flat&logo=redis&logoColor=white" alt="Redis 7">
    <img src="https://img.shields.io/badge/JWT-HS256-000000?style=flat&logo=jsonwebtokens&logoColor=white" alt="JWT HS256">
    <img src="https://img.shields.io/badge/Docker-✓-2496ED?style=flat&logo=docker&logoColor=white" alt="Docker">
    <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat" alt="License">
    <a href="https://url-shortener-production-a9f6.up.railway.app/app/index.html"><img src="https://img.shields.io/badge/Live Demo-Railway-38BDF8?style=flat&logo=railway&logoColor=white" alt="Live Demo"></a>
  </p>

  <br>
  <p><em>Developed by <a href="https://www.linkedin.com/in/chetanamahajan">Chetana Mahajan</a></em></p>
  <p>
    <a href="https://www.linkedin.com/in/chetanamahajan"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white" alt="LinkedIn"></a>
    <a href="https://github.com/chetana987"><img src="https://img.shields.io/badge/GitHub-100000?style=flat&logo=github&logoColor=white" alt="GitHub"></a>
    <a href="https://url-shortener-production-a9f6.up.railway.app/app/index.html"><img src="https://img.shields.io/badge/▶️ Live Demo-Click Here!-00C853?style=flat" alt="Live Demo"></a>
  </p>
  </p>
</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Redis Caching Strategy](#-redis-caching-strategy)
- [JWT Authentication Flow](#-jwt-authentication-flow)
- [Rate Limiting](#-rate-limiting)
- [Docker Setup](#-docker-setup)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [Future Improvements](#-future-improvements)

---

## 📖 Overview

> **🚀 Live Demo:** [url-shortener-production-a9f6.up.railway.app](https://url-shortener-production-a9f6.up.railway.app/app/index.html)

The **URL Shortener** is a full-stack web application that transforms long URLs into short, manageable links. It provides real-time click analytics, QR code generation, and secure user authentication — all powered by a high-performance Spring Boot backend with Redis caching.

### Why This Project?

This project demonstrates production-grade Spring Boot development with:

- **Security-first design**: JWT-based stateless authentication with refresh token rotation
- **Performance at scale**: Redis cache-aside pattern with sub-millisecond redirect resolution
- **Observability**: Comprehensive click analytics with browser, geographic, and temporal breakdowns
- **Developer UX**: Interactive Swagger UI, Docker Compose for one-command setup
- **Production readiness**: Rate limiting, input validation, global exception handling, HikariCP pooling

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        Client Browser                           │
│            (HTML/CSS/JS served from Spring Boot static)          │
└──────────────────┬──────────────┬───────────────────────────────┘
                   │              │
        ┌──────────▼──────┐  ┌───▼───────────┐
        │  Public Routes  │  │  Protected     │
        │  (no JWT)       │  │  Routes (JWT)  │
        └────────┬────────┘  └───────┬────────┘
                 │                   │
┌────────────────▼───────────────────▼─────────────────────────────┐
│                    Nginx / Spring Security                        │
│         RateLimitingFilter → JwtAuthenticationFilter              │
│                      (Filter Chain)                               │
└──────────────────────────────┬───────────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────────┐
│                     Spring Boot Application                       │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐   │
│  │ Controller│→│  Service  │→│  Repository│→│   MySQL 8.0    │   │
│  │   Layer   │  │   Layer  │  │   Layer   │  │   Database     │   │
│  └──────────┘  └────┬─────┘  └──────────┘  └────────────────┘   │
│                     │                                             │
│                    ┌▼─────┐                                       │
│                    │Redis │                                       │
│                    │Cache │                                       │
│                    └──────┘                                       │
└──────────────────────────────────────────────────────────────────┘
```

### Request Flow

```
User submits URL → POST /api/v1/urls
    ↓
RateLimitingFilter checks Redis (sliding window) → 429 if exceeded
    ↓
JwtAuthenticationFilter extracts Bearer token (if present)
    ↓
UrlController → UrlManagementService
    ↓
ShortCodeGenerator creates unique 7-char Base62 code
    ↓
UrlMappingRepository saves to MySQL
    ↓
RedisCacheService warms cache: url:{code} + redirect:{code}
    ↓
Response: { shortUrl: "http://localhost:8090/abc1234", ... }

User clicks short URL → GET /{shortCode}
    ↓
(No auth required) → RateLimitingFilter
    ↓
UrlRedirectService checks Redis cache first
    ↓
Cache hit? → Return original URL immediately (~1ms)
Cache miss? → Query MySQL → Warm Redis → Return URL
    ↓
AnalyticsService tracks click asynchronously:
    - Parse User-Agent (browser, OS, device type)
    - Record IP, country, referer
    - Increment clickCount in MySQL
    ↓
301 Redirect to original URL
```

---

## 🛠 Tech Stack

### Backend

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Primary language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Security | 6.2.0 | Authentication & authorization |
| Spring Data JPA | 3.2.0 | ORM & database access |
| Spring Data Redis | 3.2.0 | Redis integration |
| Hibernate ORM | 6.3.1 | JPA implementation |
| MySQL Connector/J | 8.1.0 | MySQL JDBC driver |
| HikariCP | 5.0.1 | Connection pooling |
| JJWT | 0.12.3 | JWT token generation & validation |
| ZXing | 3.5.3 | QR code generation |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI & API docs |
| Lombok | 1.18.30 | Boilerplate reduction |
| Jackson | 2.15.3 | JSON serialization |
| Lettuce | 6.3.0 | Redis client (reactive) |

### Frontend

| Technology | Purpose |
|-----------|---------|
| HTML5 | Structure |
| CSS3 (custom) | Styling with CSS variables & gradients |
| Bootstrap 5.3 | Responsive layout & components |
| Font Awesome 6 | Icons |
| Vanilla JavaScript | DOM manipulation, API calls, auth |

### Infrastructure

| Tool | Purpose |
|------|---------|
| Maven 3.9 | Build tool |
| Docker | Containerization |
| Docker Compose | Multi-service orchestration |
| Git | Version control |

---

## ✨ Features

### Core
- **URL Shortening**: Create short URLs with optional custom aliases and expiration dates
- **Batch Creation**: Create up to 100 short URLs in a single request
- **Smart Redirect**: 301 redirect with sub-millisecond Redis cache resolution
- **Expiry Management**: Automatic deactivation of expired URLs, manual extend/reactivate

### Authentication & Security
- **JWT-based Auth**: Stateless access tokens (HS256) with refresh token rotation
- **Secure Password Storage**: BCrypt hashing
- **User Data Isolation**: Each user sees only their own URLs
- **Account Management**: Profile update, password change, account lock/disable

### Analytics
- **Click Tracking**: Every redirect is logged with full request metadata
- **Browser Stats**: Breakdown by browser name, version, OS, device type
- **Geographic Stats**: Country-level click distribution
- **Referer Tracking**: Top referring websites
- **Temporal Stats**: Daily and hourly click distribution
- **Unique Visitors**: Distinct IP-based visitor counting

### Caching
- **Redis Cache-Aside**: URL mappings and redirect targets cached for sub-millisecond lookups
- **QR Code Caching**: Generated QR codes cached in Redis for 24 hours
- **Cache Invalidation**: Automatic cache refresh on URL updates, bulk invalidation on deactivation

### Rate Limiting
- **Sliding Window**: Redis sorted set + Lua script for atomic rate limiting
- **Tiered Limits**: Different limits for general, URL creation, and auth endpoints
- **Informative Headers**: `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`

### QR Codes
- **Automatic Generation**: Every short URL gets a QR code
- **Logo Overlay**: Project branding on generated QR codes
- **High Performance**: Cached in Redis, generated once per URL

### API & Developer Experience
- **Interactive Swagger UI**: `/swagger-ui.html` with "Try it out" support
- **Comprehensive Documentation**: OpenAPI 3.0 spec at `/v3/api-docs`
- **Standardized Responses**: Consistent `ApiResponse<T>` wrapper with pagination support
- **Validation**: Bean Validation with meaningful error messages
- **Global Exception Handling**: Consistent error response format across all endpoints

### Frontend
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Auto-Refresh**: Silent JWT token refresh before expiry
- **Toast Notifications**: Real-time feedback for all actions
- **QR Code Display**: Visual QR code for every shortened URL
- **Clipboard Copy**: One-click copy for short URLs

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (or Docker)
- **MySQL 8.0+** running on `localhost:3306`
- **Redis 7+** running on `localhost:6379`
- **Maven 3.9+** (optional, if using Docker)

### Option 1: Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/chetana987/url-shortener.git
cd url-shortener

# Start all services
docker-compose up --build

# Access the application
open http://localhost:8080
```

This starts MySQL 8.0, Redis 7, and the application in Docker containers with a shared network. The database initializes automatically with `schema.sql`.

### Option 2: Local Development

```bash
# 1. Create MySQL database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS url_shortener;"

# 2. Build the application
mvn clean package -DskipTests

# 3. Run the application
java -jar target/url-shortener-1.0.0.jar

# 4. Access the application
open http://localhost:8090
```

### Configure MySQL Credentials

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    username: root          # Your MySQL username
    password: yourpassword  # Your MySQL password
```

### Verify Installation

```bash
# Health check
curl http://localhost:8090/api/v1/health

# Expected response:
{"success":true,"message":"Service is healthy","data":{"status":"UP","timestamp":"...","version":"1.0.0"}}
```

---

## 📚 API Documentation

Interactive API documentation is available at **`http://localhost:8090/swagger-ui.html`** when the application is running. Below is a summary of all endpoints.

### Public Endpoints (No Authentication)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/health` | Health check |
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh JWT token |
| GET | `/{shortCode}` | Redirect to original URL |
| GET | `/api/v1/qr/{shortCode}` | Get QR code image |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/v3/api-docs` | OpenAPI spec |
| GET | `/` | Home page |
| GET | `/app/**` | Static pages (login, signup, dashboard) |

### Protected Endpoints (JWT Required)

**Authentication**
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/auth/me` | Get current user profile |
| PUT | `/api/v1/auth/profile` | Update profile |
| POST | `/api/v1/auth/change-password` | Change password |
| POST | `/api/v1/auth/logout` | Logout |

**URL Management**
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/urls` | Create short URL |
| POST | `/api/v1/urls/batch` | Batch create URLs |
| GET | `/api/v1/urls` | List active URLs |
| GET | `/api/v1/urls/inactive` | List inactive URLs |
| GET | `/api/v1/urls/expired` | List expired URLs |
| GET | `/api/v1/urls/{shortCode}` | Get URL details |
| PUT | `/api/v1/urls/{shortCode}` | Update URL |
| DELETE | `/api/v1/urls/{shortCode}` | Deactivate URL |
| POST | `/api/v1/urls/{shortCode}/activate` | Reactivate URL |
| POST | `/api/v1/urls/{shortCode}/extend` | Extend expiration |

**Analytics**
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/analytics/urls/{shortCode}` | Aggregated statistics |
| GET | `/api/v1/analytics/urls/{shortCode}/clicks` | Click history |
| GET | `/api/v1/analytics/urls/{shortCode}/clicks/range` | Click history by date range |
| GET | `/api/v1/analytics/urls/{shortCode}/total-clicks` | Total click count |
| GET | `/api/v1/analytics/urls/{shortCode}/unique-visitors` | Unique visitors |
| GET | `/api/v1/analytics/urls/{shortCode}/browsers` | Browser breakdown |
| GET | `/api/v1/analytics/urls/{shortCode}/countries` | Country breakdown |
| GET | `/api/v1/analytics/urls/{shortCode}/referers` | Referer breakdown |
| GET | `/api/v1/analytics/urls/{shortCode}/daily` | Daily statistics |
| GET | `/api/v1/analytics/urls/{shortCode}/hourly` | Hourly distribution |

### API Response Format

All endpoints return a standardized response:

```json
{
  "success": true,
  "message": "Short URL created successfully",
  "data": {
    "id": "a1b2c3d4-...",
    "shortCode": "abc1234",
    "shortUrl": "http://localhost:8090/abc1234",
    "originalUrl": "https://example.com/very-long-url",
    "clickCount": 0,
    "active": true,
    "createdAt": "2024-01-15T10:30:00",
    "expiryDate": null,
    "isExpired": false,
    "remainingDays": null
  },
  "timestamp": "2024-01-15T10:30:00.123Z"
}
```

Paginated responses include:

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20,
    ...
  },
  "page": 0,
  "size": 20,
  "total": 42
}
```

---

## 🗄 Database Schema

### Entity Relationship Diagram

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│    User      │     │   UrlMapping     │     │   Analytics      │
├──────────────┤     ├──────────────────┤     ├──────────────────┤
│ id (UUID) ◄──┼──┐  │ id (UUID)        │     │ id (BIGINT)      │
│ email (UNQ)  │  │  │ shortCode (UNQ)  │     │ urlId ──────┐   │
│ password     │  │  │ originalUrl      │     │ shortCode   │   │
│ firstName    │  │  │ expiryDate       │◄────│ ipAddress   │   │
│ lastName     │  │  │ clickCount       │     │ userAgent   │   │
│ enabled      │  │  │ active           │     │ browserName │   │
│ createdAt    │  │  │ user_id ─────────┘     │ country     │   │
└──────┬───────┘  │  │ createdAt            │ city        │   │
       │          │  └──────────────────────┘ │ latitude    │   │
       │          │                           │ longitude   │   │
       │          │     ┌──────────────────┐  │ clickedAt   │   │
       │          │     │   ClickLog       │  └─────────────┘   │
       │          │     ├──────────────────┤                    │
       │          └────►│ id (BIGINT)      │                    │
       │                │ shortCode        │                    │
       │                │ urlId ───────────┘────────────────────┘
       │                │ ipAddress
       │                │ clickedAt
       │     ┌──────────┴──────────┐
       │     │       Role          │
       │     ├─────────────────────┤
       │     │ id (BIGINT)         │
       └────►│ name (UNQ)          │
             └─────────────────────┘

┌──────────────────────┐
│     user_roles       │ (Join Table)
├──────────────────────┤
│ user_id (FK)         │
│ role_id (FK)         │
└──────────────────────┘
```

### Tables

**`users`** — Stores registered user accounts.
- `id`: UUID primary key
- `email`: Unique, used as login identifier
- `password`: BCrypt-hashed
- `firstName`, `lastName`: Optional profile information
- `enabled`, `accountNonExpired`, `accountNonLocked`, `credentialsNonExpired`: Account status flags
- `createdAt`: Auto-set on creation

**`roles`** — Available roles for authorization.
- Pre-seeded with `USER` and `ADMIN`
- `name`: Unique role identifier

**`user_roles`** — Join table linking users to roles (many-to-many).

**`url_mappings`** — Core table storing URL mappings.
- `id`: UUID primary key
- `shortCode`: 3-20 character unique identifier (Base62 encoded)
- `originalUrl`: The target URL (up to 2048 characters)
- `expiryDate`: Optional expiration timestamp (auto-deactivated when passed)
- `clickCount`: Incremented on each redirect
- `active`: Boolean flag for manual deactivation
- `user_id`: Foreign key to `users.id` (ownership)

**`analytics`** — Detailed click analytics data.
- One record per click on any short URL
- Stores parsed User-Agent information (browser, OS, device)
- Geographic data (country, city, region, lat/lng)
- IP address and referer tracking
- URL reference via `urlId` FK

**`click_logs`** — Lightweight click log for fast querying.
- Simplified schema with just short code, URL ID, IP, referer, and timestamp
- Foreign key with `ON DELETE CASCADE` from `url_mappings.id`

### Indexes
- `url_mappings`: `idx_short_code` (unique), `idx_custom_alias`, `idx_active`, `idx_expiry_date`, `idx_created_at`
- `analytics`: `idx_url_id`, `idx_short_code`, `idx_clicked_at`, `idx_ip_address`, `idx_country`, `idx_device_type`
- `click_logs`: `idx_url_id`, `idx_clicked_at`

---

## ⚡ Redis Caching Strategy

### Architecture Overview

```
┌─────────┐          ┌─────────┐          ┌─────────┐
│ Request  │  miss    │  Redis  │  miss    │  MySQL  │
│   ───────┼────────►│  Cache  ├─────────►│   DB    │
│          │          │         │          │         │
│  hit ◄───┼──────────│    ◄────┼──────────│         │
│   ~1ms   │          │         │          │  ~10ms  │
└─────────┘          └─────────┘          └─────────┘
```

### Cache Regions

| Cache Key Pattern | TTL | Content | Purpose |
|------------------|-----|---------|---------|
| `url:{shortCode}` | 1 hour | Full `UrlMapping` entity | Dashboard URL details |
| `redirect:{shortCode}` | 30 min | Original URL string | Fast redirect resolution |
| `qr:{shortCode}` | 24 hours | QR code image (hex) | QR code delivery |

### Cache-Aside Pattern

1. **Read**: Check Redis first → return if hit → query MySQL if miss → populate Redis
2. **Write**: Save to MySQL → update Redis (or invalidate)
3. **Update/Delete**: Invalidate Redis → update MySQL → re-cache if needed

### Rate Limiting with Redis

Rate limiting uses a **sliding window** algorithm implemented with Redis sorted sets and a Lua script for atomicity:

```
Key: rate_limit:{ip}:{endpoint_group}

ZADD rate_limit:192.168.1.1:GENERAL <timestamp> <unique_id>
ZREMRANGEBYSCORE rate_limit:192.168.1.1:GENERAL 0 <window_start>
ZCARD rate_limit:192.168.1.1:GENERAL

If count >= limit → BLOCK (429)
If count < limit → ALLOW
```

The Lua script executes all three operations atomically, preventing race conditions in concurrent requests.

---

## 🔐 JWT Authentication Flow

### Token Architecture

```
┌─────────────────────┐        ┌──────────────────────┐
│   Access Token      │        │   Refresh Token       │
├─────────────────────┤        ├──────────────────────┤
│ Algorithm: HS256    │        │ Algorithm: HS256      │
│ Secret: app.jwt.    │        │ Secret: app.jwt.      │
│   secret            │        │   refresh-secret      │
│ TTL: 24 hours       │        │ TTL: 7 days           │
│ Claims:             │        │ Claims:               │
│  • sub (email)      │        │  • sub (email)        │
│  • iat              │        │  • type: "refresh"    │
│  • exp              │        │  • roles: [...]       │
│                     │        │  • iat                │
│                     │        │  • exp                │
└─────────────────────┘        └──────────────────────┘
```

### Authentication Flow

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Client   │     │   Backend    │     │   Database   │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘
     │                  │                    │
     │  POST /auth/login│                    │
     │  {email, pass}   │                    │
     ├─────────────────►│                    │
     │                  │  Find user by email│
     │                  ├───────────────────►│
     │                  │◄────────────────────│
     │                  │                    │
     │                  │  BCrypt.verify()   │
     │                  │  Create JWT        │
     │                  │  Create Refresh    │
     │  {token,         │                    │
     │   refreshToken}  │                    │
     │◄─────────────────┤                    │
     │                  │                    │
     │  GET /api/v1/urls│                    │
     │  Authorization:  │                    │
     │  Bearer <token>  │                    │
     ├─────────────────►│                    │
     │                  │  JwtFilter:        │
     │                  │  • Parse token     │
     │                  │  • Verify sig      │
     │                  │  • Check exp       │
     │                  │  • Extract email   │
     │                  │  • Set SecurityCtx │
     │                  │                    │
     │                  │  Fetch user's URLs │
     │                  ├───────────────────►│
     │                  │◄────────────────────│
     │◄─────────────────┤                    │
     │                  │                    │
     │  Token expires ──┤                    │
     │                  │                    │
     │  POST /auth/     │                    │
     │  refresh         │                    │
     │  {refreshToken}  │                    │
     ├─────────────────►│                    │
     │                  │  Validate refresh  │
     │                  │  Issue new tokens  │
     │  {newToken,      │                    │
     │   newRefresh}    │                    │
     │◄─────────────────┤                    │
```

### Security Configuration

```
SecurityFilterChain
├── CSRF: DISABLED (stateless API)
├── HTTP Basic: DISABLED
├── Session: STATELESS
├── Exception Handling: JwtAuthenticationEntryPoint (401 JSON)
├── Public paths: /, /api/v1/auth/**, /{shortCode}, /api/v1/qr/**,
│                  /api/v1/health, /swagger-ui/**, /v3/api-docs/**,
│                  /app/**, /css/**, /js/**
├── All other paths: AUTHENTICATED
└── Filter Order:
    1. RateLimitingFilter
    2. JwtAuthenticationFilter
    3. UsernamePasswordAuthenticationFilter
```

### Client-Side Token Refresh

The frontend implements automatic token refresh:

1. Before each API call, checks if the access token expires within 60 seconds
2. If expiring soon, calls `POST /api/v1/auth/refresh` with the refresh token
3. Stores the new access token and refresh token in `localStorage`
4. On 401 response, automatically retries once after refreshing the token
5. On refresh failure, clears tokens and redirects to login

---

## 🚦 Rate Limiting

Three tiers of rate limiting protect the application:

| Tier | Limit | Window | Endpoints |
|------|-------|--------|-----------|
| `GENERAL` | 100 requests | 60 seconds | All API endpoints (default) |
| `URL_CREATE` | 20 requests | 60 seconds | `POST /api/v1/urls`, `POST /api/v1/urls/batch` |
| `AUTH` | 10 requests | 60 seconds | `POST /api/v1/auth/login`, `POST /api/v1/auth/register` |

When rate limited, the response includes:

```json
// Status: 429 Too Many Requests
// Headers:
//   X-RateLimit-Remaining: 0
//   X-RateLimit-Reset: 42
//   Retry-After: 42

{
  "success": false,
  "message": "Rate limit exceeded. Please wait before making another request.",
  "timestamp": "2024-01-15T10:30:42.123Z"
}
```

---

## 🐳 Docker Setup

### Services

The `docker-compose.yml` orchestrates three services:

| Service | Image | Port (Host:Container) | Purpose |
|---------|-------|-----------------------|---------|
| `mysql` | `mysql:8.0` | `3307:3306` | Primary database |
| `redis` | `redis:7-alpine` | `6380:6379` | Caching & rate limiting |
| `app` | Custom build | `8080:8080` | Spring Boot application |

### Usage

```bash
# Build and start all services
docker-compose up --build

# Start in background
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes (reset data)
docker-compose down -v
```

### Dockerfile (Multi-stage Build)

```dockerfile
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline       # Cache dependencies
COPY src ./src
RUN mvn package -DskipTests         # Build JAR

# Stage 2: Runtime with JRE
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s CMD curl -sf http://localhost:8080/api/v1/health
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Activates docker-specific configuration |
| `MYSQL_USER` | `urluser` | MySQL username |
| `MYSQL_PASSWORD` | `urlpass` | MySQL password |
| `JWT_SECRET` | *(default)* | JWT signing secret (change in production!) |
| `JWT_REFRESH_SECRET` | *(default)* | Refresh token secret (change in production!) |
| `BASE_URL` | `http://localhost:8080` | Public-facing base URL |
| `REDIS_PORT` | `6379` | Redis port (internal) |

---

## ⚙️ Configuration

Key configuration properties in `application.yml`:

```yaml
server:
  port: 8090                    # Application port

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/url_shortener?createDatabaseIfNotExist=true
    username: root
    password: yourpassword

app:
  jwt:
    secret: your-256-bit-secret  # HS256 requires 256+ bits
    refresh-secret: your-refresh-secret
    expiration: 86400000         # 24 hours in ms
  url:
    base-url: http://localhost:8090
    short-code-length: 7
  cache:
    url-ttl: 3600                # 1 hour
    redirect-ttl: 1800           # 30 minutes
  rate-limit:
    default:
      capacity: 100
      window-seconds: 60
```

> **⚠ Security Note**: Change the JWT secrets before deploying to production. The defaults in `application.yml` are for development only.

---

## 📁 Project Structure

```
url-shortener/
├── src/main/java/com/urlshortener/
│   ├── UrlShortenerApplication.java     # Entry point
│   ├── config/
│   │   ├── JpaConfig.java               # JPA auditing
│   │   ├── SecurityConfig.java          # Spring Security chain
│   │   ├── RedisConfig.java             # Redis + cache manager
│   │   ├── WebConfig.java               # View controller
│   │   ├── OpenApiConfig.java           # Swagger/OpenAPI
│   │   ├── JacksonConfig.java           # ObjectMapper config
│   │   └── RateLimitingFilter.java      # Rate limit servlet filter
│   ├── controller/
│   │   ├── AuthController.java          # Authentication endpoints
│   │   ├── UrlController.java           # URL CRUD + redirect
│   │   ├── AnalyticsController.java     # Analytics endpoints
│   │   ├── HealthController.java        # Health check
│   │   └── QrCodeController.java        # QR code generation
│   ├── dto/                             # 16 request/response DTOs
│   ├── entity/
│   │   ├── User.java                    # User entity
│   │   ├── Role.java                    # Role entity
│   │   ├── UrlMapping.java              # URL mapping entity
│   │   ├── Analytics.java               # Click analytics entity
│   │   └── ClickLog.java                # Click log entity
│   ├── exception/                       # Custom exceptions + handler
│   ├── repository/                      # JPA repositories (5)
│   ├── security/
│   │   ├── JwtService.java              # JWT token service
│   │   ├── JwtRefreshService.java       # Refresh token service
│   │   ├── JwtAuthenticationFilter.java # JWT auth filter
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── CustomUserDetailsService.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── UrlManagementService.java    # Main URL service
│   │   ├── UrlMappingService.java       # Secondary URL service
│   │   ├── UrlRedirectService.java      # Redirect resolution
│   │   ├── AnalyticsService.java        # Click analytics
│   │   ├── RedisCacheService.java        # Cache operations
│   │   ├── RateLimitingService.java      # Rate limit engine
│   │   ├── JwtRefreshService.java        # Refresh token service
│   │   └── QrCodeService.java           # QR code generation
│   └── util/
│       ├── Base62Encoder.java           # Base62 encoding
│       ├── ShortCodeGenerator.java       # Short code generation
│       ├── UserAgentParser.java          # User-Agent parsing
│       └── DateUtils.java               # Date helpers
├── src/main/resources/
│   ├── application.yml                  # Main config
│   ├── application-docker.yml           # Docker profile
│   ├── schema.sql                       # DB schema + seed data
│   ├── scripts/rate_limit.lua           # Redis Lua script
│   └── static/
│       ├── app/
│       │   ├── index.html               # Home page
│       │   ├── dashboard.html           # Dashboard
│       │   ├── login.html               # Login page
│       │   └── signup.html              # Signup page
│       ├── css/style.css                # Styles
│       └── js/app.js                    # Frontend logic
├── Dockerfile                           # Multi-stage build
├── docker-compose.yml                   # Service orchestration
├── pom.xml                              # Maven build
└── README.md                            # This file
```



---

## 🔮 Future Improvements

### Performance & Scalability
- [ ] **Database read replicas** for analytics queries (read-heavy workload)
- [ ] **CDN integration** for static assets and QR code delivery
- [ ] **WebSocket-based live updates** for dashboard analytics
- [ ] **Connection pooling tuning** with metrics-based auto-scaling
- [ ] **gRPC** for internal service communication (microservices split)

### Security
- [ ] **Rate limiting by user ID** (currently IP-based, add user-based for authenticated routes)
- [ ] **API key support** for third-party integrations
- [ ] **IP allowlisting/blocklisting** for admin endpoints
- [ ] **CSP headers** for XSS prevention
- [ ] **HttpOnly cookies** for token storage (instead of localStorage)
- [ ] **CSRF protection** for state-changing endpoints

### Features
- [ ] **Custom domains** — allow users to map their own domain
- [ ] **Link expiration webhooks** — notify users when URLs expire
- [ ] **Bulk CSV import/export** for URL management
- [ ] **Password-protected URLs** — require password to access
- [ ] **UTM parameter builder** — generate campaign-tagged URLs
- [ ] **A/B testing** — multiple destination URLs for one short code
- [ ] **Social media preview cards** — Open Graph / Twitter Card support

### Infrastructure
- [ ] **CI/CD pipeline** (GitHub Actions → build → test → deploy)
- [ ] **Kubernetes manifests** for container orchestration
- [ ] **Helm chart** for configurable K8s deployment
- [ ] **Terraform** for cloud infrastructure provisioning
- [ ] **Grafana dashboards** for Redis, JVM, and DB metrics
- [ ] **Distributed tracing** with OpenTelemetry

### Testing
- [ ] **Integration tests** with Testcontainers (MySQL + Redis)
- [ ] **Load testing** with k6 or Gatling
- [ ] **Security scanning** with OWASP ZAP
- [ ] **API contract tests** with Spring Cloud Contract

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <p><strong>© 2026 URL Shortener</strong> | Developed by <a href="https://www.linkedin.com/in/chetanamahajan">Chetana Mahajan</a> using Spring Boot &amp; Bootstrap</p>
  <p>
    <a href="https://www.linkedin.com/in/chetanamahajan"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=flat&logo=linkedin&logoColor=white" alt="LinkedIn"></a>
    &nbsp;
    <a href="https://github.com/chetana987"><img src="https://img.shields.io/badge/GitHub-100000?style=flat&logo=github&logoColor=white" alt="GitHub"></a>
  </p>
</div>
