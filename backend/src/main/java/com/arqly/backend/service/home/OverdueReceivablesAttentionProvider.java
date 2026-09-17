package com.arqly.backend.service.home;

import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import org.springframework.stereotype.Component;

@Component
public class OverdueReceivablesAttentionProvider extends AbstractOverdueFinancialAttentionProvider {
    public OverdueReceivablesAttentionProvider(FinancialInstallmentRepository installments) {
        super(installments, FinancialEntryType.RECEIVABLE, "OVERDUE_RECEIVABLE", "Conta a receber vencida");
    }
}
