import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { RouterLink } from '@angular/router';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyCurrencyInputComponent } from '../../shared/components/arqly-currency-input.component';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { ContextDocumentsComponent } from '../../shared/components/context-documents.component';

type ProposalStatus = 'DRAFT' | 'SENT' | 'VIEWED' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';
type BillingUnit = 'UN' | 'M2' | 'M' | 'HOUR' | 'DAY' | 'MONTH' | 'PROJECT' | 'VISIT' | 'OTHER';
type OriginType = 'MANUAL' | 'BRIEFING' | 'PROPOSAL';
type ProposalTab = 'data' | 'services' | 'payments' | 'notes' | 'summary' | 'documents';
type ProposalConfirmAction = 'send' | 'delete' | 'createProject';

interface Page<T> {
  content: T[];
  number: number;
  totalElements: number;
  totalPages: number;
}

interface ClientOption {
  id: string;
  displayName: string;
  email: string;
}

interface BriefingOption {
  id: string;
  clientId: string;
  clientName: string;
  title: string;
  proposalGenerated: boolean;
}

interface CatalogService {
  id: string;
  name: string;
  shortDescription?: string;
  baseValue: number | null;
  billingUnit: BillingUnit;
}

interface ProjectTemplateOption {
  id: string;
  name: string;
}

interface TenantUserOption {
  id: string;
  name: string;
  email: string;
  tenantAdmin: boolean;
}

interface ProposalSummary {
  id: string;
  number: string;
  clientId: string;
  clientName: string;
  briefingId?: string | null;
  briefingTitle?: string | null;
  originType: OriginType;
  title: string;
  total: number;
  status: ProposalStatus;
  validUntil: string | null;
  createdBy: string;
  projectCreated: boolean;
  createdAt: string;
}

interface ProposalItem {
  serviceId: string;
  serviceName: string;
  serviceDescription?: string;
  customDescription?: string;
  quantity: number;
  unit: BillingUnit;
  unitValue: number;
  discount: number;
  total: number;
}

interface PaymentCondition {
  description: string;
  percentage?: number | null;
  value: number;
  dueDate?: string | null;
}

interface ProposalDetail extends ProposalSummary {
  clientEmail: string;
  description: string;
  subtotal: number;
  discount: number;
  addition: number;
  scope: string;
  exclusions: string;
  internalNotes: string;
  clientNotes: string;
  sentAt: string | null;
  viewedAt: string | null;
  acceptedAt: string | null;
  rejectedAt: string | null;
  updatedBy: string;
  updatedAt: string;
  items: ProposalItem[];
  paymentConditions: PaymentCondition[];
}

interface ProposalStats {
  quantity: number;
  totalValue: number;
  accepted: number;
  rejected: number;
  expired: number;
  pending: number;
}

