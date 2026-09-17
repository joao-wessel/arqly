package com.arqly.backend.dto;

import com.arqly.backend.entity.ProjectStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HomeDtos {
    private HomeDtos() {}

    public record AttentionItemResponse(String type, String severity, String title, String description,
                                        UUID projectId, String projectName, UUID sourceId, String actionUrl,
                                        LocalDate dueDate) {}

    public record HomeProjectResponse(UUID id, String code, String name, String clientName, ProjectStatus status,
                                      BigDecimal completionPercentage, Instant updatedAt, boolean responsible,
                                      boolean manager, boolean stageResponsible) {}

    public record HomeIndicatorsResponse(long activeProjects, long inProgressStages, long overdueStages,
                                         long pendingApprovals) {}

    public record HomeFinancialSummaryResponse(BigDecimal expectedIncome, BigDecimal actualIncome,
                                               BigDecimal expectedExpense, BigDecimal actualExpense,
                                               BigDecimal expectedBalance, BigDecimal actualBalance) {}

    public record HomeActivityResponse(UUID id, String title, String description, String authorName,
                                       String type, UUID projectId, String projectName, Instant createdAt) {}

    public record HomeTodayResponse(List<CalendarDtos.CalendarItemResponse> items) {}
}
