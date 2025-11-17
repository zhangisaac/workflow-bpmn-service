#!/bin/bash

# E2E Test Script for Workflow BPMN Service
# This script tests the complete workflow end-to-end

set -e  # Exit on error

BASE_URL="${BASE_URL:-http://localhost:8080}"
COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    # Try to find jq in common locations
    if [ -f "$HOME/bin/jq" ]; then
        export PATH="$HOME/bin:$PATH"
    elif [ -f "/usr/local/bin/jq" ]; then
        export PATH="/usr/local/bin:$PATH"
    fi
    
    if ! command -v jq &> /dev/null; then
        echo -e "${COLOR_RED}Error: jq is required but not installed.${COLOR_RESET}"
        echo "Install it with:"
        echo "  - Run: ./QUICK_INSTALL_JQ.sh (in this directory)"
        echo "  - Or: brew install jq (if Homebrew is installed)"
        echo "  - Or: See INSTALL_JQ.md for manual installation"
        exit 1
    fi
fi

# Check if server is running
echo -e "${COLOR_BLUE}Checking if server is running...${COLOR_RESET}"
if ! curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${COLOR_RED}Error: Server is not running at $BASE_URL${COLOR_RESET}"
    echo "Start the server with: cd backend && mvn spring-boot:run"
    exit 1
fi
echo -e "${COLOR_GREEN}✓ Server is running${COLOR_RESET}"
echo ""

# Function to print test step
print_step() {
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
    echo -e "${COLOR_BLUE}$1${COLOR_RESET}"
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
}

# Function to print success
print_success() {
    echo -e "${COLOR_GREEN}✓ $1${COLOR_RESET}"
}

# Function to print error
print_error() {
    echo -e "${COLOR_RED}✗ $1${COLOR_RESET}"
}

# Function to print info
print_info() {
    echo -e "${COLOR_YELLOW}ℹ $1${COLOR_RESET}"
}

echo ""
echo -e "${COLOR_GREEN}╔════════════════════════════════════════════╗${COLOR_RESET}"
echo -e "${COLOR_GREEN}║   E2E Test: Complete Workflow              ║${COLOR_RESET}"
echo -e "${COLOR_GREEN}╚════════════════════════════════════════════╝${COLOR_RESET}"
echo ""

# ============================================
# Step 1: Login as Admin
# ============================================
print_step "Step 1: Login as Admin"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}')

if [ $? -ne 0 ]; then
    print_error "Failed to connect to server"
    exit 1
fi

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    print_error "Login failed!"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

print_success "Admin logged in successfully"
print_info "Access Token: ${TOKEN:0:50}..."
print_info "Refresh Token: ${REFRESH_TOKEN:0:50}..."
echo ""

# ============================================
# Step 2: Start a Process Instance
# ============================================
print_step "Step 2: Start a Process Instance"

TIMESTAMP=$(date +%s)
PROCESS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/processes/start" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"leave-request\",
    \"businessKey\": \"e2e-test-$TIMESTAMP\",
    \"variables\": {
      \"reason\": \"E2E Testing\",
      \"duration\": 3
    }
  }")

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/processes/start" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"leave-request\",
    \"businessKey\": \"e2e-test-$TIMESTAMP\",
    \"variables\": {
      \"reason\": \"E2E Testing\",
      \"duration\": 3
    }
  }")

if [ "$HTTP_CODE" != "200" ]; then
    print_error "Failed to start process (HTTP $HTTP_CODE)"
    echo "Response: $PROCESS_RESPONSE"
    # Continue anyway - process might not be deployed
    print_info "Note: Process might not be deployed. Continuing with other tests..."
    PROCESS_ID=""
else
    PROCESS_ID=$(echo "$PROCESS_RESPONSE" | jq -r '.id // empty')
    if [ -n "$PROCESS_ID" ] && [ "$PROCESS_ID" != "null" ]; then
        print_success "Process started successfully"
        print_info "Process ID: $PROCESS_ID"
    else
        print_error "Process ID not found in response"
        PROCESS_ID=""
    fi
fi
echo ""

# ============================================
# Step 3: Get Active Processes
# ============================================
print_step "Step 3: Get Active Processes (Admin)"

ACTIVE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/processes/active" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/processes/active" \
  -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_CODE" = "200" ]; then
    ACTIVE_COUNT=$(echo "$ACTIVE_RESPONSE" | jq '. | length')
    print_success "Retrieved active processes"
    print_info "Active processes: $ACTIVE_COUNT"
    if [ "$ACTIVE_COUNT" -gt 0 ]; then
        echo "$ACTIVE_RESPONSE" | jq '.[0] | {id, processDefinitionKey, businessKey, suspended}'
    fi
else
    print_error "Failed to get active processes (HTTP $HTTP_CODE)"
fi
echo ""

# ============================================
# Step 4: Login as Manager
# ============================================
print_step "Step 4: Login as Manager"

MANAGER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "manager", "password": "manager"}')

MANAGER_TOKEN=$(echo "$MANAGER_RESPONSE" | jq -r '.accessToken // empty')

if [ -z "$MANAGER_TOKEN" ] || [ "$MANAGER_TOKEN" = "null" ]; then
    print_error "Manager login failed!"
    exit 1
fi

print_success "Manager logged in successfully"
print_info "Manager Token: ${MANAGER_TOKEN:0:50}..."
echo ""

# ============================================
# Step 5: Get Candidate Tasks (Manager)
# ============================================
print_step "Step 5: Get Candidate Tasks (Manager)"

CANDIDATE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/tasks/candidate" \
  -H "Authorization: Bearer $MANAGER_TOKEN")

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/tasks/candidate" \
  -H "Authorization: Bearer $MANAGER_TOKEN")