@Component({
  selector: 'app-proposals',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, RouterLink, ArqlySelectComponent, ArqlyDatePickerComponent, ArqlyCurrencyInputComponent, ContextDocumentsComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Comercial</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Propostas</h2>
          <p class="mt-2 text-slate-500">Monte propostas comerciais, envie ao cliente e acompanhe aceite pelo portal.</p>
        </div>
        <button class="btn-primary" type="button" (click)="openModal()">
          <lucide-icon name="Plus" size="18"></lucide-icon>
          Nova proposta
        </button>
      </div>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Propostas</p><strong class="mt-2 block text-3xl">{{ stats()?.quantity || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Valor total</p><strong class="mt-2 block text-2xl">R$ {{ stats()?.totalValue || 0 | number:'1.2-2' }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Aceitas</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.accepted || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Pendentes</p><strong class="mt-2 block text-3xl text-amber-600">{{ stats()?.pending || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Recusadas</p><strong class="mt-2 block text-3xl text-red-600">{{ stats()?.rejected || 0 }}</strong></div>
      </div>

      <div class="card overflow-hidden">
        <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-8" [formGroup]="filterForm" (ngSubmit)="search()">
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Número</span>
            <input class="field" placeholder="PROP-2026" formControlName="number">
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Cliente</span>
            <app-arqly-select formControlName="clientId" placeholder="Todos" [options]="clientFilterOptions()" />
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Status</span>
            <app-arqly-select formControlName="status" placeholder="Todos" [options]="statusFilterOptions" />
          </label>
          <div class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Data inicial</span>
            <app-arqly-date-picker formControlName="from" placeholder="Data inicial" />
          </div>
          <div class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Data final</span>
            <app-arqly-date-picker formControlName="to" placeholder="Data final" />
          </div>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Valor mínimo</span>
            <app-arqly-currency-input formControlName="minValue" />
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Valor máximo</span>
            <app-arqly-currency-input formControlName="maxValue" />
          </label>
          <button class="btn-secondary h-12 self-end justify-center px-4" type="submit"><lucide-icon name="Search" size="18"></lucide-icon>Pesquisar</button>
        </form>

        <div class="hidden overflow-x-auto md:block">
          <table class="w-full min-w-[1040px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th class="px-6 py-4">Número</th>
                <th class="px-6 py-4">Cliente</th>
                <th class="px-6 py-4">Título</th>
                <th class="px-6 py-4">Origem</th>
                <th class="px-6 py-4">Valor</th>
                <th class="px-6 py-4">Status</th>
                <th class="px-6 py-4">Validade</th>
                <th class="px-6 py-4">Data</th>
                <th class="px-6 py-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (proposal of proposals(); track proposal.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-6 py-4 font-extrabold">{{ proposal.number }}</td>
                  <td class="px-6 py-4">{{ proposal.clientName }}</td>
                  <td class="px-6 py-4">
                    <p class="font-bold">{{ proposal.title }}</p>
                    @if (proposal.projectCreated) {
                      <p class="mt-1 text-xs font-bold text-arqly-700">Projeto criado</p>
                    }
                  </td>
                  <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="originClass(proposal.originType)">{{ originLabel(proposal.originType) }}</span><p class="mt-1 text-xs text-slate-400">{{ proposal.briefingTitle || 'Sem briefing' }}</p></td>
                  <td class="px-6 py-4">R$ {{ proposal.total || 0 | number:'1.2-2' }}</td>
                  <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(proposal.status)">{{ statusLabel(proposal.status) }}</span></td>
                  <td class="px-6 py-4 text-slate-500">{{ proposal.validUntil ? (proposal.validUntil | date:'dd/MM/yyyy') : '-' }}</td>
                  <td class="px-6 py-4 text-slate-500">{{ proposal.createdAt | date:'dd/MM/yyyy' }}</td>
                  <td class="px-6 py-4">
                    <div class="flex justify-end gap-2">
                      <button class="btn-secondary px-3 py-2" type="button" title="Visualizar" (click)="openModal(proposal.id, true)"><lucide-icon name="Search" size="16"></lucide-icon></button>
                      <a class="btn-secondary px-3 py-2" title="Arquivos" [routerLink]="['/app/files', 'PROPOSAL', proposal.id]"><lucide-icon name="Archive" size="16"></lucide-icon></a>
                      <button class="btn-secondary px-3 py-2" type="button" title="Editar" [disabled]="proposal.status !== 'DRAFT'" (click)="openModal(proposal.id)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" title="Enviar" [disabled]="proposal.status !== 'DRAFT'" (click)="openConfirmModal('send', proposal)"><lucide-icon name="Send" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" title="PDF" (click)="downloadPdf(proposal)"><lucide-icon name="FileText" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" title="Duplicar" (click)="duplicate(proposal)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                      @if (proposal.status === 'DRAFT') {
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" title="Excluir" (click)="openConfirmModal('delete', proposal)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      }
                      @if (proposal.status === 'ACCEPTED' && !proposal.projectCreated) {
                        <button class="btn-secondary px-3 py-2" type="button" (click)="openConfirmModal('createProject', proposal)">Criar projeto</button>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="9" class="px-6 py-12 text-center text-slate-500">Nenhuma proposta cadastrada.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <div class="space-y-3 p-4 md:hidden">
          @for (proposal of proposals(); track proposal.id) {
            <article class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-extrabold">{{ proposal.number }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ proposal.clientName }}</p>
                </div>
                <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(proposal.status)">{{ statusLabel(proposal.status) }}</span>
              </div>
              <p class="mt-3 font-bold">{{ proposal.title }}</p>
              <p class="mt-1 text-sm text-slate-500">R$ {{ proposal.total || 0 | number:'1.2-2' }}</p>
              <div class="mt-4 grid grid-cols-3 gap-2">
                <button class="btn-secondary px-3 py-2" type="button" (click)="openModal(proposal.id, true)">Ver</button>
                <button class="btn-secondary px-3 py-2" type="button" [disabled]="proposal.status !== 'DRAFT'" (click)="openModal(proposal.id)">Editar</button>
                <button class="btn-secondary px-3 py-2" type="button" (click)="downloadPdf(proposal)">PDF</button>
                @if (proposal.status === 'DRAFT') {
                  <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="openConfirmModal('delete', proposal)">Excluir</button>
                }
              </div>
            </article>
          } @empty {
            <p class="py-8 text-center text-sm text-slate-500">Nenhuma proposta cadastrada.</p>
          }
        </div>

        <div class="flex flex-col gap-3 border-t border-slate-100 p-4 text-sm text-slate-500 md:flex-row md:items-center md:justify-between">
          <span>{{ totalElements() }} proposta(s) encontrada(s)</span>
          <div class="flex items-center gap-2">
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() === 0" (click)="previousPage()">Anterior</button>
            <span class="px-2 font-bold text-slate-700">Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span>
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">Próxima</button>
          </div>
        </div>
      </div>
    </section>

    @if (modalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-6xl space-y-5 overflow-y-auto p-6" [formGroup]="proposalForm" (ngSubmit)="save()">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Proposta comercial</p>
              <h3 class="mt-2 text-xl font-extrabold">{{ readOnly() ? 'Visualizar proposta' : editingId() ? 'Editar proposta' : 'Nova proposta' }}</h3>
            </div>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>

          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-6">
            @for (tab of tabs; track tab.value) {
              @if (tab.value !== 'documents' || readOnly()) {
                <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                        [class.bg-white]="activeTab() === tab.value"
                        [class.text-arqly-700]="activeTab() === tab.value"
                        [class.shadow-sm]="activeTab() === tab.value"
                        [class.text-slate-500]="activeTab() !== tab.value"
                        (click)="activeTab.set(tab.value)">{{ tab.label }}</button>
              }
            }
          </div>

          @if (activeTab() === 'data') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Dados</p>
              @if (!readOnly()) {
                <div class="mt-4 rounded-2xl border border-slate-200 bg-white p-4">
                  <p class="text-sm font-extrabold">Deseja utilizar um briefing?</p>
                  <p class="mt-1 text-sm text-slate-500">O fluxo recomendado mantém a origem comercial registrada, mas a proposta também pode ser criada manualmente.</p>
                  <div class="mt-3 grid gap-3 md:grid-cols-2">
                    <button class="rounded-2xl border p-4 text-left transition" type="button"
                      [class.border-arqly-500]="proposalOriginMode() === 'BRIEFING'"
                      [class.bg-arqly-50]="proposalOriginMode() === 'BRIEFING'"
                      [class.border-slate-200]="proposalOriginMode() !== 'BRIEFING'"
                      (click)="setProposalOriginMode('BRIEFING')">
                      <strong class="block">Sim, usar briefing</strong>
                      <span class="mt-1 block text-sm text-slate-500">Vincula a proposta ao primeiro contato.</span>
                    </button>
                    <button class="rounded-2xl border p-4 text-left transition" type="button"
                      [class.border-arqly-500]="proposalOriginMode() === 'MANUAL'"
                      [class.bg-arqly-50]="proposalOriginMode() === 'MANUAL'"
                      [class.border-slate-200]="proposalOriginMode() !== 'MANUAL'"
                      (click)="setProposalOriginMode('MANUAL')">
                      <strong class="block">Não, criar manualmente</strong>
                      <span class="mt-1 block text-sm text-slate-500">Útil para negociações rápidas ou recorrentes.</span>
                    </button>
                  </div>
                  @if (proposalOriginMode() === 'MANUAL') {
                    <p class="mt-3 text-sm text-slate-500">Projetos e propostas criados manualmente não possuem rastreabilidade completa do processo comercial.</p>
                  }
                </div>
              }
              <div class="mt-4 grid gap-4 md:grid-cols-2">
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Cliente <span class="text-red-500">*</span></span>
                  <app-arqly-select formControlName="clientId" placeholder="Selecione o cliente" [options]="clientFormOptions()" />
                </label>
                @if (proposalOriginMode() === 'BRIEFING') {
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Briefing <span class="text-red-500">*</span></span>
                    <app-arqly-select formControlName="briefingId" placeholder="Selecione o briefing" [options]="briefingOptions()" panelMode="fixed" />
                  </label>
                }
                <div class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Validade</span>
                  <app-arqly-date-picker formControlName="validUntil" placeholder="Selecione" />
                </div>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Título <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Ex.: Proposta para projeto residencial" formControlName="title">
                </label>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Descrição</span>
                  <textarea class="field min-h-28" placeholder="Resumo comercial da proposta" formControlName="description"></textarea>
                </label>
              </div>
            </section>
          }

          @if (activeTab() === 'services') {
            <section class="space-y-4">
              @if (!readOnly()) {
              <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Adicionar serviço</p>
                <div class="mt-4 grid gap-3 md:grid-cols-6">
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Serviço do catálogo <span class="text-red-500">*</span></span>
                    <app-arqly-select [options]="serviceOptions()" placeholder="Selecione o serviço" [formControl]="itemForm.controls.serviceId" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Quantidade <span class="text-red-500">*</span></span>
                    <input class="field" type="number" min="0.01" step="0.01" placeholder="1" [formControl]="itemForm.controls.quantity">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Valor unitário</span>
                    <app-arqly-currency-input [formControl]="itemForm.controls.unitValue" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Desconto</span>
                    <app-arqly-currency-input [formControl]="itemForm.controls.discount" />
                  </label>
                  <button class="btn-secondary h-12 self-end justify-center px-4" type="button" (click)="addItem()">Adicionar</button>
                  <label class="space-y-1 md:col-span-6">
                    <span class="text-xs font-bold text-slate-500">Descrição personalizada</span>
                    <textarea class="field" placeholder="Descrição personalizada do item" [formControl]="itemForm.controls.customDescription"></textarea>
                  </label>
                </div>
              </div>
              }

              <div class="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
                <table class="w-full min-w-[780px] text-left text-sm">
                  <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                    <tr><th class="px-4 py-3">Serviço</th><th class="px-4 py-3">Qtd.</th><th class="px-4 py-3">Unidade</th><th class="px-4 py-3">Unitário</th><th class="px-4 py-3">Desconto</th><th class="px-4 py-3">Total</th><th class="px-4 py-3"></th></tr>
                  </thead>
                  <tbody>
                    @for (item of items(); track $index) {
                      <tr class="border-t border-slate-100">
                        <td class="px-4 py-3"><p class="font-bold">{{ item.serviceName }}</p><p class="text-xs text-slate-500">{{ item.customDescription || item.serviceDescription }}</p></td>
                        <td class="px-4 py-3">{{ item.quantity }}</td>
                        <td class="px-4 py-3">{{ unitLabel(item.unit) }}</td>
                        <td class="px-4 py-3">R$ {{ item.unitValue | number:'1.2-2' }}</td>
                        <td class="px-4 py-3">R$ {{ item.discount | number:'1.2-2' }}</td>
                        <td class="px-4 py-3 font-bold">R$ {{ item.total | number:'1.2-2' }}</td>
                        <td class="px-4 py-3 text-right">
                          @if (!readOnly()) {
                            <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="removeItem($index)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                          }
                        </td>
                      </tr>
                    } @empty {
                      <tr><td colspan="7" class="px-4 py-10 text-center text-slate-500">Adicione pelo menos um serviço.</td></tr>
                    }
                  </tbody>
                </table>
              </div>
            </section>
          }

          @if (activeTab() === 'payments') {
            <section class="space-y-4">
              @if (!readOnly()) {
              <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Condições de pagamento</p>
                <div class="mt-4 grid gap-3 md:grid-cols-5">
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Descrição <span class="text-red-500">*</span></span>
                    <input class="field" placeholder="Ex.: Entrada" [formControl]="paymentForm.controls.description">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Percentual</span>
                    <input class="field" type="number" min="0" max="100" step="0.01" placeholder="30" [formControl]="paymentForm.controls.percentage">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Valor</span>
                    <app-arqly-currency-input [formControl]="paymentForm.controls.value" />
                  </label>
                  <div class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Vencimento</span>
                    <app-arqly-date-picker [formControl]="paymentForm.controls.dueDate" placeholder="Selecione" />
                  </div>
                  <button class="btn-secondary md:col-span-5" type="button" (click)="addPayment()">Adicionar parcela</button>
                </div>
              </div>
              }
              <div class="grid gap-3">
                @for (payment of payments(); track $index) {
                  <div class="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:flex-row md:items-center md:justify-between">
                    <div>
                      <p class="font-bold">{{ payment.description }}</p>
                      <p class="text-sm text-slate-500">{{ payment.percentage ? payment.percentage + '%' : 'Valor livre' }} · {{ payment.dueDate ? (payment.dueDate | date:'dd/MM/yyyy') : 'Sem vencimento' }}</p>
                    </div>
                    <div class="flex items-center gap-3">
                      <strong>R$ {{ payment.value | number:'1.2-2' }}</strong>
                      @if (!readOnly()) {
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="removePayment($index)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      }
                    </div>
                  </div>
                } @empty {
                  <p class="rounded-2xl border border-dashed border-slate-200 p-5 text-center text-sm text-slate-500">Nenhuma condição cadastrada.</p>
                }
              </div>
            </section>
          }

          @if (activeTab() === 'notes') {
            <section class="grid gap-4 md:grid-cols-2">
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Escopo</span>
                <textarea class="field min-h-40" placeholder="O que está incluído na proposta" formControlName="scope"></textarea>
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Exclusões</span>
                <textarea class="field min-h-40" placeholder="O que não está incluído" formControlName="exclusions"></textarea>
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Observações para cliente</span>
                <textarea class="field min-h-40" placeholder="Texto visível na proposta e no portal" formControlName="clientNotes"></textarea>
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Observações internas</span>
                <textarea class="field min-h-40" placeholder="Notas internas do escritório" formControlName="internalNotes"></textarea>
              </label>
            </section>
          }

          @if (activeTab() === 'summary') {
            <section class="grid gap-4 md:grid-cols-[1fr_24rem]">
              <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Resumo</p>
                <h4 class="mt-3 text-2xl font-extrabold">{{ proposalForm.controls.title.value || 'Nova proposta' }}</h4>
                <p class="mt-2 text-sm text-slate-500">{{ selectedClientName() || 'Selecione um cliente' }}</p>
                <p class="mt-6 text-sm text-slate-600">{{ items().length }} serviço(s) e {{ payments().length }} condição(ões) de pagamento.</p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white p-5">
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Desconto geral</span>
                  <app-arqly-currency-input formControlName="discount" />
                </label>
                <label class="mt-3 block space-y-1">
                  <span class="text-xs font-bold text-slate-500">Acréscimo</span>
                  <app-arqly-currency-input formControlName="addition" />
                </label>
                <div class="mt-5 space-y-3 text-sm">
                  <div class="flex justify-between"><span>Subtotal</span><strong>R$ {{ subtotal() | number:'1.2-2' }}</strong></div>
                  <div class="flex justify-between"><span>Desconto</span><strong>R$ {{ proposalForm.controls.discount.value || 0 | number:'1.2-2' }}</strong></div>
                  <div class="flex justify-between"><span>Acréscimo</span><strong>R$ {{ proposalForm.controls.addition.value || 0 | number:'1.2-2' }}</strong></div>
                  <div class="border-t border-slate-200 pt-3">
                    <div class="flex justify-between text-lg"><span class="font-extrabold">Valor final</span><strong class="text-arqly-700">R$ {{ finalTotal() | number:'1.2-2' }}</strong></div>
                  </div>
                </div>
              </div>
            </section>
          }

          @if (activeTab() === 'documents') {
            @if (readOnly() && editingDetail()) {
              <app-context-documents [proposalId]="editingDetail()!.id" />
            }
          }

          <div class="flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center md:justify-between">
            <p class="text-sm text-slate-500">{{ editingDetail()?.number || 'A numeração será gerada ao salvar.' }}</p>
            <div class="flex justify-end gap-3">
              <button class="btn-secondary" type="button" (click)="closeModal()">Fechar</button>
              @if (!readOnly()) {
                <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button>
              }
            </div>
          </div>
        </form>
      </div>
    }

    @if (confirmModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-[60] grid place-items-center p-4">
        <section class="modal-panel card w-full max-w-md space-y-5 p-6">
          <div class="flex items-start gap-4">
            <span class="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl"
                  [class.bg-arqly-50]="confirmAction() === 'send'"
                  [class.text-arqly-700]="confirmAction() === 'send'"
                  [class.bg-red-50]="confirmAction() === 'delete'"
                  [class.text-red-700]="confirmAction() === 'delete'"
                  [class.bg-amber-50]="confirmAction() === 'createProject'"
                  [class.text-amber-700]="confirmAction() === 'createProject'">
              <lucide-icon [name]="confirmIcon()" size="22"></lucide-icon>
            </span>
            <div>
              <h3 class="text-xl font-extrabold">{{ confirmTitle() }}</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ confirmDescription() }}</p>
            </div>
          </div>
          <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-slate-500">Proposta</p>
            <p class="mt-2 font-extrabold text-slate-900">{{ selectedProposal()?.number }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ selectedProposal()?.title }}</p>
          </div>
          @if (confirmAction() === 'createProject') {
            <form class="grid gap-3" [formGroup]="projectCreateForm">
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Nome do projeto <span class="text-red-500">*</span></span>
                <input class="field" formControlName="name" placeholder="Nome do projeto">
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Modelo</span>
                <app-arqly-select formControlName="templateId" placeholder="Sem modelo" [options]="projectTemplateOptions()" panelMode="fixed" />
              </label>
              <div class="grid gap-3 md:grid-cols-2">
                <div class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Início</span>
                  <app-arqly-date-picker formControlName="startDate" placeholder="Selecione" />
                </div>
                <div class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Previsão</span>
                  <app-arqly-date-picker formControlName="expectedEndDate" placeholder="Selecione" />
                </div>
              </div>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Responsável</span>
                <app-arqly-select formControlName="responsibleUserId" placeholder="Selecione um usuário" [options]="tenantUserOptions()" panelMode="fixed" />
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Gerente do projeto</span>
                <app-arqly-select formControlName="projectManagerId" placeholder="Opcional" [options]="tenantUserOptions(true)" panelMode="fixed" />
              </label>
            </form>
          }
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeConfirmModal()">Cancelar</button>
            <button class="btn-primary" type="button"
                    [class.bg-red-600]="confirmAction() === 'delete'"
                    [class.hover:bg-red-700]="confirmAction() === 'delete'"
                    (click)="confirmProposalAction()">
              {{ confirmButtonLabel() }}
            </button>
          </div>
        </section>
      </div>
    }
  `
})
export class ProposalsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = 'http://localhost:8080/api/tenant/proposals';

  readonly proposals = signal<ProposalSummary[]>([]);
  readonly clients = signal<ClientOption[]>([]);
  readonly briefings = signal<BriefingOption[]>([]);
  readonly services = signal<CatalogService[]>([]);
  readonly projectTemplates = signal<ProjectTemplateOption[]>([]);
  readonly tenantUsers = signal<TenantUserOption[]>([]);
  readonly stats = signal<ProposalStats | null>(null);
  readonly items = signal<ProposalItem[]>([]);
  readonly payments = signal<PaymentCondition[]>([]);
  readonly modalOpen = signal(false);
  readonly readOnly = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly editingDetail = signal<ProposalDetail | null>(null);
  readonly proposalOriginMode = signal<'BRIEFING' | 'MANUAL'>('MANUAL');
  readonly activeTab = signal<ProposalTab>('data');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly confirmModalOpen = signal(false);
  readonly confirmAction = signal<ProposalConfirmAction>('send');
  readonly selectedProposal = signal<ProposalSummary | null>(null);
  readonly projectCreateForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    templateId: [''],
    startDate: [''],
    expectedEndDate: [''],
    responsibleUserId: [''],
    projectManagerId: [''],
    responsibleArchitect: [''],
    internalNotes: ['']
  });

  readonly tabs: { label: string; value: ProposalTab }[] = [
    { label: 'Dados', value: 'data' },
    { label: 'Serviços', value: 'services' },
    { label: 'Pagamentos', value: 'payments' },
    { label: 'Observações', value: 'notes' },
    { label: 'Resumo', value: 'summary' },
    { label: 'Documentos', value: 'documents' }
  ];
  readonly statusFilterOptions = [
    { label: 'Todos', value: '' },
    { label: 'Rascunho', value: 'DRAFT' },
    { label: 'Enviada', value: 'SENT' },
    { label: 'Visualizada', value: 'VIEWED' },
    { label: 'Aceita', value: 'ACCEPTED' },
    { label: 'Recusada', value: 'REJECTED' },
    { label: 'Expirada', value: 'EXPIRED' },
    { label: 'Cancelada', value: 'CANCELLED' }
  ];

  readonly filterForm = this.fb.nonNullable.group({
    number: [''],
    clientId: [''],
    status: [''],
    from: [''],
    to: [''],
    minValue: [''],
    maxValue: ['']
  });
  readonly proposalForm = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    briefingId: [''],
    title: ['', Validators.required],
    description: [''],
    validUntil: [''],
    discount: [0],
    addition: [0],
    scope: [''],
    exclusions: [''],
    internalNotes: [''],
    clientNotes: ['']
  });
  readonly itemForm = this.fb.nonNullable.group({
    serviceId: ['', Validators.required],
    quantity: [1, Validators.required],
    unitValue: [0],
    discount: [0],
    customDescription: ['']
  });
  readonly paymentForm = this.fb.nonNullable.group({
    description: ['', Validators.required],
    percentage: [0],
    value: [0],
    dueDate: ['']
  });

  ngOnInit() {
    this.loadOptions();
    this.proposalForm.controls.briefingId.valueChanges.subscribe((id) => this.applyBriefingDefaults(id));
    this.itemForm.controls.serviceId.valueChanges.subscribe((id) => this.applyServiceDefaults(id));
    this.paymentForm.controls.percentage.valueChanges.subscribe(() => this.updatePaymentValueFromPercentage());
    this.proposalForm.controls.discount.valueChanges.subscribe(() => this.updatePaymentValueFromPercentage());
    this.proposalForm.controls.addition.valueChanges.subscribe(() => this.updatePaymentValueFromPercentage());
    this.reload();
  }

  reload() {
    this.load();
    this.loadStats();
  }

  load() {
    const params = this.paramsFrom(this.filterForm.getRawValue());
    params.set('page', String(this.page()));
    params.set('size', '10');
    params.set('sort', 'createdAt,desc');
    this.http.get<ApiResponse<Page<ProposalSummary>>>(`${this.baseUrl}?${params}`)
      .subscribe((response) => {
        this.proposals.set(response.data.content);
        this.page.set(response.data.number);
        this.totalPages.set(response.data.totalPages);
        this.totalElements.set(response.data.totalElements);
      });
  }

  loadStats() {
    this.http.get<ApiResponse<ProposalStats>>(`${this.baseUrl}/stats`).subscribe((response) => this.stats.set(response.data));
  }

  loadOptions() {
    this.http.get<ApiResponse<Page<ClientOption>>>('http://localhost:8080/api/tenant/clients?page=0&size=200&sort=createdAt,desc')
      .subscribe((response) => this.clients.set(response.data.content));
    this.http.get<ApiResponse<Page<BriefingOption>>>('http://localhost:8080/api/tenant/briefings?page=0&size=200&sort=createdAt,desc')
      .subscribe((response) => this.briefings.set(response.data.content));
    this.http.get<ApiResponse<Page<CatalogService>>>('http://localhost:8080/api/tenant/service-catalog/services?page=0&size=200&sort=name,asc&active=true')
      .subscribe((response) => this.services.set(response.data.content));
    this.http.get<ApiResponse<ProjectTemplateOption[]>>('http://localhost:8080/api/tenant/projects/templates/options')
      .subscribe((response) => this.projectTemplates.set(response.data));
    this.http.get<ApiResponse<Page<TenantUserOption>>>('http://localhost:8080/api/tenant/users?size=200&sort=name,asc')
      .subscribe((response) => this.tenantUsers.set(response.data.content));
  }

  search() {
    this.page.set(0);
    this.load();
  }

  previousPage() {
    if (this.page() === 0) return;
    this.page.update((page) => page - 1);
    this.load();
  }

  nextPage() {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.update((page) => page + 1);
    this.load();
  }

  openModal(id?: string, readOnly = false) {
    this.readOnly.set(readOnly);
    this.editingId.set(id || null);
    this.editingDetail.set(null);
    this.proposalOriginMode.set('MANUAL');
    this.activeTab.set('data');
    this.items.set([]);
    this.payments.set([]);
    this.proposalForm.enable();
    this.itemForm.enable();
    this.paymentForm.enable();
    this.proposalForm.reset({
      clientId: '',
      briefingId: '',
      title: '',
      description: '',
      validUntil: '',
      discount: 0,
      addition: 0,
      scope: '',
      exclusions: '',
      internalNotes: '',
      clientNotes: ''
    });
    if (id) {
      this.http.get<ApiResponse<ProposalDetail>>(`${this.baseUrl}/${id}`).subscribe((response) => {
        const proposal = response.data;
        this.editingDetail.set(proposal);
        this.proposalOriginMode.set(proposal.briefingId ? 'BRIEFING' : 'MANUAL');
        this.proposalForm.patchValue({
          clientId: proposal.clientId,
          briefingId: proposal.briefingId || '',
          title: proposal.title,
          description: proposal.description || '',
          validUntil: proposal.validUntil || '',
          discount: proposal.discount || 0,
          addition: proposal.addition || 0,
          scope: proposal.scope || '',
          exclusions: proposal.exclusions || '',
          internalNotes: proposal.internalNotes || '',
          clientNotes: proposal.clientNotes || ''
        });
        this.items.set(proposal.items || []);
        this.payments.set(proposal.paymentConditions || []);
        if (readOnly) {
          this.proposalForm.disable();
          this.itemForm.disable();
          this.paymentForm.disable();
        }
      });
    }
    if (readOnly) {
      this.proposalForm.disable();
      this.itemForm.disable();
      this.paymentForm.disable();
    }
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  setProposalOriginMode(mode: 'BRIEFING' | 'MANUAL') {
    this.proposalOriginMode.set(mode);
    if (mode === 'MANUAL') this.proposalForm.patchValue({ briefingId: '' });
  }

  applyServiceDefaults(id: string) {
    if (!id || this.readOnly()) return;
    const service = this.services().find((item) => item.id === id);
    if (!service) return;
    this.itemForm.controls.unitValue.setValue(Number(service.baseValue || 0), { emitEvent: false });
  }

  updatePaymentValueFromPercentage() {
    if (this.readOnly()) return;
    const percentage = Number(this.paymentForm.controls.percentage.value || 0);
    if (percentage <= 0) return;
    this.paymentForm.controls.value.setValue(this.roundCurrency(this.finalTotal() * percentage / 100), { emitEvent: false });
  }

  addItem() {
    if (this.readOnly()) return;
    if (this.itemForm.invalid) {
      this.toast.validation('Selecione um serviço e informe a quantidade.');
      return;
    }
    const value = this.itemForm.getRawValue();
    const service = this.services().find((item) => item.id === value.serviceId);
    if (!service) return;
    const unitValue = Number(value.unitValue || service.baseValue || 0);
    const quantity = Number(value.quantity || 1);
    const discount = Number(value.discount || 0);
    const total = Math.max(quantity * unitValue - discount, 0);
    this.items.update((items) => [...items, {
      serviceId: service.id,
      serviceName: service.name,
      serviceDescription: service.shortDescription,
      customDescription: value.customDescription,
      quantity,
      unit: service.billingUnit,
      unitValue,
      discount,
      total
    }]);
    this.updatePaymentValueFromPercentage();
    this.itemForm.reset({ serviceId: '', quantity: 1, unitValue: 0, discount: 0, customDescription: '' });
  }

  removeItem(index: number) {
    if (this.readOnly()) return;
    this.items.update((items) => items.filter((_, current) => current !== index));
    this.updatePaymentValueFromPercentage();
  }

  addPayment() {
    if (this.readOnly()) return;
    if (this.paymentForm.invalid) {
      this.toast.validation('Informe a descrição da condição de pagamento.');
      return;
    }
    const value = this.paymentForm.getRawValue();
    const percentage = Number(value.percentage || 0);
    const amount = percentage > 0
      ? this.roundCurrency(this.finalTotal() * percentage / 100)
      : this.roundCurrency(Number(value.value || 0));
    this.payments.update((items) => [...items, {
      description: value.description,
      percentage: percentage || null,
      value: amount,
      dueDate: value.dueDate || null
    }]);
    this.paymentForm.reset({ description: '', percentage: 0, value: 0, dueDate: '' });
  }

  removePayment(index: number) {
    if (this.readOnly()) return;
    this.payments.update((items) => items.filter((_, current) => current !== index));
  }

  save() {
    if (this.proposalOriginMode() === 'BRIEFING' && !this.proposalForm.controls.briefingId.value) {
      this.toast.validation('Selecione o briefing de origem.');
      return;
    }
    if (this.proposalForm.invalid || this.items().length === 0) {
      this.proposalForm.markAllAsTouched();
      this.toast.validation('Informe cliente, título e pelo menos um serviço.');
      return;
    }
    const id = this.editingId();
    const payload = this.payload();
    const request = id
      ? this.http.put<ApiResponse<ProposalDetail>>(`${this.baseUrl}/${id}`, payload)
      : this.http.post<ApiResponse<ProposalDetail>>(this.baseUrl, payload);
    request.subscribe({
      next: () => {
        this.closeModal();
        this.reload();
        this.toast.success(id ? 'Proposta atualizada' : 'Proposta criada');
      },
      error: () => this.toast.error('Não foi possível salvar a proposta')
    });
  }

  openConfirmModal(action: ProposalConfirmAction, proposal: ProposalSummary) {
    this.confirmAction.set(action);
    this.selectedProposal.set(proposal);
    if (action === 'createProject') {
      this.projectCreateForm.reset({
        name: proposal.title,
        templateId: '',
        startDate: '',
        expectedEndDate: '',
        responsibleUserId: '',
        projectManagerId: '',
        responsibleArchitect: '',
        internalNotes: ''
      });
    }
    this.confirmModalOpen.set(true);
  }

  closeConfirmModal() {
    this.confirmModalOpen.set(false);
    this.selectedProposal.set(null);
  }

  confirmTitle() {
    return {
      send: 'Enviar proposta',
      delete: 'Excluir proposta',
      createProject: 'Criar projeto'
    }[this.confirmAction()];
  }

  confirmDescription() {
    return {
      send: 'O cliente receberá a proposta por e-mail e poderá visualizar, aceitar ou recusar pelo portal. É necessário ter um acesso ativo ao portal do cliente.',
      delete: 'Essa ação remove o rascunho da lista. Propostas enviadas, aceitas ou convertidas não podem ser excluídas.',
      createProject: 'Um novo projeto será criado a partir da proposta aceita, mantendo o vínculo comercial registrado.'
    }[this.confirmAction()];
  }

  confirmIcon() {
    return {
      send: 'Send',
      delete: 'Trash2',
      createProject: 'FolderPlus'
    }[this.confirmAction()];
  }

  confirmButtonLabel() {
    return {
      send: 'Enviar proposta',
      delete: 'Excluir',
      createProject: 'Criar projeto'
    }[this.confirmAction()];
  }

  confirmProposalAction() {
    const proposal = this.selectedProposal();
    if (!proposal) return;
    if (this.confirmAction() === 'send') {
      this.sendProposal(proposal);
      return;
    }
    if (this.confirmAction() === 'createProject') {
      if (this.projectCreateForm.invalid) {
        this.projectCreateForm.markAllAsTouched();
        this.toast.validation('Informe o nome do projeto.');
        return;
      }
      this.createProject(proposal);
      return;
    }
    this.deleteProposal(proposal);
  }

  sendProposal(proposal: ProposalSummary) {
    this.http.post<ApiResponse<ProposalDetail>>(`${this.baseUrl}/${proposal.id}/send`, {
      message: 'Sua proposta comercial está pronta para análise.'
    }).subscribe({
      next: () => {
        this.closeConfirmModal();
        this.reload();
        this.toast.success('Proposta enviada');
      },
      error: () => this.toast.error('Não foi possível enviar', 'Verifique SMTP e acesso ativo ao portal do cliente.')
    });
  }

  duplicate(proposal: ProposalSummary) {
    this.http.post<ApiResponse<ProposalDetail>>(`${this.baseUrl}/${proposal.id}/duplicate`, {}).subscribe(() => {
      this.reload();
      this.toast.success('Proposta duplicada');
    });
  }

  deleteProposal(proposal: ProposalSummary) {
    this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${proposal.id}`).subscribe({
      next: () => {
        this.closeConfirmModal();
        this.reload();
        this.toast.success('Proposta excluída');
      },
      error: () => this.toast.error('Não foi possível excluir a proposta', 'Somente rascunhos podem ser excluídos.')
    });
  }

  createProject(proposal: ProposalSummary) {
    const value = this.projectCreateForm.getRawValue();
    this.http.post<ApiResponse<unknown>>(`http://localhost:8080/api/tenant/projects/from-proposal/${proposal.id}`, {
      ...value,
      templateId: value.templateId || null,
      startDate: value.startDate || null,
      expectedEndDate: value.expectedEndDate || null,
      responsibleUserId: value.responsibleUserId || null,
      projectManagerId: value.projectManagerId || null,
      internalNotes: value.internalNotes || null
    }).subscribe({
      next: () => {
        this.closeConfirmModal();
        this.reload();
        this.toast.success('Projeto criado', 'O vínculo com a proposta foi registrado.');
      },
      error: () => this.toast.error('Não foi possível criar o projeto')
    });
  }

  downloadPdf(proposal: ProposalSummary) {
    this.http.get(`${this.baseUrl}/${proposal.id}/pdf`, { responseType: 'blob' }).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${proposal.number}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  clientFilterOptions() {
    return [{ label: 'Todos', value: '' }, ...this.clients().map((client) => ({ label: client.displayName, value: client.id }))];
  }

  clientFormOptions() {
    return this.clients().map((client) => ({ label: client.displayName, value: client.id }));
  }

  briefingOptions() {
    const clientId = this.proposalForm.controls.clientId.value;
    return this.briefings()
      .filter((briefing) => !briefing.proposalGenerated || briefing.id === this.proposalForm.controls.briefingId.value)
      .filter((briefing) => !clientId || briefing.clientId === clientId)
      .map((briefing) => ({ label: briefing.title, value: briefing.id }));
  }

  applyBriefingDefaults(id: string) {
    if (!id || this.readOnly()) return;
    this.http.get<ApiResponse<any>>(`http://localhost:8080/api/tenant/briefings/${id}`).subscribe((response) => {
      const briefing = response.data;
      const requirements = (briefing.requirements || []).map((item: any) => item.description).filter(Boolean);
      const scope = [
        briefing.projectTemplateName ? `Modelo: ${briefing.projectTemplateName}` : '',
        briefing.approximateArea ? `Área aproximada: ${briefing.approximateArea} m²` : '',
        briefing.workAddress ? `Endereço da obra: ${briefing.workAddress}` : '',
        requirements.length ? `Programa de necessidades: ${requirements.join('; ')}` : ''
      ].filter(Boolean).join('\n');
      this.proposalForm.patchValue({
        clientId: briefing.clientId || this.proposalForm.controls.clientId.value,
        title: this.proposalForm.controls.title.value || briefing.title || '',
        description: this.proposalForm.controls.description.value || briefing.description || '',
        scope: this.proposalForm.controls.scope.value || scope,
        internalNotes: this.proposalForm.controls.internalNotes.value || briefing.preferenceNotes || '',
        clientNotes: this.proposalForm.controls.clientNotes.value || briefing.restrictionNotes || ''
      }, { emitEvent: false });
    });
  }

  serviceOptions() {
    return this.services().map((service) => ({ label: service.name, value: service.id }));
  }

  projectTemplateOptions() {
    return [{ label: 'Sem modelo', value: '' }, ...this.projectTemplates().map((template) => ({ label: template.name, value: template.id }))];
  }

  tenantUserOptions(includeEmpty = false) {
    const options = this.tenantUsers().map((user) => ({ label: `${user.name} · ${user.tenantAdmin ? 'Administrador' : 'Usuário comum'}`, value: user.id }));
    return includeEmpty ? [{ label: 'Sem gerente', value: '' }, ...options] : options;
  }

  selectedClientName() {
    return this.clients().find((client) => client.id === this.proposalForm.controls.clientId.value)?.displayName;
  }

  subtotal() {
    return this.items().reduce((total, item) => total + Number(item.total || 0), 0);
  }

  finalTotal() {
    return Math.max(this.subtotal() - Number(this.proposalForm.controls.discount.value || 0) + Number(this.proposalForm.controls.addition.value || 0), 0);
  }

  private roundCurrency(value: number) {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  statusLabel(status: ProposalStatus) {
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

  statusClass(status: ProposalStatus) {
    if (status === 'ACCEPTED') return 'bg-arqly-50 text-arqly-700';
    if (status === 'REJECTED' || status === 'CANCELLED') return 'bg-red-50 text-red-700';
    if (status === 'EXPIRED') return 'bg-slate-100 text-slate-500';
    if (status === 'SENT' || status === 'VIEWED') return 'bg-amber-50 text-amber-700';
    return 'bg-slate-100 text-slate-600';
  }

  originLabel(origin: OriginType) { return origin === 'BRIEFING' ? 'Briefing' : origin === 'PROPOSAL' ? 'Proposta' : 'Manual'; }
  originClass(origin: OriginType) { return origin === 'BRIEFING' ? 'bg-arqly-50 text-arqly-700' : 'bg-blue-50 text-blue-700'; }

  unitLabel(unit: BillingUnit) {
    return {
      UN: 'UN',
      M2: 'm²',
      M: 'm',
      HOUR: 'Hora',
      DAY: 'Dia',
      MONTH: 'Mês',
      PROJECT: 'Projeto',
      VISIT: 'Visita',
      OTHER: 'Outro'
    }[unit] || unit;
  }

  private paramsFrom(values: Record<string, unknown>) {
    const params = new URLSearchParams();
    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') params.set(key, String(value));
    });
    return params;
  }

  private payload() {
    const value = this.proposalForm.getRawValue();
    return {
      ...value,
      briefingId: this.proposalOriginMode() === 'BRIEFING' ? value.briefingId || null : null,
      validUntil: value.validUntil || null,
      discount: Number(value.discount || 0),
      addition: Number(value.addition || 0),
      items: this.items().map((item) => ({
        serviceId: item.serviceId,
        customDescription: item.customDescription || null,
        quantity: item.quantity,
        unit: item.unit,
        unitValue: item.unitValue,
        discount: item.discount
      })),
      paymentConditions: this.payments().map((payment) => ({
        description: payment.description,
        percentage: payment.percentage || null,
        value: payment.value,
        dueDate: payment.dueDate || null
      }))
    };
  }
}
