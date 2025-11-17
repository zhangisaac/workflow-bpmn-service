# Token Refresh and Blacklist Implementation

## Overview

This document describes the implementation of token refresh and token blacklisting features in the Java/Spring Boot
backend.

## Features Implemented

### 1. Refresh Token Support

- **Long-lived tokens**: Refresh tokens expire after 1 day (configurable)
- **Token storage**: In-memory storage using `RefreshTokenService`
- **Token validation**: Validates refresh tokens before issuing new access tokens
- **Token revocation**: Supports revoking individual tokens or all tokens for a user

### 2. Token Blacklisting

- **Blacklist storage**: In-memory storage using `TokenBlacklistService`
- **Automatic cleanup**: Expired tokens are automatically removed from blacklist
- **Validation integration**: JWT validation checks blacklist before accepting tokens

## API Endpoints

### POST `/api/auth/login`

**Public endpoint** - Authenticates user and returns both access and refresh tokens.

**Request:**

```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response:**

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001",
  "expiresAt": "2025-11-14T18:00:00Z",
  "refreshExpiresAt": "2025-11-21T15:00:00Z",
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### POST `/api/auth/refresh`

**Public endpoint** - Refreshes access token using a valid refresh token.

**Request:**

```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:**

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001",
  "expiresAt": "2025-11-14T20:00:00Z",
  "refreshExpiresAt": "2025-11-21T15:00:00Z",
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

**Error Response (401):**

```json
{
  "error": "Invalid or expired refresh token"
}
```

### POST `/api/auth/logout`

**Authenticated endpoint** - Logs out user by blacklisting access token and revoking refresh token.

**Request Headers:**

```
Authorization: Bearer <access_token>
```

**Request Body (optional):**

```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001"
}
```

**Response:**

```
204 No Content
```

## Configuration

### application.yml

```yaml
jwt:
  secret: change-me-in-production-please-change-me
  expiration-in-minutes: 10  # Access token expiration (10 mints)
  refresh-expiration-in-days: 1  # Refresh token expiration (1 days)
```

## Components

### 1. TokenBlacklistService

- **Purpose**: Manages blacklisted JWT access tokens
- **Storage**: In-memory `ConcurrentHashMap`
- **Features**:
    - Add tokens to blacklist
    - Check if token is blacklisted
    - Automatic cleanup of expired tokens
    - Remove tokens from blacklist

**Key Methods:**

- `blacklistToken(String token, Instant expirationTime)` - Add token to blacklist
- `isTokenBlacklisted(String token)` - Check if token is blacklisted
- `removeFromBlacklist(String token)` - Remove token from blacklist
- `cleanupExpiredTokens()` - Clean up expired tokens

### 2. RefreshTokenService

- **Purpose**: Manages refresh tokens
- **Storage**: In-memory `ConcurrentHashMap`
- **Features**:
    - Generate secure refresh tokens (UUID-based)
    - Validate refresh tokens
    - Revoke individual tokens
    - Revoke all tokens for a user
    - Automatic cleanup of expired tokens

**Key Methods:**

- `generateRefreshToken(String username, Instant expirationTime)` - Generate new refresh token
- `validateRefreshToken(String refreshToken)` - Validate and return username
- `revokeRefreshToken(String refreshToken)` - Revoke a specific token
- `revokeAllTokensForUser(String username)` - Revoke all tokens for a user
- `cleanupExpiredTokens()` - Clean up expired tokens

### 3. JwtTokenProvider (Updated)

- **New Features**:
    - Blacklist checking during token validation
    - Refresh token expiration calculation
    - Integration with `TokenBlacklistService`

**Updated Methods:**

- `validateToken(String token)` - Now checks blacklist before validating signature
- `getRefreshTokenExpiration()` - Returns refresh token expiration time

### 4. AuthService (Updated)

- **New Methods**:
    - `refreshToken(RefreshTokenRequest request)` - Refresh access token
    - `logout(String accessToken, String refreshToken)` - Logout user
    - `logoutAll(String username)` - Logout all sessions for a user

**Updated Methods:**

- `login(LoginRequest request)` - Now returns refresh token

### 5. AuthController (Updated)

- **New Endpoints**:
    - `POST /api/auth/refresh` - Refresh access token
    - `POST /api/auth/logout` - Logout user

## Security Configuration

### Public Endpoints

- `/api/auth/login` - Login (no authentication required)
- `/api/auth/refresh` - Refresh token (no authentication required)

### Authenticated Endpoints

- `/api/auth/logout` - Logout (requires valid access token)

## Flow Diagrams

### Login Flow

```
Client → POST /api/auth/login
  ↓
AuthService.login()
  ↓
AuthenticationManager.authenticate()
  ↓
JwtTokenProvider.generateToken() → Access Token
  ↓
RefreshTokenService.generateRefreshToken() → Refresh Token
  ↓
Return LoginResponse (accessToken + refreshToken)
```

