import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { StatCardComponent } from '../../shared/components/stat-card.component';

interface AdminDashboard {
  tenantCount: number;
  userCount: number;
  platformUserCount: number;
  tenantUserCount: number;
  activeTenantCount: number;
  latestAccesses: { name: string; email: string; lastAccessAt: string; scope: string }[];
  latestTenants: { id: string; tradeName: string; primaryEmail: string; createdAt: string }[];
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [StatCardComponent, LucideAngularModule, DatePipe],
  template: `
    <div class="space-y-8">
      <div>
        <h2 class="text-3xl font-extrabold tracking-tight">Dashboard administrativo</h2>
        <p class="mt-2 text-slate-500">Visão geral da operação Arqly e dos tenants cadastrados.</p>
      </div>

      <div class="grid gap-5 md:grid-cols-3">
        <app-stat-card label="Tenants" [value]="dashboard()?.tenantCount || 0" hint="Total cadastrado" icon="Building2" />
        <app-stat-card label="Usuários" [value]="dashboard()?.userCount || 0" hint="Administradores da plataforma" icon="Users" />
        <app-stat-card label="Tenants ativos" [value]="dashboard()?.activeTenantCount || 0" hint="Operação liberada" icon="BadgeCheck" />
      </div>

      <div class="grid gap-6 xl:grid-cols-2">
        <section class="card p-6">
          <div class="mb-5 flex items-center justify-between">
            <h3 class="text-lg font-bold">Últimos tenants criados</h3>
            <lucide-icon name="Building2" class="text-arqly-600" size="22"></lucide-icon>
          </div>
          <div class="space-y-4">
            @for (tenant of dashboard()?.latestTenants || []; track tenant.id) {
              <div class="flex items-center justify-between rounded-2xl border border-slate-100 p-4">
                <div>
                  <p class="font-bold">{{ tenant.tradeName }}</p>
                  <p class="text-sm text-slate-500">{{ tenant.primaryEmail }}</p>
                </div>
                <span class="rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ tenant.createdAt | date:'dd/MM/yyyy' }}</span>
              </div>
            } @empty {
              <p class="text-sm text-slate-500">Nenhum tenant criado ainda.</p>
            }
          </div>
        </section>

        <section class="card p-6">
          <div class="mb-5 flex items-center justify-between">
            <h3 class="text-lg font-bold">Últimos acessos</h3>
            <lucide-icon name="Clock" class="text-arqly-600" size="22"></lucide-icon>
          </div>
          <div class="space-y-4">
            @for (access of dashboard()?.latestAccesses || []; track access.email) {
              <div class="flex items-center gap-4 rounded-2xl border border-slate-100 p-4">
                <div class="grid h-11 w-11 place-items-center rounded-full bg-slate-100 font-bold">{{ access.name[0] }}</div>
                <div>
                  <p class="font-bold">{{ access.name }}</p>
                  <p class="text-sm text-slate-500">{{ access.scope }} · {{ access.lastAccessAt | date:'short' }}</p>
                </div>
              </div>
            } @empty {
              <p class="text-sm text-slate-500">Os acessos aparecerão aqui após os primeiros logins.</p>
            }
          </div>
        </section>
      </div>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit {
  readonly dashboard = signal<AdminDashboard | null>(null);

  constructor(private readonly http: HttpClient) {}

  ngOnInit() {
    this.http.get<ApiResponse<AdminDashboard>>('/api/platform/dashboard')
      .subscribe((response) => this.dashboard.set(response.data));
  }

}
