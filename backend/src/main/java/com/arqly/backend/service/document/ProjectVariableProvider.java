package com.arqly.backend.service.document;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ProjectVariableProvider extends AbstractMapVariableProvider {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "projeto.nome", context -> context.project() == null ? "" : value(context.project().getName()),
            "projeto.codigo", context -> context.project() == null ? "" : value(context.project().getCode()),
            "projeto.descricao", context -> context.project() == null ? "" : value(context.project().getDescription()),
            "projeto.dataInicio", context -> context.project() == null || context.project().getStartDate() == null ? "" : DATE.format(context.project().getStartDate()),
            "projeto.dataConclusao", context -> context.project() == null || context.project().getExpectedEndDate() == null ? "" : DATE.format(context.project().getExpectedEndDate()),
            "projeto.responsavel.nome", context -> context.responsible() == null ? "" : value(context.responsible().getName())
    );

    protected Map<String, String> aliases() {
        return Map.of("project.name", "projeto.nome", "project.code", "projeto.codigo",
                "project.description", "projeto.descricao", "project.startDate", "projeto.dataInicio",
                "project.endDate", "projeto.dataConclusao", "project.responsibleUser.name", "projeto.responsavel.nome");
    }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }
    public List<DocumentVariableDefinition> definitions() {
        return List.of(
                definition("Projeto", "Nome", "projeto.nome", "Nome do projeto"),
                definition("Projeto", "Código", "projeto.codigo", "Código interno"),
                definition("Projeto", "Descrição", "projeto.descricao", "Descrição do projeto"),
                definition("Projeto", "Data de início", "projeto.dataInicio", "Data de início planejada"),
                definition("Projeto", "Previsão de conclusão", "projeto.dataConclusao", "Previsão de conclusão"),
                definition("Projeto", "Responsável", "projeto.responsavel.nome", "Responsável oficial pelo projeto")
        );
    }
}
