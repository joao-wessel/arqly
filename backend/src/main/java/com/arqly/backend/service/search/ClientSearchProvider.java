package com.arqly.backend.service.search;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.SearchResultType;
import com.arqly.backend.repository.ClientRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientSearchProvider implements GlobalSearchProvider {
    private final ClientRepository clients;
    public ClientSearchProvider(ClientRepository clients) { this.clients = clients; }
    public SearchResultType type() { return SearchResultType.CLIENT; }
    public List<SearchResultResponse> search(SearchUserContext context, String query, int limit) {
        return clients.findAll((root, criteria, builder) -> builder.and(
                        builder.equal(root.get("tenant").get("id"), context.tenantId()), builder.isFalse(root.get("deleted")),
                        SearchSpecifications.matches(builder, query, root.get("name"), root.get("legalName"), root.get("tradeName"), root.get("email"), root.get("cpf"), root.get("cnpj"), root.get("phone"))), PageRequest.of(0, limit))
                .stream().map(client -> SearchResults.result(client.getId(), type(), displayName(client.getName(), client.getLegalName(), client.getTradeName()), client.getEmail(), null, "/app/clients", "UserRound")).toList();
    }
    private String displayName(String name, String legalName, String tradeName) { return name != null ? name : tradeName != null ? tradeName : legalName; }
}
