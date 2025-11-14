# API Endpoints & Testing URLs

Base URL: `http://localhost:8080`

## 🔐 Authentication Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/api/auth/login` | Authenticate user and receive JWT token | `{"username": "admin", "password": "admin"}` |

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}'
```

**Example Response:**
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2025-11-14T14:00:00Z",
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

---

## 📋 Task Management Endpoints

### Requires: `ROLE_USER` or `ROLE_ADMIN`

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| GET | `/api/tasks/my` | Get tasks assigned to current user | Bearer Token |
| GET | `/api/tasks/candidate` | Get tasks available to claim | Bearer Token |
| POST | `/api/tasks/{taskId}/claim` | Claim a candidate task | Bearer Token |
| POST | `/api/tasks/{taskId}/complete` | Complete an assigned task | Bearer Token |

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

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/api/processes/deploy` | Deploy a BPMN 2.0 process definition | `ROLE_ADMIN` + Bearer Token |

**Example: Deploy BPMN File**
```bash
curl -X POST http://localhost:8080/api/processes/deploy \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@leave-request.bpmn20.xml"
```

### Process Instance Management

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| POST | `/api/processes/start` | Start a new process instance | `ROLE_USER` or `ROLE_ADMIN` + Bearer Token |
| GET | `/api/processes/active` | List active process instances | `ROLE_ADMIN` + Bearer Token |
| POST | `/api/processes/{id}/suspend` | Suspend a process instance | `ROLE_ADMIN` + Bearer Token |
| POST | `/api/processes/{id}/activate` | Activate a suspended process instance | `ROLE_ADMIN` + Bearer Token |
| DELETE | `/api/processes/{id}` | Delete a process instance | `ROLE_ADMIN` + Bearer Token |
| GET | `/api/processes/completed` | List completed process instances | `ROLE_ADMIN` + Bearer Token |
| GET | `/api/processes/{id}/history/tasks` | Get historic tasks for a process instance | `ROLE_ADMIN` + Bearer Token |

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

| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | **Main Swagger UI Interface** - Interactive API documentation with "Try it out" feature |

**Features:**
- View all available endpoints
- Test endpoints directly from the browser
- See request/response schemas
- Authenticate using JWT token (click "Authorize" button)

### OpenAPI Documentation

| URL | Description |
|-----|-------------|
| `http://localhost:8080/api/docs` | OpenAPI JSON specification |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 JSON specification |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI 3.0 YAML specification |

**Example: Get OpenAPI JSON**
```bash
curl http://localhost:8080/api/docs
```

---

## 🗄️ Database Console

### H2 Database Console

| URL | Description |
|-----|-------------|
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

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Application health status |
| GET | `/actuator/info` | Application information |

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

| Username | Password | Roles | Candidate Groups | Use Case |
|----------|----------|-------|------------------|----------|
| `admin` | `admin` | `ROLE_ADMIN`, `ROLE_USER` | `managers`, `hr_staff` | Full access, can deploy processes |
| `user` | `user` | `ROLE_USER` | `employees` | Regular user, can start processes |
| `manager` | `manager` | `ROLE_USER` | `managers` | Can claim manager approval tasks |
| `hr` | `hr` | `ROLE_USER` | `hr_staff` | Can claim HR approval tasks |

---

## 🧪 Complete Testing Workflow Example

### 1. Login as Admin
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}' \
  | jq -r '.accessToken')
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

---

## 📝 Notes

1. **JWT Token Format:** All protected endpoints require the JWT token in the Authorization header:
   ```
   Authorization: Bearer <your-jwt-token>
   ```

2. **Token Expiration:** JWT tokens expire after 120 minutes (configurable in `application.yml`)

3. **Content-Type:** 
   - JSON endpoints: `Content-Type: application/json`
   - File upload: `Content-Type: multipart/form-data`

4. **Error Responses:** All endpoints return standard HTTP status codes:
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

