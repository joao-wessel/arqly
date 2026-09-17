package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import com.arqly.backend.entity.ApprovalStatus;
import com.arqly.backend.repository.ApprovalRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PendingApprovalsAttentionProvider implements AttentionProvider {
    private final ApprovalRepository approvals;
    public PendingApprovalsAttentionProvider(ApprovalRepository approvals) { this.approvals = approvals; }
    public List<AttentionItemResponse> provide(HomeUserContext context) {
        return approvals.findTop20ByTenantIdAndStatusAndDeletedFalseOrderByDeadlineAsc(context.tenantId(), ApprovalStatus.PENDING).stream()
                .filter(approval -> HomeAccess.canSee(context, approval.getProject())).limit(10)
                .map(approval -> new AttentionItemResponse("PENDING_APPROVAL", "HIGH", "Aprovação pendente", approval.getDescription(),
                        approval.getProject().getId(), approval.getProject().getName(), approval.getId(),
                        "/app/projects/" + approval.getProject().getId() + "/approvals/" + approval.getId(), approval.getDeadline())).toList();
    }
}
