# SonarQube Integration Guide

Complete guide for setting up and using SonarQube with this project, including SonarQube Cloud, Maven, IntelliJ IDEA, and dashboard reference.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Overview](#overview)
3. [SonarQube Cloud Setup](#sonarqube-cloud-setup)
4. [Maven Configuration](#maven-configuration)
5. [IntelliJ IDEA Setup](#intellij-idea-setup)
6. [Running Analysis](#running-analysis)
7. [Dashboard Guide](#dashboard-guide)
8. [Project Key Format](#project-key-format)
9. [CI/CD Integration](#cicd-integration)
10. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites Checklist

- [ ] SonarQube Cloud account (https://sonarcloud.io/)
- [ ] SonarQube token generated
- [ ] Organization key: `zhangisaac`
- [ ] IntelliJ IDEA with SonarLint plugin (optional)

### 5-Minute Setup

1. **Sign up**: https://sonarcloud.io/
2. **Create project**: 
   - Project Key: `zhangisaac:workflow-bpmn-service`
   - Organization: `zhangisaac`
3. **Generate token**: My Account → Security → Generate Token
4. **Configure Maven**: Edit `~/.m2/settings.xml` (see [Maven Configuration](#maven-configuration))
5. **Run analysis**: `cd backend && mvn clean test jacoco:report sonar:sonar`

**Dashboard**: https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service

---

## Overview

SonarQube provides comprehensive code quality analysis including:

- ✅ **Vulnerability Assessment (VA)**: Security vulnerabilities and security hotspots
- ✅ **Manifest/Dependency Issues**: Dependency vulnerabilities and version issues
- ✅ **Static Bug Testing (SBT)**: Bugs from SpotBugs, PMD, Checkstyle
- ✅ **Unit Test Coverage**: Test coverage metrics (from JaCoCo)
- ✅ **Code Smells**: Maintainability issues
- ✅ **Duplications**: Code duplication detection
- ✅ **Technical Debt**: Estimated time to fix issues

---

## SonarQube Cloud Setup

### Step 1: Create Account

1. **Go to**: https://sonarcloud.io/
2. **Sign up** with GitHub, GitLab, Bitbucket, or email
3. **Create an organization** (or use existing)
   - Organization Key: `zhangisaac`

### Step 2: Create Project

1. **Click**: "Create Project"
2. **Select**: "Manually"
3. **Configure**:
   - Project Key: `zhangisaac:workflow-bpmn-service`
   - Display Name: `Workflow BPMN Service`
   - Organization: `zhangisaac`
   - Visibility: Public or Private

### Step 3: Generate Token

1. **Go to**: My Account → Security
2. **Generate Token**:
   - Name: `workflow-bpmn-service-token`
   - Type: "Global Analysis Token" or "Project Analysis Token"
   - Expires: Set expiration (or leave blank for no expiration)
   - Click "Generate"
3. **Copy the token** (you won't see it again!)
4. **Save securely** (you'll need it for Maven and IntelliJ)

### Configuration Summary

- **Server URL**: `https://sonarcloud.io`
- **Organization**: `zhangisaac`
- **Project Key**: `zhangisaac:workflow-bpmn-service`
- **Dashboard**: https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service

---

## Maven Configuration

### Step 1: Configure Maven Settings

Edit `~/.m2/settings.xml` (create if it doesn't exist):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">
    <profiles>
        <profile>
            <id>sonar</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <!-- SonarQube Cloud -->
                <sonar.host.url>https://sonarcloud.io</sonar.host.url>
                <sonar.organization>zhangisaac</sonar.organization>
                <sonar.login>YOUR_SONAR_TOKEN</sonar.login>
            </properties>
        </profile>
    </profiles>
</settings>
```

**Replace** `YOUR_SONAR_TOKEN` with your actual token.

### Step 2: Verify Configuration

```bash
cd backend
mvn sonar:sonar -Dsonar.projectKey=zhangisaac:workflow-bpmn-service
```

This will:
1. Run tests with coverage
2. Generate JaCoCo reports
3. Upload results to SonarQube Cloud
4. Show analysis URL in console

### Project Configuration

The project is already configured in:
- `backend/pom.xml` - SonarQube Maven plugin and properties
- `backend/sonar-project.properties` - Project-specific settings

---

## IntelliJ IDEA Setup

### Step 1: Install SonarLint Plugin

1. **Open IntelliJ IDEA**
2. **Go to**: File → Settings (Windows/Linux) or IntelliJ IDEA → Preferences (macOS)
   - Shortcut: `Ctrl+Alt+S` (Windows/Linux) or `Cmd+,` (macOS)
3. **Navigate**: Plugins
4. **Marketplace Tab**: Click "Marketplace"
5. **Search**: Type "SonarLint"
6. **Install**: Click "Install" on "SonarLint" by SonarSource
7. **Restart**: Click "Restart IDE" when prompted

### Step 2: Connect to SonarQube Cloud

1. **Open Settings**: File → Settings → Tools → SonarLint
2. **SonarQube Connections**: Click "SonarQube Connections" tab
3. **Add Connection**: Click "+" → Select "SonarQube"
4. **Configure Connection**:
   ```
   Connection Name: SonarQube Cloud
   Server URL: https://sonarcloud.io
   Token: [Paste your SonarQube token here]
   Organization: zhangisaac
   ```
5. **Test Connection**: Click "Test Connection"
   - ✅ Should show: "Connection successful"
6. **Save**: Click "OK" to save

### Step 3: Bind Project

1. **Open Settings**: File → Settings → Tools → SonarLint → Project Settings
2. **SonarQube Connection**: Select your connection (e.g., "SonarQube Cloud")
3. **Project Key**: Enter `zhangisaac:workflow-bpmn-service`
4. **Apply**: Click "Apply" and "OK"

### Step 4: Use SonarLint

**Automatic Analysis** (Real-time):
- SonarLint automatically analyzes files as you edit them
- Issues appear as warnings/errors in the editor
- View issues in the "SonarLint" tool window

**Manual Analysis**:
1. **Right-click** on a file/folder/project
2. **Select**: SonarLint → Analyze with SonarLint
3. **View Results**: Check the "SonarLint" tool window

### Step 5: Maven Run Configuration

1. **Run** → **Edit Configurations...**
2. **Add** → **Maven**
3. **Configure**:
   ```
   Name: SonarQube Analysis
   Working directory: backend
   Command line: sonar:sonar
   ```
4. **Before launch**:
   - Click "+" → "Run Maven Goal"
   - Goal: `clean test jacoco:report`
   - Working directory: `backend`
5. **Save**: Click "OK"

---

## Running Analysis

### Command Line

```bash
cd backend

# Run analysis (with tests and coverage)
mvn clean test jacoco:report sonar:sonar

# Or just analysis (if tests already run)
mvn sonar:sonar
```

### IntelliJ IDEA

1. **Select**: "SonarQube Analysis" from run configurations
2. **Run**: Click green ▶️ button
3. **Console**: Watch console for analysis progress
4. **URL**: Look for SonarQube dashboard URL in console output

### After Analysis

You'll see in the console:
```
ANALYSIS SUCCESSFUL
https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service
```

Open the URL in your browser to view the dashboard.

---

## Dashboard Guide

### Accessing the Dashboard

**URL**: https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service

### Dashboard Sections

#### 1. Overview Tab

**Quality Gate Status**:
- ✅ **Passed**: All quality gate conditions met
- ❌ **Failed**: One or more conditions not met

**Key Metrics**:

| Metric | Description | Target |
|--------|-------------|--------|
| **Bugs** 🔴 | Code that is demonstrably wrong | 0 |
| **Vulnerabilities** 🟠 | Security vulnerabilities (VA) | 0 |
| **Security Hotspots** 🟡 | Security-sensitive code to review | Review |
| **Code Smells** 🟢 | Maintainability issues | Minimize |
| **Coverage** 📊 | Test coverage percentage | 80%+ |
| **Duplications** 📋 | Code duplication percentage | < 3% |

**Quality Gate Conditions** (configurable):
- Coverage ≥ 80%
- Duplications ≤ 3%
- Maintainability Rating ≤ A
- Reliability Rating ≤ A
- Security Rating ≤ A
- Bugs ≤ threshold
- Vulnerabilities ≤ threshold

#### 2. Issues Tab

**Filter by Type**:
- **Vulnerabilities (VA)**: Security vulnerabilities
- **Bugs (SBT)**: Static bug testing issues
- **Code Smells**: Maintainability issues
- **Security Hotspots**: Security review items

**Filter by Severity**:
- **Critical**: Must fix immediately
- **Major**: Should fix soon
- **Minor**: Nice to fix
- **Info**: Informational

**Common Issue Categories**:

**Vulnerability Assessment (VA)**:
- SQL Injection (`java:S2083`)
- XSS (`java:S5131`)
- Hardcoded Secrets (`java:S2068`)
- Insecure Random (`java:S2245`)

**Manifest/Dependency Issues**:
- Vulnerable Dependencies (`java:S5725`)
- Outdated Dependencies (`java:S6830`)
- License Issues (`java:S6831`)

**Static Bug Testing (SBT)**:
- Null Pointer Exception (`java:S2259`)
- Resource Leak (`java:S2095`)
- Logic Error (`java:S1141`)
- Exception Handling (`java:S1181`)

#### 3. Measures Tab

**Coverage Metrics**:
- Overall Coverage: 90% (from JaCoCo)
- Line Coverage: Percentage of lines covered
- Branch Coverage: Percentage of branches covered
- Condition Coverage: Percentage of conditions covered

**Other Metrics**:
- Duplications: Code duplication metrics
- Technical Debt: Estimated time to fix issues
- Maintainability Rating: A (best) to E (worst)
- Reliability Rating: A (best) to E (worst)
- Security Rating: A (best) to E (worst)

#### 4. Code Tab

- Browse source code
- View issues per file
- View coverage per file

#### 5. Activity Tab

- Analysis history over time
- Trends: Coverage, issues, duplications

### Dashboard URLs

- **Overview**: https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service
- **Issues**: https://sonarcloud.io/project/issues?id=zhangisaac:workflow-bpmn-service
- **Measures**: https://sonarcloud.io/component_measures?id=zhangisaac:workflow-bpmn-service
- **Code**: https://sonarcloud.io/code?id=zhangisaac:workflow-bpmn-service
- **Activity**: https://sonarcloud.io/project/activity?id=zhangisaac:workflow-bpmn-service

---

## Project Key Format

### Correct Format

**Use `zhangisaac:workflow-bpmn-service` (with colon) everywhere.**

### Explanation

**In Configuration Files**:
- Use colon format: `zhangisaac:workflow-bpmn-service`
- `backend/pom.xml` - `sonar.projectKey` property
- `backend/sonar-project.properties` - `sonar.projectKey` property
- Maven command line: `-Dsonar.projectKey=zhangisaac:workflow-bpmn-service`
- IntelliJ IDEA SonarLint project binding

**In URLs**:
- SonarQube Cloud handles URL encoding automatically
- Use colon format: `https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service`
- The colon gets URL-encoded to `%3A` automatically
- Underscore format also works but is less standard

### Summary

| Location | Format | Example |
|----------|--------|---------|
| Configuration | Colon | `zhangisaac:workflow-bpmn-service` |
| URLs | Colon (recommended) | `zhangisaac:workflow-bpmn-service` |
| Command Line | Colon | `-Dsonar.projectKey=zhangisaac:workflow-bpmn-service` |

**Bottom line**: Use `zhangisaac:workflow-bpmn-service` (colon) everywhere for consistency.

---

## CI/CD Integration

### GitHub Actions

The CI/CD pipeline (`.github/workflows/bugbot.yml`) includes SonarQube analysis.

### Required Secrets

Add these secrets to your GitHub repository:

1. **Go to**: Repository → Settings → Secrets and variables → Actions
2. **Add Secrets**:
   - `SONAR_TOKEN`: Your SonarQube token
   - `SONAR_ORGANIZATION`: `zhangisaac`
   - `SONAR_HOST_URL`: `https://sonarcloud.io` (optional)

### Workflow

The SonarQube job:
1. Builds and tests the project
2. Generates JaCoCo coverage reports
3. Runs SonarQube analysis
4. Checks quality gate
5. Uploads reports as artifacts

---

## Troubleshooting

### Issue: "Authentication failed"

**Solutions**:
1. Check SonarQube token is correct in `~/.m2/settings.xml`
2. Verify token hasn't expired
3. Check internet connection
4. For IntelliJ: Check SonarLint connection settings

### Issue: "Project not found"

**Solutions**:
1. Verify project key: `zhangisaac:workflow-bpmn-service`
2. Check organization key: `zhangisaac`
3. Ensure project exists in SonarQube Cloud
4. Verify you have access to the project

### Issue: "Coverage not showing"

**Solutions**:
1. Ensure JaCoCo reports are generated: `mvn test jacoco:report`
2. Check `target/site/jacoco/jacoco.xml` exists
3. Verify `sonar.coverage.jacoco.xmlReportPaths` in `pom.xml`

### Issue: SonarLint not analyzing files

**Solutions**:
1. Check SonarLint plugin is installed and enabled
2. Verify SonarQube connection is configured
3. Check project is bound to SonarQube
4. Restart IntelliJ IDEA
5. Check SonarLint tool window for errors

### Issue: "Connection timeout"

**Solutions**:
1. Check internet connection
2. Verify SonarQube Cloud is accessible: `curl https://sonarcloud.io`
3. Check firewall/proxy settings

---

## Quick Reference

### Commands

```bash
# Run analysis
cd backend
mvn sonar:sonar

# With tests and coverage
mvn clean test jacoco:report sonar:sonar

# View dashboard
open https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service
```

### Configuration

- **Organization**: `zhangisaac`
- **Project Key**: `zhangisaac:workflow-bpmn-service`
- **Server URL**: `https://sonarcloud.io`
- **Token**: Configured in `~/.m2/settings.xml`

### Important URLs

- **Dashboard**: https://sonarcloud.io/dashboard?id=zhangisaac:workflow-bpmn-service
- **Issues**: https://sonarcloud.io/project/issues?id=zhangisaac:workflow-bpmn-service
- **Measures**: https://sonarcloud.io/component_measures?id=zhangisaac:workflow-bpmn-service

---

## Summary

After completing this setup, you'll have:

✅ **SonarQube Cloud** connected  
✅ **Maven** configured for analysis  
✅ **IntelliJ IDEA** configured with SonarLint  
✅ **CI/CD** integrated  
✅ **Dashboard** showing all metrics:
   - Vulnerability Assessment (VA)
   - Manifest/Dependency Issues
   - Static Bug Testing (SBT) Issues
   - Unit Test Coverage
   - Code Smells
   - Duplications
   - Technical Debt

Your SonarQube dashboard will be a **golden reference** for code quality across the entire SDLC! 🎯

---

## Support

- **SonarQube Documentation**: https://docs.sonarqube.org/
- **SonarQube Cloud**: https://sonarcloud.io/
- **SonarLint Documentation**: https://www.sonarlint.org/

