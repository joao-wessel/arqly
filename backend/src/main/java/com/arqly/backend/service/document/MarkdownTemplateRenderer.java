package com.arqly.backend.service.document;

import org.springframework.stereotype.Component;

@Component
public class MarkdownTemplateRenderer implements TemplateRenderer {
    private final DocumentVariableResolver resolver;

    public MarkdownTemplateRenderer(DocumentVariableResolver resolver) {
        this.resolver = resolver;
    }

    public String format() { return "MARKDOWN"; }

    public ResolvedDocument render(String template, DocumentContext context) {
        return resolver.resolve(template, context);
    }
}
