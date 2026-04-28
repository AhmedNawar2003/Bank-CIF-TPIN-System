# 🏦 Bank CIF & TPIN Management System

A secure REST API built with **Spring Boot 4.0.6** and **Java 21** for managing Bank Customer Information Files (CIF) and Transaction PINs (TPIN).

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [CIF Status Flow](#cif-status-flow)
- [Logging](#logging)
- [Testing](#testing)
- [Configuration](#configuration)

---

## Overview

This system provides a secure backend for:

- **Registering** new CIF accounts
- **Setting** a TPIN (Transaction PIN) for new accounts
- **Authenticating** CIF holders using their TPIN — returns a JWT token
- **Resetting** TPIN to unblock a locked account

Key security features:
- TPIN is stored as a **BCrypt hash** — never plain text
- **3 consecutive failed** authentication attempts automatically **block** the CIF
- All API requests and responses are **logged to a file** with TPIN masked
- **JWT-based** stateless authentication

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.6 | Framework |
| Spring Security | (managed by Boot 4.0.6) | Security layer |
| Spring Data JPA | (managed by Boot 4.0.6) | Database ORM |
| H2 Database | (managed by Boot 4.0.6) | In-memory data storage |
| JJWT | 0.12.5 | JWT generation & validation |
| BCrypt | Built-in via Spring Security | TPIN hashing |
| SpringDoc OpenAPI | 3.0.2 | Swagger UI |
| Lombok | (managed by Boot 4.0.6) | Boilerplate reduction |
| Maven | 3.x | Build tool |

---

## Project Structure

```
bank-cif-tpin/
├── pom.xml
├── README.md
├── logs/
│   └── bank-cif-tpin.log               ← generated at runtime
└── src/
    ├── main/
    │   ├── java/com/bank/ciftpin/
    │   │   ├── BankCifTpinApplication.java
    │   │   ├── config/
    │   │   │   └── AppConfig.java           ← BCrypt bean + Swagger config
    │   │   ├── controller/
    │   │   │   └── CifTpinController.java   ← 4 REST endpoints
    │   │   ├── service/
    │   │   │   └── CifTpinService.java      ← Business logic
    │   │   ├── model/
    │   │   │   └── CifAccount.java          ← JPA Entity
    │   │   ├── repository/
    │   │   │   └── CifAccountRepository.java
    │   │   ├── dto/
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── SetTpinRequest.java
    │   │   │   ├── AuthRequest.java
    │   │   │   ├── ResetTpinRequest.java
    │   │   │   ├── AuthResponse.java        ← contains JWT token
    │   │   │   ├── CifAccountResponse.java
    │   │   │   └── ApiResponse.java         ← standard wrapper
    │   │   ├── exception/
    │   │   │   ├── CifNotFoundException.java
    │   │   │   ├── CifAlreadyExistsException.java
    │   │   │   ├── CifBlockedException.java
    │   │   │   ├── InvalidTpinException.java
    │   │   │   ├── TpinNotSetException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── filter/
    │   │   │   └── RequestResponseLoggingFilter.java
    │   │   └── security/
    │   │       ├── SecurityConfig.java
    │   │       ├── JwtService.java
    │   │       ├── JwtAuthenticationFilter.java
    │   │       └── SecurityExceptionHandler.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/bank/ciftpin/
            └── BankCifTpinApplicationTests.java  ← 5 unit tests
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.6+

### Build & Run

```bash
# 1. Clone the project
git clone <repository-url>
cd bank-cif-tpin

# 2. Build
mvn clean package

# 3. Run
java -jar target/cif-tpin-1.0.0.jar
```

The server starts on **http://localhost:8080**

### Available URLs

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/h2-console` | H2 in-memory database console |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON spec |

### H2 Console Login

```
JDBC URL:  jdbc:h2:mem:bankdb
Username:  sa
Password:  (leave empty)
```

---

## API Endpoints

Base URL: `http://localhost:8080/api/v1/cif`

All responses follow this standard wrapper:

```json
{
  "success": true,
  "message": "...",
  "data": { },
  "timestamp": "2024-01-01T12:00:00"
}
```

---

### 1. Register CIF

**POST** `/api/v1/cif/register`

Creates a new CIF account in `PENDING_TPIN` status.

**Request:**
```json
{
  "cifNumber": "123456789",
  "fullName": "Ahmed Nawar",
  "email": "ahmed@bank.com"
}
```

**Validation rules:**
- `cifNumber` — 6 to 12 digits only
- `fullName` — 3 to 100 characters
- `email` — valid email format

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "CIF registered successfully. Please set your TPIN.",
  "data": {
    "cifNumber": "123456789",
    "fullName": "Ahmed Nawar",
    "email": "ahmed@bank.com",
    "status": "PENDING_TPIN",
    "tpinConfigured": false,
    "createdAt": "2024-01-01T12:00:00"
  }
}
```

**Error responses:**

| Status | Reason |
|---|---|
| `400` | Validation failed (missing/invalid fields) |
| `409` | CIF number or email already registered |

---

### 2. Set TPIN

**POST** `/api/v1/cif/set-tpin`

Sets the TPIN for a newly registered CIF. Activates the account.

**Request:**
```json
{
  "cifNumber": "123456789",
  "tpin": "1234"
}
```

**Validation rules:**
- `tpin` — 4 to 6 digits only

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "TPIN set successfully. Account is now active.",
  "data": {
    "cifNumber": "123456789",
    "status": "ACTIVE",
    "tpinConfigured": true
  }
}
```

**Error responses:**

| Status | Reason |
|---|---|
| `400` | TPIN already set — use Reset TPIN instead |
| `403` | CIF is blocked |
| `404` | CIF not found |

---

### 3. Authenticate

**POST** `/api/v1/cif/authenticate`

Authenticates a CIF with its TPIN. Returns a JWT Bearer token on success.
**3 consecutive failures will block the CIF.**

**Request:**
```json
{
  "cifNumber": "123456789",
  "tpin": "1234"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Authentication successful.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "account": {
      "cifNumber": "123456789",
      "status": "ACTIVE"
    }
  }
}
```

**Using the token in subsequent requests:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Error responses:**

| Status | Reason |
|---|---|
| `400` | TPIN has not been set yet |
| `401` | Invalid TPIN — message includes remaining attempts |
| `403` | CIF is blocked after 3 failures |
| `404` | CIF not found |

---

### 4. Reset TPIN

**POST** `/api/v1/cif/reset-tpin`

Unblocks a blocked CIF and sets a new TPIN. Also works for active accounts.

**Request:**
```json
{
  "cifNumber": "123456789",
  "newTpin": "5678"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "TPIN reset successfully. Account is now active.",
  "data": {
    "cifNumber": "123456789",
    "status": "ACTIVE",
    "tpinConfigured": true
  }
}
```

**Error responses:**

| Status | Reason |
|---|---|
| `400` | No TPIN was set previously — use Set TPIN instead |
| `404` | CIF not found |

---

## Security

### Architecture

```
Incoming Request
      │
      ▼
JwtAuthenticationFilter        ← reads Authorization: Bearer <token>
      │
      ▼
SecurityConfig                 ← public? pass through. protected? validate token.
      │
      ▼
Controller → Service → Repository
```

### Security Classes

| Class | Responsibility |
|---|---|
| `SecurityConfig.java` | Defines public vs protected endpoints, disables CSRF, sets stateless session |
| `JwtService.java` | Generates JWT tokens, validates them, extracts CIF number |
| `JwtAuthenticationFilter.java` | Runs on every request, reads and validates the Bearer token |
| `SecurityExceptionHandler.java` | Returns clean JSON for 401 and 403 errors |

### Public Endpoints (no token required)

```
POST /api/v1/cif/register
POST /api/v1/cif/set-tpin
POST /api/v1/cif/authenticate
POST /api/v1/cif/reset-tpin
GET  /swagger-ui/**
GET  /h2-console/**
```

### Protected Endpoints (JWT required)

```
Any other endpoint not listed above
```

### JWT Token Details

| Property | Value |
|---|---|
| Algorithm | HMAC SHA-256 |
| Expiry | 24 hours (86400 seconds) |
| Payload | `sub: cifNumber`, `type: CIF_SESSION`, `iat`, `exp` |

### TPIN Security

- TPIN is **never stored as plain text** — always BCrypt hashed
- TPIN is **masked in all logs** as `****`
- **3 consecutive wrong** TPINs lock the account
- **Successful authentication** resets the failed attempts counter to 0

---

## CIF Status Flow

```
  Register
     │
     ▼
PENDING_TPIN ──── Set TPIN ────► ACTIVE
                                   │        ▲
                                   │        │
                              3 failures   Reset TPIN
                                   │        │
                                   ▼        │
                                BLOCKED ────┘
```

| Status | Description |
|---|---|
| `PENDING_TPIN` | Account created, no TPIN set yet |
| `ACTIVE` | Fully operational, can authenticate |
| `BLOCKED` | Locked after 3 failed attempts, requires Reset TPIN |

---

## Logging

All API requests and responses are logged automatically via `RequestResponseLoggingFilter`.

**Log file location:** `logs/bank-cif-tpin.log`

**Log format:**
```
======================================================
[REQUEST-A1B2C3D4]  POST /api/v1/cif/authenticate | IP: 127.0.0.1
  Body: {"cifNumber":"123456789","tpin":"****"}
[RESPONSE-A1B2C3D4] Status: 200 | Duration: 43ms
  Body: {"success":true,"data":{"token":"eyJ..."}}
======================================================
```

> **Note:** TPIN values are always masked as `****` in logs for security.

**Log levels:**
- `DEBUG` — all application-level events
- `INFO` — successful operations
- `WARN` — failed auth attempts, blocked CIF, validation errors
- `ERROR` — unexpected exceptions

---

## Testing

### Run all tests

```bash
mvn test
```

### Test coverage

| Test | Description |
|---|---|
| `testRegisterSuccess` | Register a new CIF successfully |
| `testRegisterDuplicateCif` | Reject duplicate CIF registration |
| `testSetTpinSuccess` | Set TPIN and verify ACTIVE status |
| `testBlockAfterThreeFailures` | Verify CIF is blocked after 3 wrong TPINs |
| `testResetTpinUnblocks` | Unblock CIF and verify new TPIN works |
| `testFailedAttemptsResetOnSuccess` | Verify counter resets after successful auth |

---

## Configuration

`src/main/resources/application.properties`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
</parent>
```

> ⚠️ **Note on your pom.xml:** Several artifact IDs in the uploaded `pom.xml` don't exist in Maven Central and will cause build failures. Use the corrected versions below:

| Your artifact ID | ❌ Problem | ✅ Correct artifact ID |
|---|---|---|
| `spring-boot-h2console` | Doesn't exist | `spring-boot-starter-web` |
| `spring-boot-starter-webmvc` | Doesn't exist | `spring-boot-starter-web` |
| `spring-boot-starter-data-jpa-test` | Doesn't exist | `spring-boot-starter-test` |
| `spring-boot-starter-validation-test` | Doesn't exist | `spring-boot-starter-test` |
| `spring-boot-starter-webmvc-test` | Doesn't exist | `spring-boot-starter-test` |

**Correct `pom.xml` dependencies section:**

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>3.0.2</version>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:bankdb
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true

# JWT
jwt.secret=BankCifTpinSuperSecretKeyForJWTSigningMustBe256BitsLong!
jwt.expiration=86400000

# Logging
logging.file.name=logs/bank-cif-tpin.log
logging.level.com.bank=DEBUG

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
```

> ⚠️ **Production note:** Replace `jwt.secret` with a secure random key stored in environment variables, not hardcoded.

---

## Postman Quick Start

Import these requests into Postman to test the full flow:

```
Step 1 — Register
POST http://localhost:8080/api/v1/cif/register
Body: {"cifNumber":"123456789","fullName":"Ahmed Nawar","email":"ahmed@bank.com"}

Step 2 — Set TPIN
POST http://localhost:8080/api/v1/cif/set-tpin
Body: {"cifNumber":"123456789","tpin":"1234"}

Step 3 — Authenticate (copy the token from response)
POST http://localhost:8080/api/v1/cif/authenticate
Body: {"cifNumber":"123456789","tpin":"1234"}

Step 4 — Reset TPIN (if blocked)
POST http://localhost:8080/api/v1/cif/reset-tpin
Body: {"cifNumber":"123456789","newTpin":"5678"}
```

---

## Author

**Ahmed Nawar** — Backend Developer | Java & Spring Boot | Microservices | Docker | Kafka
