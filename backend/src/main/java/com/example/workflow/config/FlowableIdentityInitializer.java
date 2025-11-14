package com.example.workflow.config;

import com.example.workflow.model.UserAccount;
import com.example.workflow.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Initializes Flowable Identity Service with users and groups from the in-memory repository.
 * This ensures that Flowable's identity service is synchronized with the application's user accounts
 * at startup, enabling proper task assignment and candidate group matching.
 */
@Component
public class FlowableIdentityInitializer {

    private static final Logger logger = LoggerFactory.getLogger(FlowableIdentityInitializer.class);

    private final IdentityService identityService;
    private final UserAccountRepository userAccountRepository;

    public FlowableIdentityInitializer(ProcessEngine processEngine,
                                       UserAccountRepository userAccountRepository) {
        this.identityService = processEngine.getIdentityService();
        this.userAccountRepository = userAccountRepository;
    }

    @PostConstruct
    public void initializeFlowableIdentities() {
        logger.info("Initializing Flowable identity service with users and groups...");

        try {
            Collection<UserAccount> accounts = userAccountRepository.findAll();

            // Collect all unique groups from all users
            Set<String> allGroups = accounts.stream()
                    .flatMap(account -> account.groups().stream())
                    .collect(java.util.stream.Collectors.toSet());

            // Create groups in Flowable
            for (String groupId : allGroups) {
                createGroupIfNotExists(groupId);
            }

            // Create users and link them to groups
            for (UserAccount account : accounts) {
                createUserIfNotExists(account);
                linkUserToGroups(account.username(), account.groups());
            }

            logger.info("Successfully initialized Flowable identity service with {} users and {} groups",
                    accounts.size(), allGroups.size());
        } catch (Exception e) {
            logger.error("Failed to initialize Flowable identity service", e);
            throw new RuntimeException("Failed to initialize Flowable identity service", e);
        }
    }

    private void createUserIfNotExists(UserAccount account) {
        User existingUser = identityService.createUserQuery()
                .userId(account.username())
                .singleResult();

        if (existingUser == null) {
            User user = identityService.newUser(account.username());
            user.setFirstName(account.username());
            user.setLastName("");
            user.setEmail(account.username() + "@example.com");
            // Note: Flowable IdentityService doesn't store passwords for authentication
            // Authentication is handled by Spring Security, not Flowable
            identityService.saveUser(user);
            logger.debug("Created Flowable user: {}", account.username());
        } else {
            logger.debug("Flowable user already exists: {}", account.username());
        }
    }

    private void createGroupIfNotExists(String groupId) {
        Group existingGroup = identityService.createGroupQuery()
                .groupId(groupId)
                .singleResult();

        if (existingGroup == null) {
            Group group = identityService.newGroup(groupId);
            group.setName(groupId);
            group.setType("assignment");
            identityService.saveGroup(group);
            logger.debug("Created Flowable group: {}", groupId);
        } else {
            logger.debug("Flowable group already exists: {}", groupId);
        }
    }

    private void linkUserToGroups(String username, Set<String> groups) {
        for (String groupId : groups) {
            // Check if membership already exists by querying group memberships
            boolean membershipExists = identityService.createGroupQuery()
                    .groupMember(username)
                    .groupId(groupId)
                    .count() > 0;

            if (!membershipExists) {
                identityService.createMembership(username, groupId);
                logger.debug("Linked user {} to group {}", username, groupId);
            } else {
                logger.debug("Membership already exists: user {} in group {}", username, groupId);
            }
        }
    }
}

