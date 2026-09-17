package com.arqly.backend.service.home;

import com.arqly.backend.dto.FinancialDtos.CashFlowResponse;
import com.arqly.backend.dto.HomeDtos.HomeFinancialSummaryResponse;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.service.FinancialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeFinancialQueryService {
    private final FinancialService financial;
    public HomeFinancialQueryService(FinancialService financial) { this.financial = financial; }
    @Transactional(readOnly = true)
    public HomeFinancialSummaryResponse get(HomeUserContext context) {
        if (!context.isTenantAdmin()) throw new BusinessException("Você não possui permissão para visualizar o resumo financeiro.");
        CashFlowResponse flow = financial.cashFlow(context.tenantId());
        return new HomeFinancialSummaryResponse(flow.expectedIncome(), flow.actualIncome(), flow.expectedExpense(), flow.actualExpense(), flow.expectedBalance(), flow.actualBalance());
    }
}
