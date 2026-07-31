package com.arqly.backend.service.document;

import java.util.List;
import java.util.Optional;

public interface DocumentVariableProvider {
    boolean supports(String variable);
    Optional<String> resolve(String variable, DocumentContext context);
    List<DocumentVariableDefinition> definitions();
}
