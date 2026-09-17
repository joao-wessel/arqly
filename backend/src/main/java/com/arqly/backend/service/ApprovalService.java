package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.notification.NotificationDomainPublisher;
import com.arqly.backend.dto.ApprovalDtos.ApprovalDecisionRequest;
import com.arqly.backend.dto.ApprovalDtos.ApprovalRequest;
import com.arqly.backend.dto.ApprovalDtos.ApprovalResponse;
import com.arqly.backend.entity.ActivityType;
import com.arqly.backend.entity.ActivityVisibility;
import com.arqly.backend.entity.Approval;
import com.arqly.backend.entity.ApprovalStatus;
import com.arqly.backend.entity.ClientPersonType;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.ApprovalRepository;
import com.arqly.backend.repository.GeneratedDocumentRepository;
import com.arqly.backend.repository.ProjectRepository;
import com.arqly.backend.repository.ProjectStageRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final ApprovalRepository repository;
    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final TenantUserRepository userRepository;
    private final ActivityEventPublisher activityPublisher;
    private final NotificationDomainPublisher notifications;

    public ApprovalService(ApprovalRepository repository, ProjectRepository projectRepository,
                           ProjectStageRepository stageRepository, GeneratedDocumentRepository documentRepository,
                           TenantUserRepository userRepository, ActivityEventPublisher activityPublisher,
                           NotificationDomainPublisher notifications) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.activityPublisher = activityPublisher;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> list(UUID tenantId, UUID projectId) {
        return repository.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByCreatedAtDesc(projectId, tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApprovalResponse create(UUID tenantId, UUID userId, String actor, ApprovalRequest request) {
        var project = projectRepository.findByIdAndTenantIdAndDeletedFalse(request.projectId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado."));
        var approval = new Approval();
        approval.setTenant(project.getTenant());
        approval.setProject(project);
        approval.setClient(project.getClient());
        approval.setDescription(request.description());
        approval.setDeadline(request.deadline());
        approval.setCreatedBy(userRepository.findByIdAndTenantId(userId, tenantId).orElse(null));
        if (request.stageId() != null) {
            var stage = stageRepository.findByIdAndTenantIdAndDeletedFalse(request.stageId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Etapa não encontrada."));
            if (!stage.getProjectPhase().getProject().getId().equals(project.getId())) {
                throw new BusinessException("A etapa informada não pertence ao projeto.");
            }
            approval.setStage(stage);
        }
        if (request.documentId() != null) {
            var document = documentRepository.findByIdAndTenantIdAndDeletedFalse(request.documentId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Documento não encontrado."));
            if (!document.getClient().getId().equals(project.getClient().getId())) {
                throw new BusinessException("O documento informado pertence a outro cliente.");
            }
            document.setClientVisible(true);
            approval.setDocument(document);
        }
        approval = repository.save(approval);
        publish(approval, userId, actor, "Aprovação solicitada", "solicitou aprovação do cliente");
        return toResponse(approval);
    }

    @Transactional
    public ApprovalResponse decide(UUID tenantId, UUID clientId, UUID approvalId, boolean approved, ApprovalDecisionRequest request) {
        var approval = repository.findByIdAndTenantIdAndDeletedFalse(approvalId, tenantId)
                .orElseThrow(() -> new NotFoundException("Aprovação não encontrada."));
        if (!approval.getClient().getId().equals(clientId)) {
            throw new NotFoundException("Aprovação não encontrada no portal.");
        }
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("Esta aprovação já foi respondida.");
        }
        var comment = request == null ? null : request.comment();
        if (!approved && (comment == null || comment.isBlank())) {
            throw new BusinessException("Informe o motivo para solicitar ajustes.");
        }
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        approval.setApprovedAt(Instant.now());
        approval.setApprovedBy(displayName(approval.getClient()));
        approval.setClientComment(comment);
        publish(approval, null, displayName(approval.getClient()), "Aprovação do cliente",
                approved ? "aprovou a solicitação" : "solicitou ajustes");
        if (!approved) notifications.approvalRejected(approval);
        return toResponse(approval);
    }

    ApprovalResponse toResponse(Approval approval) {
        return new ApprovalResponse(approval.getId(), approval.getProject().getId(), approval.getProject().getName(),
                approval.getStage() == null ? null : approval.getStage().getId(),
                approval.getStage() == null ? null : approval.getStage().getName(),
                approval.getDocument() == null ? null : approval.getDocument().getId(),
                approval.getDocument() == null ? null : approval.getDocument().getTitle(),
                approval.getClient().getId(), displayName(approval.getClient()), approval.getStatus(),
                approval.getDescription(), approval.getDeadline(), approval.getApprovedAt(), approval.getApprovedBy(),
                approval.getCreatedBy() == null ? "Sistema" : approval.getCreatedBy().getName(),
                approval.getClientComment(), approval.getCreatedAt(), approval.getUpdatedAt());
    }

    private void publish(Approval approval, UUID userId, String actor, String title, String description) {
        activityPublisher.publishClient(approval.getTenant().getId(), approval.getProject().getId(),
                approval.getClient().getId(), null, null, userId, actor, ActivityType.CLIENT_APPROVAL,
                title, description, "{\"approvalId\":\"" + approval.getId() + "\"}");
    }

    private String displayName(com.arqly.backend.entity.Client client) {
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? client.getName() : client.getTradeName();
    }
}
