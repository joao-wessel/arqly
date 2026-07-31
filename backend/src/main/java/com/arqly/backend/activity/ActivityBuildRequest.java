package com.arqly.backend.activity;

import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ActivityVisibility;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.GeneratedDocument;
import com.arqly.backend.entity.ProjectPhase;
import com.arqly.backend.entity.ProjectStage;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.entity.TenantUser;

public record ActivityBuildRequest(
        Tenant tenant,
        Project project,
        Client client,
        Proposal proposal,
        GeneratedDocument generatedDocument,
        ProjectPhase phase,
        ProjectStage stage,
        TenantUser author,
        String authorName,
        ActivityType type,
        ActivityVisibility visibility,
        String title,
        String description,
        String metadata
) {}
