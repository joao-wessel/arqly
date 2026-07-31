package com.arqly.backend.service.document;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ProposalVariableProvider extends AbstractMapVariableProvider {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "proposta.numero", context -> context.proposal() == null ? "" : value(context.proposal().getNumber()),
            "proposta.titulo", context -> context.proposal() == null ? "" : value(context.proposal().getTitle()),
            "proposta.descricao", context -> context.proposal() == null ? "" : value(context.proposal().getDescription()),
            "proposta.valorTotal", context -> context.proposal() == null || context.proposal().getTotal() == null ? "" : NumberFormat.getNumberInstance(new Locale("pt", "BR")).format(context.proposal().getTotal()),
            "proposta.validade", context -> context.proposal() == null || context.proposal().getValidUntil() == null ? "" : DATE.format(context.proposal().getValidUntil())
    );

    protected Map<String, String> aliases() {
        return Map.of("proposal.number", "proposta.numero", "proposal.title", "proposta.titulo",
                "proposal.description", "proposta.descricao", "proposal.totalValue", "proposta.valorTotal",
                "proposal.validUntil", "proposta.validade");
    }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }
    public List<DocumentVariableDefinition> definitions() {
        return List.of(
                definition("Proposta", "Número", "proposta.numero", "Número da proposta"),
                definition("Proposta", "Título", "proposta.titulo", "Título da proposta"),
                definition("Proposta", "Descrição", "proposta.descricao", "Descrição comercial"),
                definition("Proposta", "Valor total", "proposta.valorTotal", "Valor total formatado"),
                definition("Proposta", "Validade", "proposta.validade", "Data de validade")
        );
    }
}
