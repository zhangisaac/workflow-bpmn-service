#!/bin/bash

# Simple E2E Test Script - Quick Version
# Tests basic authentication and API access

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Check if jq is installed, try common locations
if ! command -v jq &> /dev/null; then
    if [ -f "$HOME/bin/jq" ]; then
        export PATH="$HOME/bin:$PATH"
    elif [ -f "/usr/local/bin/jq" ]; then
        export PATH="/usr/local/bin:$PATH"
    fi
fi

# Final check
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed."
    echo "Run: ./QUICK_INSTALL_JQ.sh"
    exit 1
fi

echo "=== Quick E2E Test ==="
echo ""

# Check server
echo "1. Checking server health..."
if curl -s -f "$BASE_URL/actuator/health" > /dev/null; then
    echo "✓ Server is running"
else
    echo "✗ Server is not running. Start with: cd backend && mvn spring-boot:run"
    exit 1
fi

# Login
echo ""
echo "2. Logging in as admin..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "✗ Login failed"
    exit 1
fi

echo "✓ Login successful"
echo "  Token: ${TOKEN:0:50}..."

# Test protected endpoint
echo ""
echo "3. Testing protected endpoint..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/processes/active" \
  -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Protected endpoint accessible"
else
    echo "✗ Protected endpoint failed (HTTP $HTTP_CODE)"
fi

# Refresh token
echo ""
echo "4. Testing token refresh..."
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken // empty')
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken // empty')

if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
    echo "✓ Token refresh successful"
else
    echo "✗ Token refresh failed"
fi

# Logout
echo ""
echo "5. Testing logout..."
LOGOUT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

if [ "$LOGOUT_CODE" = "200" ]; then
    echo "✓ Logout successful"
else
    echo "✗ Logout failed (HTTP $LOGOUT_CODE)"
fi

echo ""
echo "=== Test Complete ==="

