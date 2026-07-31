package com.arqly.backend.service.document;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class TenantVariableProvider extends AbstractMapVariableProvider {
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "escritorio.nomeFantasia", context -> value(context.tenant().getTradeName()),
            "escritorio.razaoSocial", context -> value(context.tenant().getLegalName()),
            "escritorio.documento", context -> value(context.tenant().getCnpj()),
            "escritorio.email", context -> value(context.tenant().getPrimaryEmail()),
            "escritorio.telefone", context -> value(context.tenant().getPhone()),
            "escritorio.endereco", context -> value(context.tenant().getAddress())
    );

    protected Map<String, String> aliases() {
        return Map.of("tenant.companyName", "escritorio.nomeFantasia", "tenant.legalName", "escritorio.razaoSocial",
                "tenant.document", "escritorio.documento", "tenant.email", "escritorio.email",
                "tenant.phone", "escritorio.telefone", "tenant.address", "escritorio.endereco");
    }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }
    public List<DocumentVariableDefinition> definitions() {
        return List.of(
                definition("Escritório", "Nome fantasia", "escritorio.nomeFantasia", "Nome do escritório"),
                definition("Escritório", "Razão social", "escritorio.razaoSocial", "Razão social"),
                definition("Escritório", "CPF/CNPJ", "escritorio.documento", "Documento do escritório"),
                definition("Escritório", "E-mail", "escritorio.email", "E-mail principal"),
                definition("Escritório", "Telefone", "escritorio.telefone", "Telefone"),
                definition("Escritório", "Endereço", "escritorio.endereco", "Endereço principal")
        );
    }
}
