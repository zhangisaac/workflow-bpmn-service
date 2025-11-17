package com.example.workflow.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenApiControllerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RequestDispatcher requestDispatcher;

    @InjectMocks
    private OpenApiController openApiController;

    @BeforeEach
    void setUp() {
        when(request.getRequestDispatcher("/api/docs")).thenReturn(requestDispatcher);
    }

    @Test
    void testGetApiDocs_Success() throws Exception {
        // When
        openApiController.getApiDocs(request, response);

        // Then
        verify(request).getRequestDispatcher("/api/docs");
        verify(requestDispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void testGetApiDocs_ForwardException() throws Exception {
        // Given
        doThrow(new RuntimeException("Forward failed")).when(requestDispatcher).forward(request, response);

        // When
        openApiController.getApiDocs(request, response);

        // Then
        verify(request).getRequestDispatcher("/api/docs");
        verify(requestDispatcher).forward(request, response);
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to forward request to /api/docs: Forward failed");
    }

    @Test
    void testGetApiDocs_IOException() throws Exception {
        // Given
        doThrow(new IOException("IO error")).when(requestDispatcher).forward(request, response);

        // When
        openApiController.getApiDocs(request, response);

        // Then
        verify(request).getRequestDispatcher("/api/docs");
        verify(requestDispatcher).forward(request, response);
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Failed to forward request to /api/docs: IO error");
    }
}

