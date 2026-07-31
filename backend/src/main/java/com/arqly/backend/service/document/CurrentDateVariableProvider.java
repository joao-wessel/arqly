package com.arqly.backend.service.document;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class CurrentDateVariableProvider extends AbstractMapVariableProvider {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "dataAtual", context -> DATE.format(context.currentDate())
    );

    protected Map<String, String> aliases() { return Map.of("currentDate", "dataAtual"); }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }
    public List<DocumentVariableDefinition> definitions() {
        return List.of(definition("Sistema", "Data atual", "dataAtual", "Data da geração do documento"));
    }
}
