# Workflow BPMN Service

Simplified workflow management system built with Spring Boot, Flowable BPMN, and JWT-based security. The project
demonstrates a leave-request business process with role-based access control, task management, and process monitoring
APIs.

## Features

- **Workflow engine:** Flowable 7.0.1 with BPMN 2.0 deployment, execution, and history queries.
- **REST APIs:** Endpoints for process deployment, instance lifecycle control, task claiming/completion, and historical
  reporting.
- **Security:** JWT authentication with Spring Security 6, stateless sessions, and RBAC for `ROLE_ADMIN` and
  `ROLE_USER`.
- **Backend:** Spring Boot 3.1.5 with Java 17, fully compatible with Jakarta EE.
- **Documentation:** Auto-generated OpenAPI docs (Swagger UI) via SpringDoc.

## Repository Structure

```
backend/   # Spring Boot + Flowable service
```

> **Note:** The frontend Vue 3 dashboard has been moved to a separate repository at
`/Users/zhangisaac/VSCodeProjects/vue-ui`.

## Prerequisites

- Java 17+
- Maven 3.9+ (for backend)

> `mvn` must be available on your PATH. If Maven is unavailable, install it
> via [Apache Maven](https://maven.apache.org/) or your system package manager.

## Backend Setup (`backend/`)

1. Install dependencies & build:

   ```bash
   cd backend
   mvn clean package
   ```

2. Run the application:

   ```bash
   mvn spring-boot:run
   ```

   The API server listens on `http://localhost:8080`.

3. Useful endpoints:

    - Swagger UI: `http://localhost:8080/swagger-ui.html`
    - OpenAPI JSON: `http://localhost:8080/api/docs`
    - H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:workflow`)

### Default Users

| Username | Password | Roles                 | Candidate Groups   |
|----------|----------|-----------------------|--------------------|
| admin    | admin    | ROLE_ADMIN, ROLE_USER | managers, hr_staff |
| user     | user     | ROLE_USER             | employees          |
| manager  | manager  | ROLE_USER             | managers           |
| hr       | hr       | ROLE_USER             | hr_staff           |

### BPMN Leave Request Flow

Deployed automatically from `classpath:/processes/leave-request.bpmn20.xml`:

1. Employee submits a leave request (`Submit Leave Request`)
2. Manager reviews (`Manager Approval`, candidate group: `managers`)
3. HR finalises (`HR Approval`, candidate group: `hr_staff`)
4. Process completes

All endpoints require JWT authentication except `/api/auth/**` and documentation/health endpoints. Administrator-only
APIs cover process deployment and lifecycle actions.

> **Frontend:** The Vue 3 dashboard is available in a separate repository. See `/Users/zhangisaac/VSCodeProjects/vue-ui`
> for the frontend application.

## API Overview

- `POST /api/auth/login` – Authenticate and receive a JWT.
- `POST /api/processes/deploy` – Upload BPMN definition (admin only).
- `POST /api/processes/start` – Start a process instance (admin or user).
- `GET /api/processes/active` – List active instances (admin only).
- `POST /api/processes/{id}/suspend|activate` – Control instance state (admin only).
- `DELETE /api/processes/{id}` – Delete an instance (admin only).
- `GET /api/processes/completed` – View completed instances (admin only).
- `GET /api/processes/{id}/history/tasks` – Historic task breakdown (admin only).
- `GET /api/tasks/my` – Tasks assigned to current user.
- `GET /api/tasks/candidate` – Group tasks available to current user.
- `POST /api/tasks/{id}/claim` – Claim a group task.
- `POST /api/tasks/{id}/complete` – Complete an assigned task with optional variables.

Refer to Swagger UI for request/response schemas.

## Testing the Flow

1. Log in as `admin` to deploy custom BPMN definitions or start instances.
2. Log in as `user` to submit the initial task in the leave process.
3. Switch to `manager` to claim and approve the manager task.
4. Switch to `hr` to claim and complete the HR approval task.
5. Inspect completed processes and history from the admin view.

## Notes & Future Improvements

- Token refresh/blacklisting is not implemented; login again once tokens expire.
- Flowable identity data is derived from the in-memory user repository at startup. Extend the repository for persistence
  if needed.
- Add automated tests (unit/integration) around workflow orchestration and security for production readiness.

## License

MIT (see `LICENSE` if provided) or adapt to your organisational standards.
