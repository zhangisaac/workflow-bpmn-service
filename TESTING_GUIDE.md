# Testing Guide

Complete guide for testing the Workflow BPMN Service, covering unit tests, integration tests, E2E tests, code coverage, and test reporting.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Testing Strategy Overview](#testing-strategy-overview)
3. [Unit Tests](#unit-tests)
4. [Integration Tests](#integration-tests)
5. [E2E Tests](#e2e-tests)
6. [Code Coverage (JaCoCo)](#code-coverage-jacoco)
7. [Test Reports](#test-reports)
8. [Running Tests](#running-tests)
9. [CI/CD Integration](#cicd-integration)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Run All Tests

```bash
cd backend
mvn test
```

### Run with Coverage

```bash
cd backend
mvn clean test jacoco:report
# View report: backend/target/site/jacoco/index.html
```

### View Test Summary

```bash
./view-test-summary.sh
```

### Run E2E Tests

```bash
# Start server first
cd backend
mvn spring-boot:run

# In another terminal
./e2e-test.sh
```

---

## Testing Strategy Overview

This project uses a **multi-layered testing approach** with three distinct test types:

### Test Types Comparison

| Aspect | Unit Tests | Integration Tests | E2E Tests |
|--------|-----------|------------------|-----------|
| **Naming** | `*Test.java` | `*IntegrationTest.java` | Shell scripts |
| **Purpose** | Test individual components | Test request/response cycles | Test complete workflows |
| **Environment** | Mocked dependencies | Full Spring context | Real server |
| **HTTP** | N/A | MockMvc (simulated) | Real HTTP (curl) |
| **Database** | N/A | In-memory H2 (fresh per test) | Real H2 (shared state) |
| **State** | Isolated | Isolated per test | Shared across tests |
| **Speed** | Very Fast | Fast | Slower |
| **Coverage** | Business logic | Controllers, security | User journeys |
| **CI/CD** | ✅ Automated | ✅ Automated | ⚠️ Manual/Scheduled |
| **Run Command** | `mvn test` | `mvn verify` | `./e2e-test.sh` |

### Test Pyramid

```
        /\
       /E2E\        ← Few, slow, expensive
      /------\
     /Integration\  ← Some, medium speed
    /------------\
   /   Unit Tests  \ ← Many, fast, cheap
  /----------------\
```

- **Many Unit Tests** - Fast, test business logic
- **Some Integration Tests** - Test API contracts
- **Few E2E Tests** - Test critical workflows

---

## Unit Tests

### Purpose

Test individual components in isolation with mocked dependencies.

### Characteristics

- **Fast execution** - No Spring context, no database
- **Isolated** - Each test is independent
- **Focused** - Test single methods/classes
- **Mocked dependencies** - Use Mockito for external dependencies

### Example

```java
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock
    private TaskService taskService;
    
    @InjectMocks
    private WorkflowService workflowService;
    
    @Test
    void testClaimTask_Success() {
        // Test business logic with mocked dependencies
    }
}
```

### Run

```bash
mvn test
```

### Test Files

- `*Test.java` (e.g., `WorkflowServiceTest.java`, `AuthServiceTest.java`)

---

## Integration Tests

### Purpose

Test complete request/response cycles with full Spring Boot application context.

### Characteristics

- **Full Spring context** - `@SpringBootTest` loads entire application
- **MockMvc** - Simulated HTTP requests/responses
- **In-memory H2** - Fresh database per test
- **Test profile** - Uses `application-test.yml`
- **Complete stack** - Controllers, Services, Security, Database

### Example

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testLogin_Success() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk());
    }
}
```

### Run

```bash
# Run all tests (unit + integration)
mvn test

# Run only integration tests
mvn verify
# or
mvn failsafe:integration-test

# Run specific integration test
mvn test -Dtest=AuthControllerIntegrationTest
```

### Test Files

- `*IntegrationTest.java` (e.g., `AuthControllerIntegrationTest.java`, `ProcessControllerIntegrationTest.java`)

### Available Integration Tests

1. **AuthControllerIntegrationTest** - Authentication flows (login, refresh, logout)
2. **ProcessControllerIntegrationTest** - Process management (deploy, start, suspend, activate)
3. **TaskControllerIntegrationTest** - Task management (claim, complete, list)

### Running in IntelliJ IDEA

1. **Run all integration tests:**
   - Right-click on test package → "Run 'Tests in 'package''"
   - Or use shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

2. **Run specific test:**
   - Click green ▶️ icon next to class/method
   - Or right-click → "Run 'TestClassName'"

3. **Create run configuration:**
   - Run → Edit Configurations... → + → JUnit
   - Test kind: `All in package`
   - Package: `com.example.workflow.controller`
   - Pattern: `*IntegrationTest`

---

## E2E Tests

### Purpose

Test complete workflows against a real running server with real HTTP requests.

### Characteristics

- **Real server** - Requires `mvn spring-boot:run`
- **Real HTTP** - Uses curl or HTTP clients
- **Real database** - Shared state across tests
- **Production-like** - Uses `application.yml` (default profile)
- **Complete workflows** - Tests user journeys end-to-end

### Scripts

- **`e2e-test.sh`** - Comprehensive workflow test
- **`e2e-test-simple.sh`** - Quick smoke test

### Example Workflow

The E2E scripts test:
- ✅ Login (admin and manager)
- ✅ Start process instance
- ✅ Get active processes
- ✅ Get candidate tasks
- ✅ Claim task
- ✅ Complete task
- ✅ Token refresh
- ✅ Logout
- ✅ Token blacklisting

### Run

```bash
# Step 1: Start server
cd backend
mvn spring-boot:run

# Step 2: Run E2E test (in another terminal)
./e2e-test.sh
# or
./e2e-test-simple.sh
```

### Prerequisites

- Server must be running on `http://localhost:8080`
- `jq` must be installed (for JSON parsing)
  - macOS: `brew install jq`
  - Or use `QUICK_INSTALL_JQ.sh` script

### Manual E2E Testing

You can also test manually using curl:

```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | jq -r '.accessToken')

# Use token for authenticated requests
curl -X GET http://localhost:8080/api/tasks/my \
  -H "Authorization: Bearer $TOKEN"
```

---

## Code Coverage (JaCoCo)

### Overview

JaCoCo measures code coverage from **Unit Tests + Integration Tests**:

- **Overall Coverage**: 90%
- **Security**: 99%
- **Services**: 88%
- **Controllers**: 64% (target: 80%+)
- **Repository**: 100%
- **Config**: 96%

### Generate Coverage Report

```bash
cd backend

# Run tests and generate report
mvn clean test jacoco:report

# Or just generate report (if tests already run)
mvn jacoco:report
```

### View Coverage Report

```bash
# macOS
open backend/target/site/jacoco/index.html

# Linux
xdg-open backend/target/site/jacoco/index.html

# Windows
start backend/target/site/jacoco/index.html
```

### Coverage Report Locations

- **HTML Report**: `backend/target/site/jacoco/index.html`
- **XML Report**: `backend/target/site/jacoco/jacoco.xml`
- **CSV Report**: `backend/target/site/jacoco/jacoco.csv`
- **Raw Execution Data**: `backend/target/jacoco.exec`

### Understanding the Report

**HTML Report Structure**:
1. **Package View**: Shows coverage by package
2. **Class View**: Shows coverage by class
3. **Source View**: Shows line-by-line coverage with color coding:
   - **Green**: Fully covered lines
   - **Yellow**: Partially covered lines (branches)
   - **Red**: Not covered lines

**Coverage Metrics**:
- **Line Coverage**: Percentage of lines executed
- **Branch Coverage**: Percentage of branches executed
- **Instruction Coverage**: Percentage of bytecode instructions executed
- **Method Coverage**: Percentage of methods executed
- **Class Coverage**: Percentage of classes executed

### Quick Coverage Script

Use the helper script:

```bash
./run-jacoco.sh
```

This script:
- Runs tests with coverage
- Generates reports
- Checks coverage thresholds
- Opens the HTML report

### Coverage Thresholds

Current configuration has a minimum coverage threshold of 0.0 (no enforcement). To check coverage:

```bash
cd backend
mvn jacoco:check
```

---

## Test Reports

### Viewing Test Execution Summaries

After running tests, you can view summaries in multiple ways:

#### Method 1: Quick Summary Script

```bash
./view-test-summary.sh
```

This displays:
- Overall summary (total tests, failures, errors, skipped)
- Per-class breakdown
- Success rate
- Failed classes (if any)

#### Method 2: Console Output

```bash
cd backend
mvn test
```

Console shows:
```
[INFO] Tests run: 147, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Method 3: Surefire Reports

**Location**: `backend/target/surefire-reports/`

**View text reports**:
```bash
cd backend/target/surefire-reports
cat *.txt

# View summary only
grep "Tests run:" *.txt
```

**View XML reports**:
```bash
cat TEST-*.xml
```

#### Method 4: IntelliJ IDEA Test Runner

1. **Run tests** in IntelliJ IDEA
2. **View results** in Test Runner window:
   - Shows: Passed/Failed/Skipped counts
   - Execution time per test
   - Failure messages and stack traces
   - Click on failed tests to see details

#### Method 5: Maven Site Reports

```bash
cd backend
mvn site
open target/site/index.html
```

### Report Locations

- **Text reports**: `backend/target/surefire-reports/*.txt`
- **XML reports**: `backend/target/surefire-reports/TEST-*.xml`
- **Failsafe summary**: `backend/target/failsafe-reports/failsafe-summary.xml`
- **Coverage reports**: `backend/target/site/jacoco/index.html`

---

## Running Tests

### Command Line

```bash
cd backend

# Run all tests (unit + integration)
mvn test

# Run with coverage
mvn clean test jacoco:report

# Run only integration tests
mvn verify
# or
mvn failsafe:integration-test

# Run specific test class
mvn test -Dtest=WorkflowServiceTest

# Run specific test method
mvn test -Dtest=WorkflowServiceTest#testClaimTask_Success
```

### IntelliJ IDEA

**Run All Tests**:
1. Right-click on `src/test/java` → "Run 'All Tests'"
2. Or use shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

**Run Specific Test**:
1. Click green ▶️ icon next to class/method
2. Or right-click → "Run 'TestClassName'"

**Run Configuration**:
1. Run → Edit Configurations... → + → JUnit
2. Configure test scope and filters
3. Save and run

### Quick Reference Commands

| Action | Command |
|--------|---------|
| Run all tests | `mvn test` |
| Run with coverage | `mvn test jacoco:report` |
| View test summary | `./view-test-summary.sh` |
| View coverage | `open backend/target/site/jacoco/index.html` |
| Run integration tests | `mvn verify` |
| Run specific test | `mvn test -Dtest=TestClassName` |
| Start server | `mvn spring-boot:run` |
| Run E2E tests | `./e2e-test.sh` |
| Run quick E2E | `./e2e-test-simple.sh` |

---

## CI/CD Integration

### GitHub Actions

The CI/CD pipeline (`.github/workflows/bugbot.yml`) runs:

1. **Unit Tests** - `mvn test`
2. **Integration Tests** - `mvn verify`
3. **Code Coverage** - JaCoCo report generation
4. **Static Analysis** - SpotBugs, PMD, Checkstyle
5. **Security Scanning** - OWASP Dependency-Check, CodeQL

### E2E Tests in CI/CD

E2E tests are **not automatically run** in CI/CD because:
- They require a running server
- They are slower
- They test real-world scenarios

**Recommendation**: Run E2E tests:
- Before releases
- On a schedule (nightly/weekly)
- Manually for smoke testing

---

## Best Practices

### 1. Test Naming

- **Unit Tests**: `*Test.java` (e.g., `WorkflowServiceTest.java`)
- **Integration Tests**: `*IntegrationTest.java` (e.g., `AuthControllerIntegrationTest.java`)
- **E2E Tests**: Shell scripts (e.g., `e2e-test.sh`)

### 2. Test Isolation

- **Unit Tests**: Completely isolated, no shared state
- **Integration Tests**: Isolated per test, fresh database
- **E2E Tests**: Shared state, requires cleanup between runs

### 3. Test Data

- **Unit Tests**: Use test fixtures, mocks
- **Integration Tests**: Use `@BeforeEach` to set up test data
- **E2E Tests**: Use real user accounts, may need cleanup

### 4. Test Coverage Goals

- **Target**: 80%+ overall coverage
- **Critical paths**: 100% coverage
- **Review uncovered code** regularly

### 5. Test Organization

- Keep tests close to source code
- Use descriptive test method names
- Follow AAA pattern (Arrange, Act, Assert)
- One assertion per test (when possible)

---

## Troubleshooting

### Issue: Tests fail with "ClassNotFoundException"

**Solution**:
```bash
cd backend
mvn clean compile test-compile
mvn test
```

### Issue: Integration tests fail with database errors

**Solution**:
- Check `application-test.yml` configuration
- Ensure H2 is properly configured
- Verify test profile is active: `@ActiveProfiles("test")`

### Issue: E2E tests fail with connection errors

**Solution**:
- Ensure server is running: `mvn spring-boot:run`
- Check server is on `http://localhost:8080`
- Verify `jq` is installed: `which jq`

### Issue: Coverage not showing

**Solution**:
```bash
cd backend
mvn clean test jacoco:report
# Check: target/site/jacoco/index.html exists
```

### Issue: Test reports not found

**Solution**:
- Run tests first: `mvn test`
- Check: `backend/target/surefire-reports/` exists
- Verify build succeeded

### Issue: Slow test execution

**Solution**:
- Run specific tests: `mvn test -Dtest=TestClassName`
- Check for unnecessary Spring context loading
- Review test setup/teardown overhead

---

## Summary

This project uses a **comprehensive multi-layered testing strategy**:

1. ✅ **Unit Tests** - Fast, isolated, test business logic
2. ✅ **Integration Tests** - Full Spring context, test API contracts (90% coverage)
3. ✅ **E2E Tests** - Real server, test complete workflows

All three layers are important and serve complementary purposes:
- **Unit Tests** ensure individual components work correctly
- **Integration Tests** ensure components work together (measured by coverage)
- **E2E Tests** ensure the system works in real-world scenarios

### Current Coverage

- **Overall**: 90% (JaCoCo)
- **Security**: 99%
- **Services**: 88%
- **Controllers**: 64% (target: 80%+)
- **Repository**: 100%
- **Config**: 96%

### Quick Commands

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View test summary
./view-test-summary.sh

# View coverage
open backend/target/site/jacoco/index.html

# Run E2E tests
./e2e-test.sh
```

---

## Helper Scripts

- **`view-test-summary.sh`** - View test execution summary
- **`run-jacoco.sh`** - Run tests with coverage and open report
- **`e2e-test.sh`** - Comprehensive E2E test
- **`e2e-test-simple.sh`** - Quick E2E smoke test

---

## Support

For more information:
- **Testing Strategy**: See this guide
- **Architecture**: See [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)
- **API Documentation**: See [API_ENDPOINTS.md](API_ENDPOINTS.md)

