package com.arqly.backend.service.home;

import com.arqly.backend.entity.FinancialEntryType;
import com.arqly.backend.repository.FinancialInstallmentRepository;
import org.springframework.stereotype.Component;

@Component
public class OverduePayablesAttentionProvider extends AbstractOverdueFinancialAttentionProvider {
    public OverduePayablesAttentionProvider(FinancialInstallmentRepository installments) {
        super(installments, FinancialEntryType.PAYABLE, "OVERDUE_PAYABLE", "Conta a pagar vencida");
    }
}
