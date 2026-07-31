package com.arqly.backend.service.document;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ResponsibleVariableProvider extends AbstractMapVariableProvider {
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "responsavel.nome", context -> context.responsible() == null ? "" : value(context.responsible().getName()),
            "responsavel.email", context -> context.responsible() == null ? "" : value(context.responsible().getEmail())
    );

    protected Map<String, String> aliases() {
        return Map.of("responsible.name", "responsavel.nome", "responsible.email", "responsavel.email");
    }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }
    public List<DocumentVariableDefinition> definitions() {
        return List.of(
                definition("Responsável", "Nome", "responsavel.nome", "Nome do responsável pelo projeto"),
                definition("Responsável", "E-mail", "responsavel.email", "E-mail do responsável")
        );
    }
}
