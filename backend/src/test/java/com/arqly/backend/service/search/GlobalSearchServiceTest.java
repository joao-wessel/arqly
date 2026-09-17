package com.arqly.backend.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arqly.backend.dto.SearchDtos.SearchResultResponse;
import com.arqly.backend.entity.Role;
import com.arqly.backend.entity.SearchResultType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {
    private final SearchUserContext context = new SearchUserContext(UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.ROLE_USER));
    @Mock private GlobalSearchProvider clients;
    @Mock private GlobalSearchProvider projects;

    @Test
    void skipsProvidersForShortTerms() {
        GlobalSearchService service = service();

        var response = service.search(context, "s", 12, null);

        assertThat(response.results()).isEmpty();
        verify(clients, never()).search(any(), any(), anyInt());
        verify(projects, never()).search(any(), any(), anyInt());
    }

    @Test
    void ranksExactMatchesBeforePrefixesAndContainsMatches() {
        when(clients.type()).thenReturn(SearchResultType.CLIENT);
        when(projects.type()).thenReturn(SearchResultType.PROJECT);
        when(clients.search(any(), any(), anyInt())).thenReturn(List.of(result(SearchResultType.CLIENT, "João Silva")));
        when(projects.search(any(), any(), anyInt())).thenReturn(List.of(
                result(SearchResultType.PROJECT, "Silva Residence"),
                result(SearchResultType.PROJECT, "Residência Silva"),
                result(SearchResultType.PROJECT, "Silva")));

        var response = service().search(context, "silva", 12, null);

        assertThat(response.results()).extracting(SearchResultResponse::title)
                .containsExactly("Silva", "Silva Residence", "João Silva", "Residência Silva");
    }

    @Test
    void limitsResultsAndOnlyExecutesRequestedProviders() {
        when(clients.type()).thenReturn(SearchResultType.CLIENT);
        when(projects.type()).thenReturn(SearchResultType.PROJECT);
        when(projects.search(any(), any(), anyInt())).thenReturn(List.of(
                result(SearchResultType.PROJECT, "Projeto 1"), result(SearchResultType.PROJECT, "Projeto 2"), result(SearchResultType.PROJECT, "Projeto 3")));

        var response = service().search(context, "projeto", 2, List.of(SearchResultType.PROJECT));

        assertThat(response.results()).hasSize(2);
        assertThat(response.hasMore()).isTrue();
        verify(projects).search(context, "projeto", 3);
        verify(clients, never()).search(any(), any(), anyInt());
    }

    @Test
    void normalizesAccentsBeforeDelegatingToProviders() {
        when(clients.type()).thenReturn(SearchResultType.CLIENT);
        when(projects.type()).thenReturn(SearchResultType.PROJECT);
        when(clients.search(any(), any(), anyInt())).thenReturn(List.of());
        when(projects.search(any(), any(), anyInt())).thenReturn(List.of());

        service().search(context, "João", 12, null);

        verify(clients).search(context, "joao", 12);
        verify(projects).search(context, "joao", 12);
    }

    private GlobalSearchService service() {
        when(clients.type()).thenReturn(SearchResultType.CLIENT);
        when(projects.type()).thenReturn(SearchResultType.PROJECT);
        return new GlobalSearchService(new GlobalSearchProviderRegistry(List.of(clients, projects)));
    }

    private SearchResultResponse result(SearchResultType type, String title) {
        return new SearchResultResponse(UUID.randomUUID(), type, title, null, null, "/app", "FileText", java.util.Map.of());
    }
}
