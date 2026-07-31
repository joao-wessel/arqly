package com.arqly.backend.service.document;

import com.arqly.backend.entity.ClientPersonType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ClientVariableProvider extends AbstractMapVariableProvider {
    private final Map<String, Function<DocumentContext, String>> variables = Map.of(
            "cliente.nome", context -> context.client() == null ? "" : displayName(context),
            "cliente.email", context -> context.client() == null ? "" : value(context.client().getEmail()),
            "cliente.telefone", context -> context.client() == null ? "" : value(context.client().getPhone()),
            "cliente.documento", context -> context.client() == null ? "" : document(context),
            "cliente.endereco", context -> context.client() == null ? "" : address(context),
            "cliente.cidade", context -> context.client() == null ? "" : value(context.client().getCity()),
            "cliente.estado", context -> context.client() == null ? "" : value(context.client().getState())
    );

    protected Map<String, String> aliases() {
        return Map.of("client.name", "cliente.nome", "client.email", "cliente.email", "client.phone", "cliente.telefone",
                "client.document", "cliente.documento", "client.address", "cliente.endereco", "client.city", "cliente.cidade",
                "client.state", "cliente.estado");
    }

    protected Map<String, Function<DocumentContext, String>> variables() { return variables; }

    public List<DocumentVariableDefinition> definitions() {
        return List.of(
                definition("Cliente", "Nome", "cliente.nome", "Nome ou nome fantasia do cliente"),
                definition("Cliente", "E-mail", "cliente.email", "E-mail principal"),
                definition("Cliente", "Telefone", "cliente.telefone", "Telefone principal"),
                definition("Cliente", "CPF/CNPJ", "cliente.documento", "Documento de identificação"),
                definition("Cliente", "Endereço", "cliente.endereco", "Endereço completo"),
                definition("Cliente", "Cidade", "cliente.cidade", "Cidade do cliente"),
                definition("Cliente", "Estado", "cliente.estado", "Estado do cliente")
        );
    }

    private String displayName(DocumentContext context) {
        var client = context.client();
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? value(client.getName()) : value(client.getTradeName());
    }

    private String document(DocumentContext context) {
        var client = context.client();
        return client.getPersonType() == ClientPersonType.NATURAL_PERSON ? value(client.getCpf()) : value(client.getCnpj());
    }

    private String address(DocumentContext context) {
        var client = context.client();
        return java.util.stream.Stream.of(client.getStreet(), client.getNumber(), client.getComplement(), client.getDistrict(), client.getCity(), client.getState())
                .filter(part -> part != null && !part.isBlank()).collect(java.util.stream.Collectors.joining(", "));
    }
}
