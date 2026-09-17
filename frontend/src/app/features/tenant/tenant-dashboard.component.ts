import { HttpClient } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';

interface TenantDashboard {
  cards: { title: string; description: string; icon: string }[];
}

@Component({
  selector: 'app-tenant-dashboard',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="space-y-8">
      <section class="rounded-[2rem] bg-gradient-to-br from-arqly-700 to-arqly-900 p-8 text-white shadow-soft">
        <p class="text-sm font-bold uppercase tracking-[0.2em] text-white/70">Workspace do escritório</p>
        <h2 class="mt-3 text-4xl font-extrabold tracking-tight">Fundação pronta para seus projetos</h2>
        <p class="mt-3 max-w-2xl text-white/75">Os módulos abaixo já têm espaço reservado na experiência, sem regras de negócio prematuras nesta fase.</p>
      </section>

      <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        @for (card of dashboard()?.cards || []; track card.title) {
          <article class="card p-6">
            <div class="grid h-14 w-14 place-items-center rounded-full bg-arqly-100 text-arqly-700">
              <lucide-icon [name]="card.icon" size="24"></lucide-icon>
            </div>
            <h3 class="mt-6 text-xl font-bold">{{ card.title }}</h3>
            <p class="mt-2 text-sm leading-6 text-slate-500">{{ card.description }}</p>
          </article>
        }
      </div>
    </div>
  `
})
export class TenantDashboardComponent implements OnInit {
  readonly dashboard = signal<TenantDashboard | null>(null);

  constructor(private readonly http: HttpClient) {}

  ngOnInit() {
    this.http.get<ApiResponse<TenantDashboard>>('/api/tenant/dashboard')
      .subscribe((response) => this.dashboard.set(response.data));
  }
}
