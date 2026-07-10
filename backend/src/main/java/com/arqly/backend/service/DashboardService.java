package com.arqly.backend.service;

import com.arqly.backend.dto.DashboardDtos.AccessItem;
import com.arqly.backend.dto.DashboardDtos.AdminDashboard;
import com.arqly.backend.dto.DashboardDtos.Card;
import com.arqly.backend.dto.DashboardDtos.TenantDashboard;
import com.arqly.backend.dto.DashboardDtos.TenantItem;
import com.arqly.backend.entity.TenantStatus;
import com.arqly.backend.repository.PlatformUserRepository;
import com.arqly.backend.repository.TenantRepository;
import com.arqly.backend.repository.TenantUserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final TenantRepository tenants;
    private final TenantUserRepository tenantUsers;
    private final PlatformUserRepository platformUsers;

    public DashboardService(TenantRepository tenants, TenantUserRepository tenantUsers, PlatformUserRepository platformUsers) {
        this.tenants = tenants;
        this.tenantUsers = tenantUsers;
        this.platformUsers = platformUsers;
    }

    public AdminDashboard admin() {
        var latestTenants = tenants.findAll(PageRequest.of(0, 5)).stream()
                .map(t -> new TenantItem(t.getId(), t.getTradeName(), t.getPrimaryEmail(), t.getCreatedAt()))
                .toList();
        var platformAccess = platformUsers.findAll().stream()
                .filter(u -> u.getLastAccessAt() != null)
                .map(u -> new AccessItem(u.getName(), u.getEmail(), u.getLastAccessAt(), "Plataforma"));
        var tenantAccess = tenantUsers.findAll().stream()
                .filter(u -> u.getLastAccessAt() != null)
                .map(u -> new AccessItem(u.getName(), u.getEmail(), u.getLastAccessAt(), "Tenant"));
        var latestAccesses = java.util.stream.Stream.concat(platformAccess, tenantAccess)
                .sorted(Comparator.comparing(AccessItem::lastAccessAt).reversed())
                .limit(5)
                .toList();
        long platformUserCount = platformUsers.count();
        long tenantUserCount = tenantUsers.count();
        return new AdminDashboard(tenants.count(), platformUserCount, platformUserCount, tenantUserCount,
                tenants.countByStatus(TenantStatus.ACTIVE), latestAccesses, latestTenants);
    }

    public TenantDashboard tenant() {
        return new TenantDashboard(List.of(
                new Card("Projetos", "Espaço preparado para gestão de projetos.", "folder"),
                new Card("Tarefas", "Organize atividades e responsáveis.", "list-checks"),
                new Card("Documentos", "Centralize arquivos e versões.", "file-text"),
                new Card("Equipe", "Gerencie papéis e permissões.", "users"),
                new Card("Clientes", "Cadastre clientes e contatos.", "building-2"),
                new Card("Cronograma", "Planeje entregas e marcos.", "calendar-days")
        ));
    }
}
