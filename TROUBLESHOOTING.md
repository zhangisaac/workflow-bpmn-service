# Troubleshooting Guide - 403 Forbidden Errors

## Issue: Getting 403 errors when accessing URLs in browser

### Solution Applied

I've updated the `SecurityConfig.java` to use both `AntPathRequestMatcher` and `MvcRequestMatcher` for Swagger UI and
API documentation paths. This ensures proper path matching regardless of how Spring Security processes the requests.

### Steps to Verify

1. **Restart the application** after the security configuration changes:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Try accessing these URLs in your browser** (should work without authentication):
    - `http://localhost:8080/swagger-ui.html`
    - `http://localhost:8080/api/docs`
    - `http://localhost:8080/v3/api-docs`
    - `http://localhost:8080/actuator/health`
    - `http://localhost:8080/h2-console`

### If Still Getting 403 Errors

#### Option 1: Check Application Logs

Look for security-related errors in the console output when you try to access the URLs.

#### Option 2: Test with cURL

Test if the endpoints are actually accessible:

```bash
# Test Swagger UI (should return HTML)
curl -v http://localhost:8080/swagger-ui.html

# Test OpenAPI docs (should return JSON)
curl -v http://localhost:8080/api/docs

# Test health endpoint
curl -v http://localhost:8080/actuator/health
```

#### Option 3: Check Browser Console

Open browser developer tools (F12) and check:

- Network tab: What status code is returned?
- Console tab: Any JavaScript errors?
- Response: What does the 403 response body say?

#### Option 4: Verify SpringDoc Configuration

Check if SpringDoc is properly configured. The paths in `application.yml` should match:

```yaml
springdoc:
  api-docs:
    path: /api/docs
  swagger-ui:
    path: /swagger-ui.html
```

### Alternative: Access Swagger UI via Different Path

Sometimes SpringDoc redirects to a different path. Try these variations:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/swagger-ui/`
- `http://localhost:8080/swagger-ui.html`

### For API Endpoints (Require Authentication)

All API endpoints (except `/api/auth/login`) require a JWT token. You **cannot** test them directly in a browser without
authentication.

**To test API endpoints:**

1. **First, get a JWT token:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin"}'
   ```

2. **Copy the `accessToken` from the response**

3. **Use the token in subsequent requests:**
   ```bash
   curl -X GET http://localhost:8080/api/tasks/my \
     -H "Authorization: Bearer YOUR_TOKEN_HERE"
   ```

4. **Or use Swagger UI:**
    - Go to `http://localhost:8080/swagger-ui.html`
    - Click the "Authorize" button (lock icon)
    - Enter: `Bearer YOUR_TOKEN_HERE`
    - Click "Authorize"
    - Now you can test endpoints directly in Swagger UI

### Common Issues

#### Issue: Swagger UI loads but shows "Failed to load API definition"

**Solution:** Check if `/api/docs` or `/v3/api-docs` is accessible. Swagger UI needs to fetch the OpenAPI spec.

#### Issue: 403 on all endpoints including Swagger

**Solution:**

1. Verify the application restarted after security config changes
2. Check if there are any errors in application startup logs
3. Try clearing browser cache and cookies

#### Issue: Can access Swagger but API calls return 403

**Solution:** This is expected! You need to:

1. Login via `/api/auth/login` to get a token
2. Use the token in the Authorization header
3. Or use Swagger UI's "Authorize" feature

### Quick Test Script

Save this as `test-endpoints.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "Testing public endpoints..."
echo ""

echo "1. Health Check:"
curl -s "$BASE_URL/actuator/health" | jq .
echo ""

echo "2. OpenAPI Docs:"
curl -s "$BASE_URL/api/docs" | jq . | head -20
echo ""

echo "3. Login to get token:"
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}' \
  | jq -r '.accessToken')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo "Token obtained: ${TOKEN:0:50}..."
  echo ""
  
  echo "4. Test authenticated endpoint:"
  curl -s -X GET "$BASE_URL/api/tasks/my" \
    -H "Authorization: Bearer $TOKEN" | jq .
else
  echo "Failed to get token!"
fi
```

Make it executable and run:

```bash
chmod +x test-endpoints.sh
./test-endpoints.sh
```

### Still Having Issues?

1. **Check application logs** for security filter chain information
2. **Verify SpringDoc dependency** is in `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.2.0</version>
   </dependency>
   ```
3. **Check if port 8080 is correct** - verify in `application.yml`
4. **Try accessing from different browser** or incognito mode

