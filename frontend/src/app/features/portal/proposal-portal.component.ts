import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { LogoComponent } from '../../shared/components/logo.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type PortalDecision = 'accept' | 'reject';

interface PortalProposal {
  client: { displayName: string; email: string; phone: string };
  message: string;
  proposal: {
    id: string;
    number: string;
    title: string;
    description: string;
    validUntil: string | null;
    subtotal: number;
    discount: number;
    addition: number;
    total: number;
    status: string;
    scope: string;
    exclusions: string;
    clientNotes: string;
    items: { serviceName: string; customDescription?: string; serviceDescription?: string; quantity: number; unit: string; unitValue: number; total: number }[];
    paymentConditions: { description: string; percentage?: number | null; value: number; dueDate?: string | null }[];
  };
}

@Component({
  selector: 'app-proposal-portal',
  standalone: true,
  imports: [DatePipe, DecimalPipe, LucideAngularModule, LogoComponent],
  template: `
    <main class="min-h-screen bg-[var(--surface-muted)] p-4 text-slate-900 md:p-8">
      <section class="mx-auto max-w-6xl space-y-5">
        <header class="flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <app-logo />
          <div class="flex flex-wrap items-center gap-3">
            <button class="btn-secondary px-3 py-2" type="button" (click)="goBack()">
              <lucide-icon name="ArrowLeft" size="17"></lucide-icon>
              Voltar
            </button>
            @if (portal()) {
              <span class="rounded-full bg-arqly-50 px-4 py-2 text-sm font-bold text-arqly-700">{{ statusLabel(portal()!.proposal.status) }}</span>
              @if (portal()!.proposal.validUntil) {
                <span class="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold text-slate-600">Válida até {{ portal()!.proposal.validUntil | date:'dd/MM/yyyy' }}</span>
              }
            }
          </div>
        </header>

        @if (portal(); as data) {
          <article class="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <div class="grid gap-6 border-b border-slate-200 bg-slate-50/70 p-6 md:grid-cols-[1fr_19rem] md:p-8">
              <div>
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">{{ data.proposal.number }}</p>
                <h1 class="mt-3 max-w-3xl text-3xl font-extrabold tracking-tight md:text-4xl">{{ data.proposal.title }}</h1>
                <p class="mt-3 max-w-3xl leading-7 text-slate-500">{{ data.proposal.description || data.message }}</p>
                <div class="mt-6 grid gap-3 sm:grid-cols-2">
                  <div class="rounded-2xl border border-slate-200 bg-white p-4">
                    <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-slate-500">Cliente</p>
                    <p class="mt-2 font-extrabold">{{ data.client.displayName }}</p>
                    <p class="mt-1 text-sm text-slate-500">{{ data.client.email || 'E-mail não informado' }}</p>
                  </div>
                  <div class="rounded-2xl border border-slate-200 bg-white p-4">
                    <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-slate-500">Contato</p>
                    <p class="mt-2 font-extrabold">{{ data.client.phone || 'Telefone não informado' }}</p>
                    <p class="mt-1 text-sm text-slate-500">Portal do cliente Arqly</p>
                  </div>
                </div>
              </div>

              <aside class="rounded-3xl border border-arqly-100 bg-white p-5">
                <p class="text-sm font-bold text-slate-500">Valor final</p>
                <strong class="mt-2 block text-4xl text-arqly-700">R$ {{ data.proposal.total || 0 | number:'1.2-2' }}</strong>
                <div class="mt-5 space-y-3 text-sm text-slate-600">
                  <div class="flex justify-between gap-4"><span>Subtotal</span><strong>R$ {{ data.proposal.subtotal || 0 | number:'1.2-2' }}</strong></div>
                  <div class="flex justify-between gap-4"><span>Desconto</span><strong>R$ {{ data.proposal.discount || 0 | number:'1.2-2' }}</strong></div>
                  <div class="flex justify-between gap-4"><span>Acréscimo</span><strong>R$ {{ data.proposal.addition || 0 | number:'1.2-2' }}</strong></div>
                </div>
              </aside>
            </div>

            <div class="grid gap-6 p-6 md:grid-cols-[1fr_19rem] md:p-8">
              <div class="space-y-6">
                <section>
                  <div class="flex items-center justify-between gap-4">
                    <h2 class="text-xl font-extrabold">Serviços</h2>
                    <span class="text-sm font-bold text-slate-500">{{ data.proposal.items.length }} item(ns)</span>
                  </div>

                  <div class="mt-4 hidden overflow-x-auto rounded-2xl border border-slate-200 md:block">
                    <table class="w-full text-left text-sm">
                      <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                        <tr>
                          <th class="px-4 py-3">Serviço</th>
                          <th class="px-4 py-3">Qtd.</th>
                          <th class="px-4 py-3">Unitário</th>
                          <th class="px-4 py-3 text-right">Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (item of data.proposal.items; track $index) {
                          <tr class="border-t border-slate-100">
                            <td class="px-4 py-4">
                              <p class="font-extrabold">{{ item.serviceName }}</p>
                              <p class="mt-1 text-xs leading-5 text-slate-500">{{ item.customDescription || item.serviceDescription || 'Sem descrição adicional.' }}</p>
                            </td>
                            <td class="px-4 py-4">{{ item.quantity }}</td>
                            <td class="px-4 py-4">R$ {{ item.unitValue || 0 | number:'1.2-2' }}</td>
                            <td class="px-4 py-4 text-right font-extrabold">R$ {{ item.total || 0 | number:'1.2-2' }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>

                  <div class="mt-4 space-y-3 md:hidden">
                    @for (item of data.proposal.items; track $index) {
                      <article class="rounded-2xl border border-slate-200 bg-white p-4">
                        <p class="font-extrabold">{{ item.serviceName }}</p>
                        <p class="mt-1 text-sm text-slate-500">{{ item.customDescription || item.serviceDescription || 'Sem descrição adicional.' }}</p>
                        <div class="mt-4 grid grid-cols-3 gap-3 text-sm">
                          <div><span class="block text-xs font-bold text-slate-500">Qtd.</span><strong>{{ item.quantity }}</strong></div>
                          <div><span class="block text-xs font-bold text-slate-500">Unitário</span><strong>R$ {{ item.unitValue || 0 | number:'1.2-2' }}</strong></div>
                          <div><span class="block text-xs font-bold text-slate-500">Total</span><strong>R$ {{ item.total || 0 | number:'1.2-2' }}</strong></div>
                        </div>
                      </article>
                    }
                  </div>
                </section>

                <section class="grid gap-4 md:grid-cols-2">
                  <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                    <h3 class="font-extrabold">Escopo</h3>
                    <p class="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{{ data.proposal.scope || 'Escopo descrito na proposta comercial.' }}</p>
                  </div>
                  <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                    <h3 class="font-extrabold">Exclusões</h3>
                    <p class="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{{ data.proposal.exclusions || 'Sem exclusões informadas.' }}</p>
                  </div>
                </section>

                @if (data.proposal.clientNotes) {
                  <section class="rounded-2xl border border-slate-200 bg-white p-5">
                    <h3 class="font-extrabold">Observações</h3>
                    <p class="mt-2 whitespace-pre-line text-sm leading-6 text-slate-600">{{ data.proposal.clientNotes }}</p>
                  </section>
                }
              </div>

              <aside class="space-y-4">
                <div class="rounded-2xl border border-slate-200 bg-white p-5">
                  <h3 class="font-extrabold">Condições de pagamento</h3>
                  <div class="mt-3 space-y-2">
                    @for (condition of data.proposal.paymentConditions; track $index) {
                      <div class="rounded-xl bg-slate-50 p-3 text-sm">
                        <p class="font-bold">{{ condition.description }}</p>
                        <p class="mt-1 text-slate-500">
                          R$ {{ condition.value || 0 | number:'1.2-2' }}
                          @if (condition.percentage) { · {{ condition.percentage }}% }
                          · {{ condition.dueDate ? (condition.dueDate | date:'dd/MM/yyyy') : 'Sem vencimento' }}
                        </p>
                      </div>
                    } @empty {
                      <p class="text-sm text-slate-500">Condições a combinar.</p>
                    }
                  </div>
                </div>

                @if (canDecide()) {
                  <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                    <p class="text-sm font-bold text-slate-600">Pronto para responder?</p>
                    <div class="mt-3 grid gap-3">
                      <button class="btn-primary justify-center" type="button" (click)="openDecisionModal('accept')"><lucide-icon name="Check" size="18"></lucide-icon>Aceitar proposta</button>
                      <button class="btn-secondary justify-center text-red-600" type="button" (click)="openDecisionModal('reject')"><lucide-icon name="X" size="18"></lucide-icon>Recusar proposta</button>
                    </div>
                  </div>
                }
              </aside>
            </div>
          </article>
        } @else {
          <section class="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <h1 class="text-3xl font-extrabold">Proposta indisponível</h1>
            <p class="mt-2 text-slate-500">O link pode ter expirado, sido revogado ou não estar mais disponível.</p>
          </section>
        }
      </section>

      @if (decisionModalOpen()) {
        <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
          <section class="modal-panel card w-full max-w-md space-y-5 p-6">
            <div class="flex items-start gap-4">
              <span class="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl"
                    [class.bg-arqly-50]="decision() === 'accept'"
                    [class.text-arqly-700]="decision() === 'accept'"
                    [class.bg-red-50]="decision() === 'reject'"
                    [class.text-red-700]="decision() === 'reject'">
                <lucide-icon [name]="decision() === 'accept' ? 'Check' : 'X'" size="22"></lucide-icon>
              </span>
              <div>
                <h3 class="text-xl font-extrabold">{{ decision() === 'accept' ? 'Aceitar proposta' : 'Recusar proposta' }}</h3>
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  {{ decision() === 'accept' ? 'Confirme para registrar o aceite desta proposta.' : 'Confirme para registrar a recusa desta proposta.' }}
                </p>
              </div>
            </div>
            <div class="flex justify-end gap-3">
              <button class="btn-secondary" type="button" (click)="closeDecisionModal()">Cancelar</button>
              <button class="btn-primary" type="button"
                      [class.bg-red-600]="decision() === 'reject'"
                      [class.hover:bg-red-700]="decision() === 'reject'"
                      (click)="confirmDecision()">
                {{ decision() === 'accept' ? 'Aceitar' : 'Recusar' }}
              </button>
            </div>
          </section>
        </div>
      }
    </main>
  `
})
export class ProposalPortalComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  readonly portal = signal<PortalProposal | null>(null);
  readonly decisionModalOpen = signal(false);
  readonly decision = signal<PortalDecision>('accept');

  ngOnInit() {
    this.load();
  }

  load() {
    const token = this.route.snapshot.paramMap.get('token');
    const proposalId = this.route.snapshot.paramMap.get('proposalId');
    this.http.get<ApiResponse<PortalProposal>>(`http://localhost:8080/api/portal/${token}/proposals/${proposalId}`)
      .subscribe({
        next: (response) => this.portal.set(response.data),
        error: () => this.portal.set(null)
      });
  }

  canDecide() {
    const status = this.portal()?.proposal.status;
    return status === 'SENT' || status === 'VIEWED';
  }

  openDecisionModal(decision: PortalDecision) {
    this.decision.set(decision);
    this.decisionModalOpen.set(true);
  }

  closeDecisionModal() {
    this.decisionModalOpen.set(false);
  }

  confirmDecision() {
    this.decide(this.decision(), this.decision() === 'accept' ? 'Proposta aceita' : 'Proposta recusada');
  }

  goBack() {
    window.history.length > 1 ? window.history.back() : window.location.assign(`/portal/${this.route.snapshot.paramMap.get('token')}`);
  }

  decide(action: PortalDecision, message: string) {
    const token = this.route.snapshot.paramMap.get('token');
    const proposalId = this.route.snapshot.paramMap.get('proposalId');
    this.http.post<ApiResponse<unknown>>(`http://localhost:8080/api/portal/${token}/proposals/${proposalId}/${action}`, {})
      .subscribe({
        next: () => {
          this.closeDecisionModal();
          this.toast.success(message);
          this.load();
        },
        error: () => this.toast.error('Não foi possível registrar sua decisão')
      });
  }

  statusLabel(status: string) {
    return {
      DRAFT: 'Rascunho',
      SENT: 'Enviada',
      VIEWED: 'Visualizada',
      ACCEPTED: 'Aceita',
      REJECTED: 'Recusada',
      EXPIRED: 'Expirada',
      CANCELLED: 'Cancelada'
    }[status] || status;
  }

  unitLabel(unit: string) {
    return {
      UN: 'UN',
      M2: 'm²',
      M: 'm',
      HOUR: 'hora',
      DAY: 'dia',
      MONTH: 'mês',
      PROJECT: 'projeto',
      VISIT: 'visita',
      OTHER: 'outro'
    }[unit] || unit;
  }
}
