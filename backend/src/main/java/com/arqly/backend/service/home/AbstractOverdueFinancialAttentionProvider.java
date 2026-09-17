package com.arqly.backend.service.home;

import com.arqly.backend.dto.HomeDtos.AttentionItemResponse;
import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.entity.FinancialInstallmentStatus;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import java.time.LocalDate;
import java.util.List;

abstract class AbstractOverdueFinancialAttentionProvider implements AttentionProvider {
    private final FinancialInstallmentRepository installments;
    private final FinancialEntryType type;
    private final String attentionType;
    private final String title;

    AbstractOverdueFinancialAttentionProvider(FinancialInstallmentRepository installments, FinancialEntryType type,
                                              String attentionType, String title) {
        this.installments = installments; this.type = type; this.attentionType = attentionType; this.title = title;
    }

    public List<AttentionItemResponse> provide(HomeUserContext context) {
        if (!context.isTenantAdmin()) return List.of();
        return installments.findAllByEntryTenantIdAndEntryDeletedFalse(context.tenantId()).stream()
                .filter(installment -> installment.getEntry().getType() == type)
                .filter(installment -> installment.getStatus() != FinancialInstallmentStatus.PAID && installment.getStatus() != FinancialInstallmentStatus.CANCELLED)
                .filter(installment -> installment.getDueDate() != null && installment.getDueDate().isBefore(LocalDate.now())).limit(10)
                .map(installment -> {
                    var entry = installment.getEntry();
                    return new AttentionItemResponse(attentionType, "HIGH", title, entry.getDescription(),
                            entry.getProject() == null ? null : entry.getProject().getId(), entry.getProject() == null ? null : entry.getProject().getName(),
                            installment.getId(), "/app/financial", installment.getDueDate());
                }).toList();
    }
}
