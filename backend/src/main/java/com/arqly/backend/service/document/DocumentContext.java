package com.arqly.backend.service.document;

import com.arqly.backend.entity.Client;
import com.arqly.backend.entity.Project;
import com.arqly.backend.entity.Proposal;
import com.arqly.backend.entity.Tenant;
import com.arqly.backend.entity.TenantUser;
import java.time.LocalDate;

public record DocumentContext(
        Tenant tenant,
        Client client,
        Project project,
        Proposal proposal,
        TenantUser responsible,
        LocalDate currentDate
) {}
