# API Endpoints & Testing URLs

Base URL: `http://localhost:8080`

## 🔐 Authentication Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint            | Description                                     | Request Body                                           |
|--------|---------------------|-------------------------------------------------|--------------------------------------------------------|
| POST   | `/api/auth/login`   | Authenticate user and receive tokens            | `{"username": "admin", "password": "admin"}`          |
| POST   | `/api/auth/refresh` | Refresh access token using refresh token        | `{"refreshToken": "<refresh_token>"}`                 |

**Example: Login Request**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'
```

**Example: Login Response**

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001",
  "expiresAt": "2025-11-14T14:00:00Z",
  "refreshExpiresAt": "2025-11-21T15:00:00Z",
  "username": "admin",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_USER"
  ]
}
```

**Example: Refresh Token Request**

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001"}'
```

**Example: Refresh Token Response**

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001",
  "expiresAt": "2025-11-14T16:00:00Z",
  "refreshExpiresAt": "2025-11-21T15:00:00Z",
  "username": "admin",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_USER"
  ]
}
```

**Example: Refresh Token Error (401 Unauthorized)**

```json
{
  "error": "Invalid or expired refresh token"
}
```

### Authenticated Endpoints (Authentication Required)

| Method | Endpoint          | Description                          | Authorization                    | Request Body (Optional)                    |
|--------|-------------------|--------------------------------------|----------------------------------|--------------------------------------------|
| POST   | `/api/auth/logout` | Logout user (blacklist token)        | Bearer Token                     | `{"refreshToken": "<refresh_token>"}`      |

**Example: Logout Request**

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000-550e8400-e29b-41d4-a716-446655440001"}'
```

**Example: Logout Response**

```
204 No Content
```

**Notes:**
- The `refreshToken` in the logout request body is optional. If provided, it will be revoked.
- The access token in the Authorization header will be blacklisted.
- After logout, both tokens become invalid and cannot be used for further requests.

---

## 📋 Task Management Endpoints

### Requires: `ROLE_USER` or `ROLE_ADMIN`

| Method | Endpoint                       | Description                        | Authorization |
|--------|--------------------------------|------------------------------------|---------------|
| GET    | `/api/tasks/my`                | Get tasks assigned to current user | Bearer Token  |
| GET    | `/api/tasks/candidate`         | Get tasks available to claim       | Bearer Token  |
| POST   | `/api/tasks/{taskId}/claim`    | Claim a candidate task             | Bearer Token  |
| POST   | `/api/tasks/{taskId}/complete` | Complete an assigned task          | Bearer Token  |

**Example: Get My Tasks**

```bash
curl -X GET http://localhost:8080/api/tasks/my \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Example: Claim Task**

```bash
curl -X POST http://localhost:8080/api/tasks/task-123/claim \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Example: Complete Task**

```bash
curl -X POST http://localhost:8080/api/tasks/task-123/complete \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"variables": {"approved": true, "comment": "Approved"}}'
```

---

## 🔄 Process Management Endpoints

### Process Deployment (Admin Only)

| Method | Endpoint                | Description                          | Authorization               |
|--------|-------------------------|--------------------------------------|-----------------------------|
| POST   | `/api/processes/deploy` | Deploy a BPMN 2.0 process definition | `ROLE_ADMIN` + Bearer Token |

**Example: Deploy BPMN File**

```bash
curl -X POST http://localhost:8080/api/processes/deploy \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@leave-request.bpmn20.xml"
```

### Process Instance Management

| Method | Endpoint                            | Description                               | Authorization                              |
|--------|-------------------------------------|-------------------------------------------|--------------------------------------------|
| POST   | `/api/processes/start`              | Start a new process instance              | `ROLE_USER` or `ROLE_ADMIN` + Bearer Token |
| GET    | `/api/processes/active`             | List active process instances             | `ROLE_ADMIN` + Bearer Token                |
| POST   | `/api/processes/{id}/suspend`       | Suspend a process instance                | `ROLE_ADMIN` + Bearer Token                |
| POST   | `/api/processes/{id}/activate`      | Activate a suspended process instance     | `ROLE_ADMIN` + Bearer Token                |
| DELETE | `/api/processes/{id}`               | Delete a process instance                 | `ROLE_ADMIN` + Bearer Token                |
| GET    | `/api/processes/completed`          | List completed process instances          | `ROLE_ADMIN` + Bearer Token                |
| GET    | `/api/processes/{id}/history/tasks` | Get historic tasks for a process instance | `ROLE_ADMIN` + Bearer Token                |

**Example: Start Process Instance**

```bash
curl -X POST http://localhost:8080/api/processes/start \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "leaveRequestProcess",
    "businessKey": "leave-001",
    "variables": {
      "reason": "Vacation",
      "duration": 5
    }
  }'
```

**Example: Get Active Processes**

```bash
curl -X GET http://localhost:8080/api/processes/active \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Example: Suspend Process**

```bash
curl -X POST http://localhost:8080/api/processes/{processInstanceId}/suspend \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Example: Delete Process**

```bash
curl -X DELETE "http://localhost:8080/api/processes/{processInstanceId}?reason=No%20longer%20needed" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📚 API Documentation & Swagger

### Swagger UI (Interactive API Documentation)

| URL                                     | Description                                                                             |
|-----------------------------------------|-----------------------------------------------------------------------------------------|
| `http://localhost:8080/swagger-ui.html` | **Main Swagger UI Interface** - Interactive API documentation with "Try it out" feature |

**Features:**