### Refresh Flow

```
Client → POST /api/auth/refresh (with refreshToken)
  ↓
AuthService.refreshToken()
  ↓
RefreshTokenService.validateRefreshToken() → username
  ↓
UserDetailsService.loadUserByUsername() → UserDetails
  ↓
JwtTokenProvider.generateToken() → New Access Token
  ↓
Return LoginResponse (new accessToken + same refreshToken)
```

### Logout Flow

```
Client → POST /api/auth/logout (with Authorization header)
  ↓
JwtAuthenticationFilter validates access token
  ↓
AuthController.logout() extracts tokens
  ↓
AuthService.logout()
  ↓
TokenBlacklistService.blacklistToken() → Blacklist access token
  ↓
RefreshTokenService.revokeRefreshToken() → Revoke refresh token
  ↓
Return 204 No Content
```

### Token Validation Flow

```
Request with Authorization header
  ↓
JwtAuthenticationFilter.resolveToken()
  ↓
JwtTokenProvider.validateToken()
  ↓
TokenBlacklistService.isTokenBlacklisted() → Check blacklist
  ↓
If not blacklisted → Validate JWT signature and expiration
  ↓
If valid → Set SecurityContext authentication
```

## Usage Examples

### cURL Examples

**1. Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**2. Refresh Token:**

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token_from_login>"}'
```

**3. Logout:**

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}'
```

## Production Considerations

### Current Implementation (In-Memory)

- ✅ Fast and simple
- ❌ Not persistent (lost on restart)
- ❌ Not distributed (won't work in cluster)

### Recommended Production Enhancements

1. **Use H2 Database for Persistence:**
    - Create `blacklisted_tokens` table
    - Create `refresh_tokens` table
    - Use JPA repositories instead of in-memory maps

2. **Use Redis for Distributed Systems:**
    - Store blacklist in Redis with TTL
    - Store refresh tokens in Redis
    - Enables horizontal scaling

3. **Token Rotation:**
    - Rotate refresh tokens on each refresh
    - Invalidate old refresh token when new one is issued
    - Prevents token reuse attacks

4. **Token Tracking:**
    - Track active tokens per user
    - Enable `logoutAll()` to blacklist all user tokens
    - Add device/session tracking

5. **Scheduled Cleanup:**
    - Use `@Scheduled` to periodically clean expired tokens
    - Prevents memory leaks
    - Reduces storage usage

## Testing

> **Note**: For comprehensive testing information, see [TESTING_GUIDE.md](TESTING_GUIDE.md). This project includes Integration Tests and E2E Tests that cover token refresh and blacklisting functionality.

### Test Scenarios

The following scenarios are covered by Integration Tests (`*IntegrationTest.java`) and E2E Tests:

1. **Login and receive tokens:**
    - Login should return both access and refresh tokens
    - Tokens should have correct expiration times
    - Tested in: `AuthControllerIntegrationTest`, `e2e-test.sh`

2. **Refresh token:**
    - Valid refresh token should return new access token
    - Invalid refresh token should return 401
    - Expired refresh token should return 401
    - Tested in: `AuthControllerIntegrationTest`, `e2e-test.sh`

3. **Logout:**
    - Logout should blacklist access token
    - Logout should revoke refresh token
    - Blacklisted token should be rejected on subsequent requests
    - Tested in: `AuthControllerIntegrationTest`, `e2e-test.sh`

4. **Token validation:**
    - Blacklisted token should fail validation
    - Valid token should pass validation
    - Expired token should fail validation
    - Tested in: `JwtTokenProviderTest`, `AuthControllerIntegrationTest`

### Running Tests

```bash
# Run Integration Tests
mvn verify

# Run E2E Tests (requires server running)
./e2e-test.sh

# Run with coverage
mvn test jacoco:report
```

## Notes

- **No Vue/Frontend Changes**: This implementation is backend-only as requested
- **In-Memory Storage**: Both blacklist and refresh tokens use in-memory storage
- **Token Format**: Refresh tokens are UUID-based strings, not JWTs
- **Security**: Access tokens are JWTs with blacklist checking
- **Expiration**: Access tokens expire in 10 mints, refresh tokens in 1 days (configurable)

## Timezone Guidance

- JWT timestamps (`iat`, `exp`) are epoch-based and timezone-agnostic. Using `Instant` is correct.
- For local-time display in logs or API responses, convert Instants:
    - `ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());`
    - Or return `OffsetDateTime`/`ZonedDateTime` in DTOs if you prefer including zone/offset.
- If you want to compute expiry from local time and still store in UTC Instant:
    - `ZonedDateTime nowLocal = ZonedDateTime.now(ZoneId.systemDefault());`
    - `Instant expiry = nowLocal.plusMinutes(expMinutes).toInstant();`
- Optional JSON serialization timezone (local):
    - `spring.jackson.time-zone=system` and `serialization.write-dates-as-timestamps=false` in `application.yml`.