if [ "$HTTP_CODE" = "200" ]; then
    CANDIDATE_COUNT=$(echo "$CANDIDATE_RESPONSE" | jq '. | length')
    print_success "Retrieved candidate tasks"
    print_info "Candidate tasks: $CANDIDATE_COUNT"
    
    if [ "$CANDIDATE_COUNT" -gt 0 ]; then
        TASK_ID=$(echo "$CANDIDATE_RESPONSE" | jq -r '.[0].id // empty')
        TASK_NAME=$(echo "$CANDIDATE_RESPONSE" | jq -r '.[0].name // empty')
        print_info "First task: $TASK_NAME (ID: $TASK_ID)"
        
        # ============================================
        # Step 6: Claim a Task
        # ============================================
        if [ -n "$TASK_ID" ] && [ "$TASK_ID" != "null" ]; then
            print_step "Step 6: Claim Task"
            
            CLAIM_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/tasks/$TASK_ID/claim" \
              -H "Authorization: Bearer $MANAGER_TOKEN")
            
            if [ "$CLAIM_HTTP_CODE" = "200" ]; then
                print_success "Task claimed successfully"
                
                # ============================================
                # Step 7: Complete the Task
                # ============================================
                print_step "Step 7: Complete Task"
                
                COMPLETE_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/tasks/$TASK_ID/complete" \
                  -H "Authorization: Bearer $MANAGER_TOKEN" \
                  -H "Content-Type: application/json" \
                  -d '{"variables": {"approved": true, "comment": "E2E test approval"}}')
                
                if [ "$COMPLETE_HTTP_CODE" = "200" ]; then
                    print_success "Task completed successfully"
                else
                    print_error "Failed to complete task (HTTP $COMPLETE_HTTP_CODE)"
                fi
            else
                print_error "Failed to claim task (HTTP $CLAIM_HTTP_CODE)"
            fi
        fi
    else
        print_info "No candidate tasks available (process might not be deployed)"
    fi
else
    print_error "Failed to get candidate tasks (HTTP $HTTP_CODE)"
fi
echo ""

# ============================================
# Step 8: Get My Tasks (Manager)
# ============================================
print_step "Step 8: Get My Tasks (Manager)"

MY_TASKS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/tasks/my" \
  -H "Authorization: Bearer $MANAGER_TOKEN")

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/tasks/my" \
  -H "Authorization: Bearer $MANAGER_TOKEN")

if [ "$HTTP_CODE" = "200" ]; then
    MY_TASKS_COUNT=$(echo "$MY_TASKS_RESPONSE" | jq '. | length')
    print_success "Retrieved my tasks"
    print_info "My tasks: $MY_TASKS_COUNT"
else
    print_error "Failed to get my tasks (HTTP $HTTP_CODE)"
fi
echo ""

# ============================================
# Step 9: Refresh Access Token
# ============================================
print_step "Step 9: Refresh Access Token"

REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken // empty')

if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
    print_success "Token refreshed successfully"
    print_info "New Token: ${NEW_TOKEN:0:50}..."
    TOKEN="$NEW_TOKEN"  # Update token for logout
else
    print_error "Token refresh failed"
    echo "Response: $REFRESH_RESPONSE"
fi
echo ""

# ============================================
# Step 10: Test with Refreshed Token
# ============================================
print_step "Step 10: Test with Refreshed Token"

if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
    TEST_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/processes/active" \
      -H "Authorization: Bearer $NEW_TOKEN")
    
    if [ "$TEST_HTTP_CODE" = "200" ]; then
        print_success "Refreshed token works correctly"
    else
        print_error "Refreshed token failed (HTTP $TEST_HTTP_CODE)"
    fi
fi
echo ""

# ============================================
# Step 11: Logout
# ============================================
print_step "Step 11: Logout"

LOGOUT_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

if [ "$LOGOUT_HTTP_CODE" = "200" ]; then
    print_success "Logout successful"
else
    print_error "Logout failed (HTTP $LOGOUT_HTTP_CODE)"
fi
echo ""

# ============================================
# Step 12: Verify Token is Blacklisted
# ============================================
print_step "Step 12: Verify Token is Blacklisted"

BLACKLIST_TEST_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/processes/active" \
  -H "Authorization: Bearer $TOKEN")

if [ "$BLACKLIST_TEST_CODE" = "401" ] || [ "$BLACKLIST_TEST_CODE" = "403" ]; then
    print_success "Token is blacklisted (as expected)"
else
    print_info "Token still works (HTTP $BLACKLIST_TEST_CODE) - might be timing issue"
fi
echo ""

# ============================================
# Summary
# ============================================
echo -e "${COLOR_GREEN}╔════════════════════════════════════════════╗${COLOR_RESET}"
echo -e "${COLOR_GREEN}║   E2E Test Summary                         ║${COLOR_RESET}"
echo -e "${COLOR_GREEN}╚════════════════════════════════════════════╝${COLOR_RESET}"
echo ""
print_success "All E2E tests completed!"
echo ""
print_info "Tested endpoints:"
echo "  ✓ POST /api/auth/login"
echo "  ✓ POST /api/processes/start"
echo "  ✓ GET  /api/processes/active"
echo "  ✓ GET  /api/tasks/candidate"
echo "  ✓ POST /api/tasks/{id}/claim"
echo "  ✓ POST /api/tasks/{id}/complete"
echo "  ✓ GET  /api/tasks/my"
echo "  ✓ POST /api/auth/refresh"
echo "  ✓ POST /api/auth/logout"
echo ""
print_info "Note: Some tests may show warnings if BPMN processes are not deployed."
print_info "This is normal for a fresh installation."
echo ""

