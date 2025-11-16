# Architecture Flow Diagram - Java Spring Boot + Flowable BPMN + H2

## System Architecture Flow

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Browser/API Client]
        Swagger[Swagger UI]
    end

    subgraph "Spring Boot Application"
        subgraph "Spring Security Layer"
            JWTFilter[JWT Authentication Filter]
            SecurityConfig[Security Configuration]
        end
        
        subgraph "REST Controllers"
            AuthController[AuthController<br/>/api/auth/login]
            ProcessController[ProcessController<br/>/api/processes/**]
            TaskController[TaskController<br/>/api/tasks/**]
        end
        
        subgraph "Service Layer"
            AuthService[AuthService]
            WorkflowService[WorkflowService]
            UserContextService[UserContextService]
        end
        
        subgraph "Repository Layer"
            UserRepo[InMemoryUserAccountRepository<br/>Java Map Storage]
            FlowableIdentity[FlowableIdentityInitializer<br/>Syncs users to Flowable]
        end
        
        subgraph "Flowable Engine"
            ProcessEngine[ProcessEngine]
            RuntimeService[RuntimeService]
            TaskService[TaskService]
            HistoryService[HistoryService]
            RepositoryService[RepositoryService]
            IdentityService[IdentityService]
        end
    end

    subgraph "Data Layer"
        DataSource[Spring DataSource Bean<br/>Auto-configured from application.yml]
        H2DB[(H2 In-Memory Database<br/>jdbc:h2:mem:workflow)]
        
        subgraph "Flowable Tables in H2"
            ACT_RU[ACT_RU_*<br/>Runtime Tables]
            ACT_HI[ACT_HI_*<br/>History Tables]
            ACT_ID[ACT_ID_*<br/>Identity Tables]
            ACT_RE[ACT_RE_*<br/>Repository Tables]
        end
    end

    subgraph "Configuration Files"
        AppYml[application.yml<br/>DataSource Config]
        PomXml[pom.xml<br/>Dependencies]
        BPMNFile[BPMN Files<br/>leave-request.bpmn20.xml]
    end

    %% Client to Controllers
    Browser -->|HTTP Requests| JWTFilter
    Swagger -->|API Docs| JWTFilter
    JWTFilter -->|Validated Requests| AuthController
    JWTFilter -->|Validated Requests| ProcessController
    JWTFilter -->|Validated Requests| TaskController

    %% Controllers to Services
    AuthController --> AuthService
    ProcessController --> WorkflowService
    TaskController --> WorkflowService
    WorkflowService --> UserContextService

    %% Services to Repositories
    AuthService --> UserRepo
    WorkflowService --> ProcessEngine
    FlowableIdentity --> IdentityService

    %% Flowable Services
    ProcessEngine --> RuntimeService
    ProcessEngine --> TaskService
    ProcessEngine --> HistoryService
    ProcessEngine --> RepositoryService
    ProcessEngine --> IdentityService

    %% Flowable to DataSource
    ProcessEngine -->|Uses| DataSource
    DataSource -->|Connects to| H2DB

    %% H2 Tables
    H2DB --> ACT_RU
    H2DB --> ACT_HI
    H2DB --> ACT_ID
    H2DB --> ACT_RE

    %% Configuration
    AppYml -->|Configures| DataSource
    PomXml -->|Provides| ProcessEngine
    BPMNFile -->|Deployed via| RepositoryService

    %% Security
    SecurityConfig -->|Configures| JWTFilter

    style H2DB fill:#e1f5ff
    style ProcessEngine fill:#fff4e1
    style DataSource fill:#e8f5e9
    style UserRepo fill:#fce4ec
```

## Detailed Component Flow

```mermaid
sequenceDiagram
    participant Client
    participant JWTFilter
    participant Controller
    participant Service
    participant Flowable
    participant DataSource
    participant H2

    Note over Client,H2: Application Startup
    DataSource->>H2: Spring Boot creates DataSource from application.yml
    Flowable->>DataSource: Flowable starter injects DataSource
    Flowable->>H2: Creates ACT_* tables (database-schema-update: true)
    
    Note over Client,H2: User Login Flow
    Client->>JWTFilter: POST /api/auth/login
    JWTFilter->>Controller: AuthController.login()
    Controller->>Service: AuthService.authenticate()
    Service->>Service: InMemoryUserAccountRepository.findByUsername()
    Service->>Service: BCryptPasswordEncoder.matches()
    Service->>Service: JwtTokenProvider.generateToken()
    Service->>Client: Returns JWT token
    
    Note over Client,H2: Process Deployment Flow
    Client->>JWTFilter: POST /api/processes/deploy (with JWT)
    JWTFilter->>JWTFilter: Validate JWT token
    JWTFilter->>Controller: ProcessController.deployProcess()
    Controller->>Service: WorkflowService.deployProcess()
    Service->>Flowable: RepositoryService.createDeployment()
    Flowable->>DataSource: Execute SQL INSERT
    DataSource->>H2: Store in ACT_RE_DEPLOYMENT, ACT_RE_PROCDEF
    
    Note over Client,H2: Start Process Flow
    Client->>JWTFilter: POST /api/processes/start (with JWT)
    JWTFilter->>Controller: ProcessController.startProcess()
    Controller->>Service: WorkflowService.startProcess()
    Service->>Flowable: RuntimeService.startProcessInstanceByKey()
    Flowable->>DataSource: Execute SQL INSERT
    DataSource->>H2: Store in ACT_RU_PROCESSINST, ACT_RU_EXECUTION
    
    Note over Client,H2: Query Tasks Flow
    Client->>JWTFilter: GET /api/tasks/my (with JWT)
    JWTFilter->>Controller: TaskController.getMyTasks()
    Controller->>Service: WorkflowService.getMyTasks()
    Service->>Flowable: TaskService.createTaskQuery()
    Flowable->>DataSource: Execute SQL SELECT
    DataSource->>H2: Query ACT_RU_TASK
    H2->>DataSource: Return results
    DataSource->>Flowable: Return Task objects
    Flowable->>Service: Return List<Task>
    Service->>Controller: Return List<TaskDto>
    Controller->>Client: JSON response
```

## Data Flow Diagram

```mermaid
flowchart LR
    subgraph "Input Sources"
        BPMN[BPMN XML Files]
        API[API Requests]
        Config[application.yml]
    end

    subgraph "Spring Boot Layer"
        AutoConfig[Spring Boot<br/>Auto-Configuration]
        DataSourceBean[DataSource Bean]
    end

    subgraph "Flowable Layer"
        Engine[ProcessEngine]
        Services[Flowable Services<br/>Runtime/Task/History]
    end

    subgraph "H2 Database"
        Tables[ACT_* Tables]
    end

    Config -->|1. Reads config| AutoConfig
    AutoConfig -->|2. Creates| DataSourceBean
    DataSourceBean -->|3. Injects| Engine
    Engine -->|4. Uses| Services
    Services -->|5. Executes SQL| Tables
    BPMN -->|6. Deployed via| Services
    API -->|7. Triggers| Services

    style Config fill:#e3f2fd
    style AutoConfig fill:#fff3e0
    style DataSourceBean fill:#e8f5e9
    style Engine fill:#fce4ec
    style Tables fill:#e1f5ff
```

## Connection Details

```mermaid
graph TD
    subgraph "1. Configuration Phase"
        YML[application.yml]
        YML -->|spring.datasource.url| URL[jdbc:h2:mem:workflow]
        YML -->|spring.datasource.driver| Driver[org.h2.Driver]
        YML -->|spring.datasource.username| User[sa]
    end

    subgraph "2. Spring Boot Auto-Configuration"
        SpringBoot[Spring Boot Starter]
        SpringBoot -->|Detects H2 in classpath| H2Detect[H2 Auto-Configuration]
        H2Detect -->|Creates| DSBean[DataSource Bean]
        URL --> DSBean
        Driver --> DSBean
        User --> DSBean
    end

    subgraph "3. Flowable Integration"
        FlowableStarter[flowable-spring-boot-starter-process]
        FlowableStarter -->|Injects DataSource| ProcessEngine[ProcessEngine]
        DSBean --> ProcessEngine
        ProcessEngine -->|database-schema-update: true| SchemaUpdate[Creates ACT_* Tables]
    end

    subgraph "4. Runtime Connection"
        RuntimeService[RuntimeService]
        TaskService[TaskService]
        HistoryService[HistoryService]
        ProcessEngine --> RuntimeService
        ProcessEngine --> TaskService
        ProcessEngine --> HistoryService
        RuntimeService -->|JDBC Connection| DSBean
        TaskService -->|JDBC Connection| DSBean
        HistoryService -->|JDBC Connection| DSBean
        DSBean -->|SQL Queries| H2DB[(H2 Database)]
    end

    style YML fill:#e3f2fd
    style DSBean fill:#e8f5e9
    style ProcessEngine fill:#fff3e0
    style H2DB fill:#e1f5ff
```

## Component Dependencies

```mermaid
graph LR
    subgraph "Maven Dependencies (pom.xml)"
        JDBC[spring-boot-starter-jdbc]
        H2[h2 database]
        Flowable[flowable-spring-boot-starter-process]
    end

    subgraph "Spring Beans"
        DS[DataSource]
        PE[ProcessEngine]
        RS[RuntimeService]
        TS[TaskService]
        HS[HistoryService]
    end

    subgraph "Application Code"
        WS[WorkflowService]
        PC[ProcessController]
    end

    JDBC -->|Provides| DS
    H2 -->|Runtime| DS
    Flowable -->|Creates| PE
    DS -->|Injected into| PE
    PE -->|Provides| RS
    PE -->|Provides| TS
    PE -->|Provides| HS
    RS -->|Used by| WS
    TS -->|Used by| WS
    HS -->|Used by| WS
    WS -->|Called by| PC

    style DS fill:#e8f5e9
    style PE fill:#fff3e0
    style WS fill:#fce4ec
```

## Key Points

1. **Spring Boot Auto-Configuration**: Automatically creates DataSource from `application.yml`
2. **Flowable Integration**: Flowable starter injects DataSource into ProcessEngine
3. **Table Creation**: Flowable creates all `ACT_*` tables automatically on startup
4. **No JPA Required**: Uses JDBC directly (no JPA/Hibernate for Flowable)
5. **User Accounts**: Stored in-memory (Java Map), NOT in H2
6. **Connection Pool**: Spring Boot manages connection pool automatically

