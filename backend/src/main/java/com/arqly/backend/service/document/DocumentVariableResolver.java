package com.arqly.backend.service.document;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DocumentVariableResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_.]*)\\s*}}");
    private final DocumentVariableRegistry registry;

    public DocumentVariableResolver(DocumentVariableRegistry registry) {
        this.registry = registry;
    }

    public ResolvedDocument resolve(String template, DocumentContext context) {
        var matcher = PLACEHOLDER.matcher(template == null ? "" : template);
        var rendered = new StringBuffer();
        var unresolved = new LinkedHashSet<String>();
        while (matcher.find()) {
            var variable = matcher.group(1);
            if (registry.supports(variable)) {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(registry.resolve(variable, context)));
            } else {
                unresolved.add("{{" + variable + "}}");
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(rendered);
        return new ResolvedDocument(rendered.toString(), List.copyOf(unresolved));
    }
}
