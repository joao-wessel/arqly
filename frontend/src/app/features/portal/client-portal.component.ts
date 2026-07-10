import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiResponse } from '../../core/auth/auth.models';
import { LogoComponent } from '../../shared/components/logo.component';

interface PortalProposal {
  id: string;
  number: string;
  title: string;
  status: 'DRAFT' | 'SENT' | 'VIEWED' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';
  total: number;
  validUntil: string | null;
  portalUrl: string;
  createdAt: string;
}

interface PortalResponse {
  client: { displayName: string; email: string; phone: string; city: string; state: string };
  projects: unknown[];
  proposals: PortalProposal[];
  message: string;
}

@Component({
  selector: 'app-client-portal',
  standalone: true,
  imports: [DecimalPipe, LogoComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center p-6">
      <section class="card w-full max-w-3xl p-8">
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
            <h2 class="text-lg font-extrabold">Propostas</h2>
            <div class="mt-3 space-y-3">
              @for (proposal of portal()?.proposals; track proposal.id) {
                <a class="block rounded-2xl border border-slate-200 bg-slate-50/70 p-4 transition hover:border-arqly-200 hover:bg-arqly-50"
                   [href]="proposal.portalUrl">
                  <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">{{ proposal.number }}</p>
                      <h3 class="mt-1 font-extrabold text-slate-900">{{ proposal.title }}</h3>
                      <p class="mt-1 text-sm text-slate-500">Validade: {{ proposal.validUntil || '-' }}</p>
                    </div>
                    <div class="text-left sm:text-right">
                      <span class="rounded-full bg-white px-3 py-1 text-xs font-bold text-arqly-700">{{ statusLabel(proposal.status) }}</span>
                      <p class="mt-2 text-sm font-extrabold text-slate-900">R$ {{ proposal.total || 0 | number:'1.2-2' }}</p>
                    </div>
                  </div>
                </a>
              } @empty {
                <p class="text-sm text-slate-500">Nenhuma proposta disponível ainda.</p>
              }
            </div>
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

  statusLabel(status: PortalProposal['status']) {
    return {
      DRAFT: 'Rascunho',
      SENT: 'Enviada',
      VIEWED: 'Visualizada',
      ACCEPTED: 'Aceita',
      REJECTED: 'Recusada',
      EXPIRED: 'Expirada',
      CANCELLED: 'Cancelada'
    }[status];
  }
}
