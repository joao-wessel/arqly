package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import com.arqly.backend.entity.ProjectStageStatus;
import com.arqly.backend.repository.ProjectStageRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OverdueStagesAttentionProvider implements AttentionProvider {
    private final ProjectStageRepository stages;
    public OverdueStagesAttentionProvider(ProjectStageRepository stages) { this.stages = stages; }
    public List<AttentionItemResponse> provide(HomeUserContext context) {
        return stages.findOverdueForHome(context.tenantId(), LocalDate.now(), List.of(ProjectStageStatus.COMPLETED, ProjectStageStatus.CANCELLED)).stream()
                .filter(stage -> HomeAccess.canSee(context, stage)).limit(10)
                .map(stage -> new AttentionItemResponse("OVERDUE_STAGE", "HIGH", "Etapa atrasada", stage.getName(),
                        stage.getProjectPhase().getProject().getId(), stage.getProjectPhase().getProject().getName(), stage.getId(),
                        "/app/projects/" + stage.getProjectPhase().getProject().getId() + "/stages/" + stage.getId(), stage.getPlannedEnd())).toList();
    }
}