- View all available endpoints
- Test endpoints directly from the browser
- See request/response schemas
- Authenticate using JWT token (click "Authorize" button)

### OpenAPI Documentation

| URL                                      | Description                    |
|------------------------------------------|--------------------------------|
| `http://localhost:8080/api/docs`         | OpenAPI JSON specification     |
| `http://localhost:8080/v3/api-docs`      | OpenAPI 3.0 JSON specification |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI 3.0 YAML specification |

**Example: Get OpenAPI JSON**

```bash
curl http://localhost:8080/api/docs
```

---

## 🗄️ Database Console

### H2 Database Console

| URL                                | Description                                             |
|------------------------------------|---------------------------------------------------------|
| `http://localhost:8080/h2-console` | **H2 Database Web Console** - Access in-memory database |

**Connection Settings:**

- **JDBC URL:** `jdbc:h2:mem:workflow`
- **Username:** `sa`
- **Password:** (leave empty)

**Useful Tables:**

- `ACT_RU_TASK` - Runtime tasks
- `ACT_RU_EXECUTION` - Process executions
- `ACT_RU_PROCESSINST` - Process instances
- `ACT_HI_PROCINST` - Historic process instances
- `ACT_HI_TASKINST` - Historic task instances
- `ACT_ID_USER` - Flowable users
- `ACT_ID_GROUP` - Flowable groups
- `ACT_ID_MEMBERSHIP` - User-group memberships

---

## 🔍 Health & Monitoring Endpoints

### Spring Boot Actuator

| Method | Endpoint           | Description               |
|--------|--------------------|---------------------------|
| GET    | `/actuator/health` | Application health status |
| GET    | `/actuator/info`   | Application information   |

**Example: Check Health**

```bash
curl http://localhost:8080/actuator/health
```

**Example Response:**

```json
{
  "status": "UP"
}
```

---

## 👥 Default Test Users

| Username  | Password  | Roles                     | Candidate Groups       | Use Case                          |
|-----------|-----------|---------------------------|------------------------|-----------------------------------|
| `admin`   | `admin`   | `ROLE_ADMIN`, `ROLE_USER` | `managers`, `hr_staff` | Full access, can deploy processes |
| `user`    | `user`    | `ROLE_USER`               | `employees`            | Regular user, can start processes |
| `manager` | `manager` | `ROLE_USER`               | `managers`             | Can claim manager approval tasks  |
| `hr`      | `hr`      | `ROLE_USER`               | `hr_staff`             | Can claim HR approval tasks       |

---

## 🧪 Complete Testing Workflow Example

### 1. Login as Admin

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.refreshToken')

echo "Access Token: $TOKEN"
echo "Refresh Token: $REFRESH_TOKEN"
```

### 1a. Refresh Access Token (when access token expires)

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
echo "New Access Token: $TOKEN"
```

### 2. Start a Process Instance

```bash
curl -X POST http://localhost:8080/api/processes/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "leaveRequestProcess",
    "businessKey": "leave-001",
    "variables": {
      "reason": "Vacation",
      "duration": 5
    }
  }'
```

### 3. Get Active Processes

```bash
curl -X GET http://localhost:8080/api/processes/active \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Login as Manager

```bash
MANAGER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "manager", "password": "manager"}' \
  | jq -r '.accessToken')
```

### 5. Get Candidate Tasks

```bash
curl -X GET http://localhost:8080/api/tasks/candidate \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 6. Claim a Task

```bash
curl -X POST http://localhost:8080/api/tasks/{taskId}/claim \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 7. Complete the Task

```bash
curl -X POST http://localhost:8080/api/tasks/{taskId}/complete \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"variables": {"approved": true}}'
```

### 8. Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

---

## 📝 Notes

1. **JWT Token Format:** All protected endpoints require the JWT token in the Authorization header:
   ```
   Authorization: Bearer <your-access-token>
   ```

2. **Token Expiration:**
   - **Access Token**: Expires after 120 minutes (2 hours) - configurable in `application.yml`
   - **Refresh Token**: Expires after 7 days - configurable in `application.yml`
   - Use the refresh token to obtain a new access token before it expires

3. **Token Refresh:** 
   - When the access token expires, use `/api/auth/refresh` with the refresh token
   - This returns a new access token without requiring re-login
   - Refresh tokens are long-lived (7 days) for convenience

4. **Token Blacklisting:**
   - When you logout, the access token is blacklisted
   - Blacklisted tokens are rejected even if they are still valid (not expired)
   - This provides security against token theft and allows immediate logout

5. **Token Storage:** 
   - Store both `accessToken` and `refreshToken` securely in the frontend
   - Use the access token for API requests
   - Use the refresh token when the access token expires

6. **Content-Type:**
    - JSON endpoints: `Content-Type: application/json`
    - File upload: `Content-Type: multipart/form-data`

7. **Error Responses:** All endpoints return standard HTTP status codes:
    - `200 OK` - Success
    - `400 Bad Request` - Invalid request
    - `401 Unauthorized` - Missing or invalid token
    - `403 Forbidden` - Insufficient permissions
    - `404 Not Found` - Resource not found
    - `500 Internal Server Error` - Server error

5. **Swagger UI Authentication:**
    - Click the "Authorize" button in Swagger UI
    - Enter: `Bearer <your-jwt-token>`
    - Click "Authorize" to enable authenticated requests

---

## 🔗 Quick Links Summary

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api/docs
- **H2 Console:** http://localhost:8080/h2-console
- **Health Check:** http://localhost:8080/actuator/health
- **Login Endpoint:** http://localhost:8080/api/auth/login

