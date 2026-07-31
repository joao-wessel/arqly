package com.arqly.backend.service.file;

import java.util.UUID;

public record FileOwnerContext(
        UUID ownerId,
        String label,
        UUID projectId,
        UUID phaseId,
        UUID stageId,
        UUID proposalId,
        UUID clientId
) {}
