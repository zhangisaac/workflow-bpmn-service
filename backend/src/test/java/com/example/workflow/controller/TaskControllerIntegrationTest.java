package com.example.workflow.controller;

import com.example.workflow.dto.TaskCompletionRequest;
import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import com.example.workflow.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskController.
 * These tests verify the full request/response cycle including security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Set up authentication context for testing
        UserAccount userAccount = userAccountRepository.findByUsername("user")
                .orElseThrow();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userAccount.username(),
                null,
                userAccount.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        authToken = jwtTokenProvider.generateToken(authentication);
    }

    @Test
    void testGetMyTasks_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/tasks/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMyTasks_Authenticated() throws Exception {
        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetCandidateTasks_Authenticated() throws Exception {
        mockMvc.perform(get("/api/tasks/candidate")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testClaimTask_InvalidTaskId() throws Exception {
        mockMvc.perform(post("/api/tasks/invalid-task-id/claim")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteTask_InvalidTaskId() throws Exception {
        TaskCompletionRequest request = new TaskCompletionRequest(Map.of("approved", true));

        mockMvc.perform(post("/api/tasks/invalid-task-id/complete")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCompleteTask_WithVariables() throws Exception {
        TaskCompletionRequest request = new TaskCompletionRequest(
                Map.of("approved", true, "comment", "Looks good")
        );

        // This will fail if no task exists, but tests the endpoint structure
        mockMvc.perform(post("/api/tasks/non-existent-task/complete")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

