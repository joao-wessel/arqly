package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ActivityVisibility;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ActivityEventPublisher {
    private final ApplicationEventPublisher publisher;

    public ActivityEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId, UUID authorId, String authorName,
                        ActivityType type, String title, String description) {
        publisher.publishEvent(new ActivityRequestedEvent(tenantId, projectId, null, null, null, phaseId, stageId, authorId, authorName,
                type, ActivityVisibility.INTERNAL, title, description, null));
    }

    public void publishDocument(UUID tenantId, UUID projectId, UUID proposalId, UUID clientId, UUID generatedDocumentId,
                                UUID authorId, String authorName, ActivityType type, String title,
                                String description, String metadata) {
        publisher.publishEvent(new ActivityRequestedEvent(tenantId, projectId, clientId, proposalId, generatedDocumentId,
                null, null, authorId, authorName, type, ActivityVisibility.INTERNAL, title, description, metadata));
    }

    public void publishFile(UUID tenantId, UUID projectId, UUID phaseId, UUID stageId, UUID proposalId, UUID clientId,
                            UUID authorId, String authorName, ActivityType type, String title,
                            String description, String metadata) {
        publisher.publishEvent(new ActivityRequestedEvent(tenantId, projectId, clientId, proposalId, null,
                phaseId, stageId, authorId, authorName, type, ActivityVisibility.INTERNAL,
                title, description, metadata));
    }

    public void publishClient(UUID tenantId, UUID projectId, UUID clientId, UUID proposalId, UUID generatedDocumentId,
                              UUID authorId, String authorName, ActivityType type, String title,
                              String description, String metadata) {
        publisher.publishEvent(new ActivityRequestedEvent(tenantId, projectId, clientId, proposalId, generatedDocumentId,
                null, null, authorId, authorName, type, ActivityVisibility.CLIENT_VISIBLE,
                title, description, metadata));
    }
}
