package com.example.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void testWorkflowOpenApi() {
        // When
        OpenAPI openAPI = config.workflowOpenApi();

        // Then
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Workflow BPMN Service API", openAPI.getInfo().getTitle());
        assertEquals("REST APIs for the simplified workflow management system", openAPI.getInfo().getDescription());
        assertEquals("v1", openAPI.getInfo().getVersion());

        // Check security scheme
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        SecurityScheme bearerAuth = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(bearerAuth);
        assertEquals(SecurityScheme.Type.HTTP, bearerAuth.getType());
        assertEquals("bearer", bearerAuth.getScheme());
        assertEquals("JWT", bearerAuth.getBearerFormat());

        // Check security requirement
        assertNotNull(openAPI.getSecurity());
        assertEquals(1, openAPI.getSecurity().size());
        SecurityRequirement requirement = openAPI.getSecurity().get(0);
        assertTrue(requirement.containsKey("bearerAuth"));
    }

    @Test
    void testPublicApi() {
        // When
        GroupedOpenApi groupedOpenApi = config.publicApi();

        // Then
        assertNotNull(groupedOpenApi);
        assertEquals("workflow-api", groupedOpenApi.getGroup());
        assertNotNull(groupedOpenApi.getPathsToMatch());
        assertEquals(1, groupedOpenApi.getPathsToMatch().size());
        assertEquals("/api/**", groupedOpenApi.getPathsToMatch().get(0));
    }
}

