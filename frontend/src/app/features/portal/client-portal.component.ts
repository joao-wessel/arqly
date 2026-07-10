import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiResponse } from '../../core/auth/auth.models';
import { LogoComponent } from '../../shared/components/logo.component';

interface PortalResponse {
  client: { displayName: string; email: string; phone: string; city: string; state: string };
  projects: unknown[];
  message: string;
}

@Component({
  selector: 'app-client-portal',
  standalone: true,
  imports: [LogoComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center p-6">
      <section class="card w-full max-w-2xl p-8">
        <app-logo />
        @if (portal()) {
          <p class="mt-8 text-sm font-bold uppercase tracking-[0.18em] text-arqly-600">Portal do cliente</p>
          <h1 class="mt-3 text-3xl font-extrabold">{{ portal()?.client?.displayName }}</h1>
          <p class="mt-3 text-slate-500">{{ portal()?.message }}</p>
          <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <p class="text-sm text-slate-600">{{ portal()?.client?.email }} · {{ portal()?.client?.phone }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ portal()?.client?.city }}/{{ portal()?.client?.state }}</p>
          </div>
          <div class="mt-6">
            <h2 class="text-lg font-extrabold">Projetos</h2>
            <p class="mt-2 text-sm text-slate-500">Nenhum projeto disponível ainda.</p>
          </div>
        } @else {
          <h1 class="mt-8 text-3xl font-extrabold">Portal indisponível</h1>
          <p class="mt-2 text-slate-500">O link pode ter expirado ou sido revogado.</p>
        }
      </section>
    </main>
  `
})
export class ClientPortalComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  readonly portal = signal<PortalResponse | null>(null);

  ngOnInit() {
    const token = this.route.snapshot.paramMap.get('token');
    this.http.get<ApiResponse<PortalResponse>>(`http://localhost:8080/api/portal/${token}`)
      .subscribe({
        next: (response) => this.portal.set(response.data),
        error: () => this.portal.set(null)
      });
  }
}
