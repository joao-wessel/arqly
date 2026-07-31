package com.arqly.backend.service.document;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractMapVariableProvider implements DocumentVariableProvider {
    protected abstract Map<String, Function<DocumentContext, String>> variables();
    protected Map<String, String> aliases() { return Map.of(); }

    @Override
    public boolean supports(String variable) {
        return variables().containsKey(variable) || aliases().containsKey(variable);
    }

    @Override
    public Optional<String> resolve(String variable, DocumentContext context) {
        var resolver = variables().get(aliases().getOrDefault(variable, variable));
        return resolver == null ? Optional.empty() : Optional.ofNullable(resolver.apply(context)).map(String::valueOf);
    }

    @Override
    public abstract List<DocumentVariableDefinition> definitions();

    protected DocumentVariableDefinition definition(String group, String label, String variable, String description) {
        return new DocumentVariableDefinition(group, label, "{{" + variable + "}}", description);
    }

    protected String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
