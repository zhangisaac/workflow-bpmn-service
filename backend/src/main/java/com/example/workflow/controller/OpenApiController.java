package com.example.workflow.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller to expose OpenAPI documentation at /v3/api-docs endpoint.
 * This ensures compatibility with standard OpenAPI 3.0 paths even when
 * springdoc.api-docs.path is customized to /api/docs.
 * <p>
 * This controller forwards the request internally to /api/docs.
 */
@RestController
@RequestMapping("/v3")
public class OpenApiController {

    /**
     * Expose OpenAPI 3.0 specification at /v3/api-docs
     * This endpoint forwards to /api/docs which returns the same OpenAPI specification
     */
    @GetMapping("/api-docs")
    public void getApiDocs(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Forward the request internally to /api/docs
            RequestDispatcher dispatcher = request.getRequestDispatcher("/api/docs");
            dispatcher.forward(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to forward request to /api/docs: " + e.getMessage());
        }
    }
}

