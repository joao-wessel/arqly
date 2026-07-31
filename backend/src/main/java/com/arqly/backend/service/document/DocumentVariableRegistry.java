package com.arqly.backend.service.document;

import com.arqly.backend.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentVariableRegistry {
    private final List<DocumentVariableProvider> providers;

    public DocumentVariableRegistry(List<DocumentVariableProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public String resolve(String variable, DocumentContext context) {
        return providers.stream()
                .filter(provider -> provider.supports(variable))
                .findFirst()
                .flatMap(provider -> provider.resolve(variable, context))
                .orElseThrow(() -> new BusinessException("Variável não suportada: {{" + variable + "}}"));
    }

    public boolean supports(String variable) {
        return providers.stream().anyMatch(provider -> provider.supports(variable));
    }

    public List<DocumentVariableDefinition> definitions() {
        return providers.stream()
                .flatMap(provider -> provider.definitions().stream())
                .sorted(Comparator.comparing(DocumentVariableDefinition::group).thenComparing(DocumentVariableDefinition::label))
                .toList();
    }
}
