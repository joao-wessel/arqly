import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyCurrencyInputComponent } from '../../shared/components/arqly-currency-input.component';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent, ArqlySelectOption } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type BriefingStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';
type BriefingTab = 'general' | 'projectInfo' | 'requirements' | 'preferences' | 'restrictions' | 'summary';

interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; }
interface ClientOption { id: string; displayName: string; email?: string; }
interface ProjectTemplateOption { id: string; name: string; }
interface TenantUserOption { id: string; name: string; email: string; tenantAdmin: boolean; }
interface BriefingRequirement { id?: string; description: string; order: number; }
interface BriefingSummary {
  id: string; clientId: string; clientName: string; projectTemplateId?: string | null; projectTemplateName?: string | null;
  responsibleUserId?: string | null; responsibleName?: string | null; title: string; status: BriefingStatus;
  proposalGenerated: boolean; proposalId?: string | null; proposalNumber?: string | null; createdAt: string; updatedAt: string;
}
interface BriefingDetail extends BriefingSummary {
  description?: string | null; approximateArea?: number | null; workAddress?: string | null; city?: string | null; state?: string | null;
  desiredDeadline?: string | null; expectedBudget?: number | null; architecturalStyle?: string | null; colorPalette?: string | null;
  desiredMaterials?: string | null; preferenceNotes?: string | null; legalRestrictions?: string | null; technicalRestrictions?: string | null;
  clientRestrictions?: string | null; restrictionNotes?: string | null; requirements: BriefingRequirement[];
}
interface BriefingStats { total: number; inProgress: number; completed: number; converted: number; }

@Component({
  selector: 'app-briefings',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    RouterLink,
    LucideAngularModule,
    ArqlySelectComponent,
    ArqlyDatePickerComponent,
    ArqlyCurrencyInputComponent
  ],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Primeiro contato</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Briefings</h2>
          <p class="mt-2 text-slate-500">Organize o programa de necessidades antes de transformar a demanda em proposta.</p>
        </div>
        <button class="btn-primary" type="button" (click)="openModal()">
          <lucide-icon name="Plus" size="18"></lucide-icon>
          Novo briefing
        </button>
      </div>

      <div class="grid gap-4 md:grid-cols-4">
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Briefings</p><strong class="mt-2 block text-3xl">{{ stats()?.total || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Em andamento</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.inProgress || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Concluídos</p><strong class="mt-2 block text-3xl">{{ stats()?.completed || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Convertidos</p><strong class="mt-2 block text-3xl text-emerald-600">{{ stats()?.converted || 0 }}</strong></div>
      </div>

      <div class="card overflow-hidden">
        <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-5" [formGroup]="filterForm" (ngSubmit)="search()">
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Busca</span>
            <input class="field" formControlName="search" placeholder="Título ou descrição">
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Cliente</span>
            <app-arqly-select formControlName="clientId" placeholder="Todos" [options]="clientOptions()" />
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Responsável</span>
            <app-arqly-select formControlName="responsibleUserId" placeholder="Todos" [options]="userOptions()" />
          </label>
          <label class="space-y-1">
            <span class="text-xs font-bold text-slate-500">Status</span>
            <app-arqly-select formControlName="status" placeholder="Todos" [options]="statusOptions" />
          </label>
          <button class="btn-secondary h-12 self-end justify-center px-4" type="submit">
            <lucide-icon name="Search" size="18"></lucide-icon>
            Pesquisar
          </button>
        </form>

        <div class="hidden overflow-x-auto md:block">
          <table class="w-full min-w-[980px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th class="px-6 py-4">Título</th>
                <th class="px-6 py-4">Cliente</th>
                <th class="px-6 py-4">Modelo</th>
                <th class="px-6 py-4">Responsável</th>
                <th class="px-6 py-4">Status</th>
                <th class="px-6 py-4">Proposta</th>
                <th class="px-6 py-4">Data</th>
                <th class="px-6 py-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (briefing of briefings(); track briefing.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-6 py-4 font-bold">{{ briefing.title }}</td>
                  <td class="px-6 py-4 text-slate-600">{{ briefing.clientName }}</td>
                  <td class="px-6 py-4 text-slate-600">{{ briefing.projectTemplateName || 'A definir' }}</td>
                  <td class="px-6 py-4 text-slate-600">{{ briefing.responsibleName || 'A definir' }}</td>
                  <td class="px-6 py-4"><span class="badge" [class.bg-arqly-50]="briefing.status !== 'ARCHIVED'" [class.text-arqly-700]="briefing.status !== 'ARCHIVED'">{{ statusLabel(briefing.status) }}</span></td>
                  <td class="px-6 py-4">
                    @if (briefing.proposalGenerated) {
                      <a class="font-bold text-arqly-700" [routerLink]="['/app/proposals']">{{ briefing.proposalNumber }}</a>
                    } @else {
                      <span class="text-slate-400">Não gerada</span>
                    }
                  </td>
                  <td class="px-6 py-4 text-slate-500">{{ briefing.createdAt | date:'dd/MM/yyyy' }}</td>
                  <td class="px-6 py-4">
                    <div class="flex justify-end gap-2">
                      <button class="btn-secondary px-3 py-2" type="button" title="Visualizar" (click)="openModal(briefing.id, true)"><lucide-icon name="Search" size="16"></lucide-icon></button>
                      <a class="btn-secondary px-3 py-2" title="Arquivos" [routerLink]="['/app/files', 'BRIEFING', briefing.id]"><lucide-icon name="Archive" size="16"></lucide-icon></a>
                      <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openModal(briefing.id)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" title="Gerar proposta" [disabled]="briefing.proposalGenerated" (click)="generateProposal(briefing)"><lucide-icon name="FileText" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2 text-red-600" type="button" title="Excluir" [disabled]="briefing.proposalGenerated" (click)="openDeleteModal(briefing)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="8" class="px-6 py-10 text-center text-slate-500">Nenhum briefing encontrado.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <div class="grid gap-3 p-4 md:hidden">
          @for (briefing of briefings(); track briefing.id) {
            <article class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="font-extrabold">{{ briefing.title }}</p>
                  <p class="mt-1 text-sm text-slate-500">{{ briefing.clientName }}</p>
                </div>
                <span class="badge bg-arqly-50 text-arqly-700">{{ statusLabel(briefing.status) }}</span>
              </div>
              <div class="mt-4 flex flex-wrap gap-2">
                <button class="btn-secondary px-3 py-2" type="button" (click)="openModal(briefing.id)">Abrir</button>
                <button class="btn-secondary px-3 py-2" type="button" [disabled]="briefing.proposalGenerated" (click)="generateProposal(briefing)">Gerar proposta</button>
              </div>
            </article>
          }
        </div>
      </div>

      @if (modalOpen()) {
        <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
          <form class="modal-panel card max-h-[92vh] w-full max-w-4xl space-y-5 overflow-y-auto p-6" [formGroup]="form" (ngSubmit)="save()">
            <div class="flex items-center justify-between">
              <h3 class="text-xl font-extrabold">{{ selectedBriefing() ? (readOnly() ? 'Visualizar briefing' : 'Editar briefing') : 'Novo briefing' }}</h3>
              <button class="btn-secondary px-3 py-2" type="button" (click)="closeModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
            </div>

            <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-6">
              @for (tab of tabs; track tab.id) {
                <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                        [class.bg-white]="activeTab() === tab.id"
                        [class.text-arqly-700]="activeTab() === tab.id"
                        [class.shadow-sm]="activeTab() === tab.id"
                        [class.text-slate-500]="activeTab() !== tab.id"
                        (click)="activeTab.set(tab.id)">
                  {{ tab.label }}
                </button>
              }
            </div>

            @if (activeTab() === 'general') {
              <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Dados gerais</p>
                  <p class="mt-1 text-sm text-slate-500">Defina o cliente, o modelo e as informa&ccedil;&otilde;es iniciais da demanda.</p>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">T&iacute;tulo <span class="text-red-500">*</span></span>
                    <input class="field" formControlName="title" placeholder="Ex.: Resid&ecirc;ncia Vila Nova">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Cliente <span class="text-red-500">*</span></span>
                    <app-arqly-select formControlName="clientId" placeholder="Selecione o cliente" [options]="clientOptions()" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Modelo de projeto</span>
                    <app-arqly-select formControlName="projectTemplateId" placeholder="Selecione o modelo" [options]="templateOptions()" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Respons&aacute;vel</span>
                    <app-arqly-select formControlName="responsibleUserId" placeholder="Selecione um usu&aacute;rio" [options]="userOptions()" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Status</span>
                    <app-arqly-select formControlName="status" placeholder="Selecione o status" [options]="statusOptions" />
                  </label>
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Descri&ccedil;&atilde;o</span>
                    <textarea class="field min-h-28" formControlName="description" placeholder="Contexto, objetivos e informa&ccedil;&otilde;es iniciais do cliente"></textarea>
                  </label>
                </div>
              </section>
            }

            @if (activeTab() === 'projectInfo') {
              <section class="rounded-2xl border border-slate-200 bg-white p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Informa&ccedil;&otilde;es do projeto</p>
                  <p class="mt-1 text-sm text-slate-500">Registre dados iniciais da obra para orientar proposta e projeto.</p>
                </div>
                <div class="grid gap-4 md:grid-cols-3">
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">&Aacute;rea aproximada</span>
                    <input class="field" type="number" formControlName="approximateArea" placeholder="Ex.: 180">
                  </label>
                  <div class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Prazo desejado</span>
                    <app-arqly-date-picker formControlName="desiredDeadline" placeholder="Selecione a data" />
                  </div>
                  <div class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Or&ccedil;amento previsto</span>
                    <app-arqly-currency-input formControlName="expectedBudget" />
                  </div>
                  <label class="space-y-1 md:col-span-3">
                    <span class="text-xs font-bold text-slate-500">Endere&ccedil;o da obra</span>
                    <input class="field" formControlName="workAddress" placeholder="Rua, n&uacute;mero e complemento">
                  </label>
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Cidade</span>
                    <input class="field" formControlName="city" placeholder="Cidade da obra">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Estado</span>
                    <input class="field uppercase" maxlength="2" formControlName="state" placeholder="UF">
                  </label>
                </div>
              </section>
            }

            @if (activeTab() === 'requirements') {
              <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div class="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div>
                    <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Programa de necessidades</p>
                    <p class="mt-1 text-sm text-slate-500">Liste ambientes, desejos e entregas esperadas pelo cliente.</p>
                  </div>
                  <button class="btn-secondary" type="button" (click)="addRequirement()" [disabled]="readOnly()"><lucide-icon name="Plus" size="16"></lucide-icon>Adicionar</button>
                </div>
                <div class="space-y-3">
                  @for (item of requirements(); track $index) {
                    <div class="grid gap-3 rounded-2xl border border-slate-200 bg-white p-3 md:grid-cols-[auto_1fr_auto] md:items-center">
                      <span class="grid h-10 w-10 place-items-center rounded-xl bg-arqly-50 text-sm font-extrabold text-arqly-700">{{ $index + 1 }}</span>
                      <input class="field" [value]="item.description" [disabled]="readOnly()" placeholder="Ex.: 3 su&iacute;tes, &aacute;rea gourmet, escrit&oacute;rio" (input)="updateRequirement($index, $any($event.target).value)">
                      <div class="flex gap-2">
                        <button class="btn-secondary px-3 py-2" type="button" [disabled]="$index === 0 || readOnly()" (click)="moveRequirement($index, -1)"><lucide-icon name="ChevronUp" size="15"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" [disabled]="$index === requirements().length - 1 || readOnly()" (click)="moveRequirement($index, 1)"><lucide-icon name="ChevronDown" size="15"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" [disabled]="readOnly()" (click)="removeRequirement($index)"><lucide-icon name="Trash2" size="15"></lucide-icon></button>
                      </div>
                    </div>
                  } @empty {
                    <div class="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center text-slate-500">Nenhum item informado.</div>
                  }
                </div>
              </section>
            }

            @if (activeTab() === 'preferences') {
              <section class="rounded-2xl border border-slate-200 bg-white p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Prefer&ecirc;ncias</p>
                  <p class="mt-1 text-sm text-slate-500">Registre refer&ecirc;ncias est&eacute;ticas, materiais e expectativas do cliente.</p>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Estilo arquitet&ocirc;nico</span>
                    <input class="field" formControlName="architecturalStyle" placeholder="Ex.: contempor&acirc;neo, minimalista">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Paleta de cores</span>
                    <input class="field" formControlName="colorPalette" placeholder="Tons, refer&ecirc;ncias e prefer&ecirc;ncias">
                  </label>
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Materiais desejados</span>
                    <textarea class="field min-h-28" formControlName="desiredMaterials" placeholder="Madeira, concreto aparente, pedra natural..."></textarea>
                  </label>
                  <label class="space-y-1 md:col-span-2">
                    <span class="text-xs font-bold text-slate-500">Observa&ccedil;&otilde;es</span>
                    <textarea class="field min-h-28" formControlName="preferenceNotes" placeholder="Prefer&ecirc;ncias gerais e detalhes importantes"></textarea>
                  </label>
                </div>
              </section>
            }

            @if (activeTab() === 'restrictions') {
              <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Restri&ccedil;&otilde;es</p>
                  <p class="mt-1 text-sm text-slate-500">Mapeie limites legais, t&eacute;cnicos e decis&otilde;es importantes do cliente.</p>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Restri&ccedil;&otilde;es legais</span>
                    <textarea class="field min-h-28" formControlName="legalRestrictions" placeholder="Normas, condom&iacute;nio, prefeitura, zoneamento"></textarea>
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Restri&ccedil;&otilde;es t&eacute;cnicas</span>
                    <textarea class="field min-h-28" formControlName="technicalRestrictions" placeholder="Estrutura existente, instala&ccedil;&otilde;es, limita&ccedil;&otilde;es construtivas"></textarea>
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Restri&ccedil;&otilde;es do cliente</span>
                    <textarea class="field min-h-28" formControlName="clientRestrictions" placeholder="Prazos, or&ccedil;amento, materiais vetados, necessidades espec&iacute;ficas"></textarea>
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Observa&ccedil;&otilde;es</span>
                    <textarea class="field min-h-28" formControlName="restrictionNotes" placeholder="Notas adicionais sobre restri&ccedil;&otilde;es"></textarea>
                  </label>
                </div>
              </section>
            }

            @if (activeTab() === 'summary') {
              <section class="rounded-2xl border border-slate-200 bg-white p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Resumo</p>
                  <p class="mt-1 text-sm text-slate-500">Revise as principais informa&ccedil;&otilde;es antes de gerar a proposta.</p>
                </div>
                <div class="grid gap-4 lg:grid-cols-[1fr_0.8fr]">
                  <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                    <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Briefing</p>
                    <h5 class="mt-2 text-2xl font-extrabold text-slate-950">{{ form.value.title || untitledLabel }}</h5>
                    <p class="mt-3 text-sm text-slate-500">{{ form.value.description || noDescriptionLabel }}</p>
                    <div class="mt-5 grid gap-3 text-sm text-slate-600">
                      <span><strong class="text-slate-900">Cliente:</strong> {{ selectedClientName() || 'A definir' }}</span>
                      <span><strong class="text-slate-900">Modelo:</strong> {{ selectedTemplateName() || 'A definir' }}</span>
                      <span><strong class="text-slate-900">Respons&aacute;vel:</strong> {{ selectedUserName() || 'A definir' }}</span>
                      <span><strong class="text-slate-900">Prazo:</strong> {{ form.value.desiredDeadline || 'A definir' }}</span>
                    </div>
                  </div>
                  <div class="rounded-2xl border border-slate-200 bg-white p-5">
                    <h5 class="text-lg font-extrabold text-slate-950">Necessidades</h5>
                    <ul class="mt-4 space-y-2 text-sm text-slate-600">
                      @for (item of requirements(); track $index) {
                        <li class="rounded-xl bg-slate-50 px-3 py-2">{{ item.description }}</li>
                      } @empty {
                        <li class="text-slate-400">Nenhum item informado.</li>
                      }
                    </ul>
                  </div>
                </div>
              </section>
            }

            <div class="flex justify-end gap-3">
              <button class="btn-secondary" type="button" (click)="closeModal()">Fechar</button>
              @if (selectedBriefing() && !selectedBriefing()?.proposalGenerated) {
                <button class="btn-secondary" type="button" (click)="generateProposal(selectedBriefing()!)"><lucide-icon name="FileText" size="18"></lucide-icon>Gerar proposta</button>
              }
              @if (!readOnly()) {
                <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button>
              }
            </div>
          </form>
        </div>
      }

      @if (deleteModalOpen()) {
        <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
          <section class="modal-panel w-full max-w-md space-y-5 p-6">
            <div class="flex gap-4">
              <span class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-red-50 text-red-600">
                <lucide-icon name="Trash2" size="22"></lucide-icon>
              </span>
              <div>
                <h3 class="text-xl font-extrabold">Excluir briefing</h3>
                <p class="mt-2 text-sm leading-6 text-slate-500">Essa ação remove o briefing da lista. Briefings com proposta gerada não podem ser excluídos.</p>
              </div>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-slate-500">Briefing</p>
              <p class="mt-2 font-extrabold">{{ briefingToDelete()?.title }}</p>
              <p class="mt-1 text-sm text-slate-500">{{ briefingToDelete()?.clientName }}</p>
            </div>
            <div class="flex justify-end gap-3">
              <button class="btn-secondary" type="button" (click)="closeDeleteModal()">Cancelar</button>
              <button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="confirmDelete()">Excluir</button>
            </div>
          </section>
        </div>
      }
    </section>
  `
})
export class BriefingsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = '/api/tenant/briefings';

  readonly briefings = signal<BriefingSummary[]>([]);
  readonly clients = signal<ClientOption[]>([]);
  readonly templates = signal<ProjectTemplateOption[]>([]);
  readonly users = signal<TenantUserOption[]>([]);
  readonly stats = signal<BriefingStats | null>(null);
  readonly modalOpen = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly briefingToDelete = signal<BriefingSummary | null>(null);
  readonly readOnly = signal(false);
  readonly selectedBriefing = signal<BriefingDetail | null>(null);
  readonly activeTab = signal<BriefingTab>('general');
  readonly requirements = signal<BriefingRequirement[]>([]);

  readonly untitledLabel = 'Briefing sem t\u00edtulo';
  readonly noDescriptionLabel = 'Sem descri\u00e7\u00e3o.';

  readonly tabs: { id: BriefingTab; label: string }[] = [
    { id: 'general', label: 'Dados Gerais' },
    { id: 'projectInfo', label: 'Projeto' },
    { id: 'requirements', label: 'Programa' },
    { id: 'preferences', label: 'Prefer\u00eancias' },
    { id: 'restrictions', label: 'Restri\u00e7\u00f5es' },
    { id: 'summary', label: 'Resumo' }
  ];
  readonly statusOptions: ArqlySelectOption[] = [
    { label: 'Rascunho', value: 'DRAFT' },
    { label: 'Em andamento', value: 'IN_PROGRESS' },
    { label: 'Conclu\u00eddo', value: 'COMPLETED' },
    { label: 'Arquivado', value: 'ARCHIVED' }
  ];

  readonly filterForm = this.fb.nonNullable.group({ search: [''], clientId: [''], responsibleUserId: [''], status: [''] });
  readonly form = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    projectTemplateId: [''],
    responsibleUserId: [''],
    title: ['', Validators.required],
    description: [''],
    status: ['DRAFT'],
    approximateArea: [null as number | null],
    workAddress: [''],
    city: [''],
    state: [''],
    desiredDeadline: [''],
    expectedBudget: [null as number | null],
    architecturalStyle: [''],
    colorPalette: [''],
    desiredMaterials: [''],
    preferenceNotes: [''],
    legalRestrictions: [''],
    technicalRestrictions: [''],
    clientRestrictions: [''],
    restrictionNotes: ['']
  });

  ngOnInit() {
    this.loadOptions();
    this.load();
    this.loadStats();
  }

  load() {
    const params = new URLSearchParams({ size: '50', sort: 'createdAt,desc' });
    Object.entries(this.filterForm.getRawValue()).forEach(([key, value]) => { if (value) params.set(key, String(value)); });
    this.http.get<ApiResponse<Page<BriefingSummary>>>(`${this.baseUrl}?${params}`).subscribe((response) => this.briefings.set(response.data.content));
  }

  loadStats() {
    this.http.get<ApiResponse<BriefingStats>>(`${this.baseUrl}/stats`).subscribe((response) => this.stats.set(response.data));
  }

  loadOptions() {
    this.http.get<ApiResponse<Page<ClientOption>>>('/api/tenant/clients?size=200&sort=name,asc')
      .subscribe((response) => this.clients.set(response.data.content));
    this.http.get<ApiResponse<ProjectTemplateOption[]>>('/api/tenant/projects/templates/options')
      .subscribe((response) => this.templates.set(response.data));
    this.http.get<ApiResponse<Page<TenantUserOption>>>('/api/tenant/users?size=200&sort=name,asc')
      .subscribe((response) => this.users.set(response.data.content));
  }

  search() { this.load(); }

  openModal(id?: string, readOnly = false) {
    this.readOnly.set(readOnly);
    this.activeTab.set('general');
    this.selectedBriefing.set(null);
    this.requirements.set([]);
    this.form.reset({ clientId: '', projectTemplateId: '', responsibleUserId: '', title: '', description: '', status: 'DRAFT',
      approximateArea: null, workAddress: '', city: '', state: '', desiredDeadline: '', expectedBudget: null,
      architecturalStyle: '', colorPalette: '', desiredMaterials: '', preferenceNotes: '', legalRestrictions: '',
      technicalRestrictions: '', clientRestrictions: '', restrictionNotes: '' });
    this.form.enable();
    if (readOnly) this.form.disable();
    this.modalOpen.set(true);
    if (id) {
      this.http.get<ApiResponse<BriefingDetail>>(`${this.baseUrl}/${id}`).subscribe((response) => {
        const briefing = response.data;
        this.selectedBriefing.set(briefing);
        this.form.patchValue({
          clientId: briefing.clientId,
          projectTemplateId: briefing.projectTemplateId || '',
          responsibleUserId: briefing.responsibleUserId || '',
          title: briefing.title,
          description: briefing.description || '',
          status: briefing.status,
          approximateArea: briefing.approximateArea || null,
          workAddress: briefing.workAddress || '',
          city: briefing.city || '',
          state: briefing.state || '',
          desiredDeadline: briefing.desiredDeadline || '',
          expectedBudget: briefing.expectedBudget || null,
          architecturalStyle: briefing.architecturalStyle || '',
          colorPalette: briefing.colorPalette || '',
          desiredMaterials: briefing.desiredMaterials || '',
          preferenceNotes: briefing.preferenceNotes || '',
          legalRestrictions: briefing.legalRestrictions || '',
          technicalRestrictions: briefing.technicalRestrictions || '',
          clientRestrictions: briefing.clientRestrictions || '',
          restrictionNotes: briefing.restrictionNotes || ''
        });
        this.requirements.set((briefing.requirements || []).map((item, index) => ({ ...item, order: index + 1 })));
        if (readOnly) this.form.disable();
      });
    }
  }

  closeModal() { this.modalOpen.set(false); }

  save() {
    if (this.form.invalid) {
      this.toast.error('Preencha os campos obrigatórios do briefing.');
      this.form.markAllAsTouched();
      return;
    }
    const payload = { ...this.form.getRawValue(), requirements: this.requirements().filter((item) => item.description.trim()).map((item, index) => ({ description: item.description.trim(), order: index + 1 })) };
    const current = this.selectedBriefing();
    const request = current
      ? this.http.put<ApiResponse<BriefingDetail>>(`${this.baseUrl}/${current.id}`, payload)
      : this.http.post<ApiResponse<BriefingDetail>>(this.baseUrl, payload);
    request.subscribe({
      next: () => { this.toast.success('Briefing salvo.'); this.closeModal(); this.load(); this.loadStats(); },
      error: () => this.toast.error('Não foi possível salvar o briefing.')
    });
  }

  generateProposal(briefing: BriefingSummary | BriefingDetail) {
    this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${briefing.id}/generate-proposal`, {}).subscribe({
      next: () => { this.toast.success('Proposta gerada a partir do briefing.'); this.closeModal(); this.load(); this.loadStats(); },
      error: () => this.toast.error('Não foi possível gerar a proposta.')
    });
  }

  openDeleteModal(briefing: BriefingSummary) {
    this.briefingToDelete.set(briefing);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
    this.briefingToDelete.set(null);
  }

  confirmDelete() {
    const briefing = this.briefingToDelete();
    if (!briefing) return;
    this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${briefing.id}`).subscribe({
      next: () => { this.toast.success('Briefing removido.'); this.closeDeleteModal(); this.load(); this.loadStats(); },
      error: () => this.toast.error('Não foi possível remover o briefing.')
    });
  }

  addRequirement() {
    this.requirements.update((items) => [...items, { description: '', order: items.length + 1 }]);
  }

  updateRequirement(index: number, description: string) {
    this.requirements.update((items) => items.map((item, itemIndex) => itemIndex === index ? { ...item, description } : item));
  }

  removeRequirement(index: number) {
    this.requirements.update((items) => items.filter((_, itemIndex) => itemIndex !== index).map((item, itemIndex) => ({ ...item, order: itemIndex + 1 })));
  }

  moveRequirement(index: number, direction: number) {
    this.requirements.update((items) => {
      const next = [...items];
      const target = index + direction;
      [next[index], next[target]] = [next[target], next[index]];
      return next.map((item, itemIndex) => ({ ...item, order: itemIndex + 1 }));
    });
  }

  clientOptions(): ArqlySelectOption[] {
    return this.clients().map((client) => ({ label: client.displayName, value: client.id }));
  }

  templateOptions(): ArqlySelectOption[] {
    return this.templates().map((template) => ({ label: template.name, value: template.id }));
  }

  userOptions(): ArqlySelectOption[] {
    return this.users().map((user) => ({ label: `${user.name} · ${user.tenantAdmin ? 'Administrador' : 'Usuário comum'}`, value: user.id }));
  }

  selectedClientName() { return this.clients().find((client) => client.id === this.form.value.clientId)?.displayName || ''; }
  selectedTemplateName() { return this.templates().find((template) => template.id === this.form.value.projectTemplateId)?.name || ''; }
  selectedUserName() { return this.users().find((user) => user.id === this.form.value.responsibleUserId)?.name || ''; }

  statusLabel(status: BriefingStatus) {
    return { DRAFT: 'Rascunho', IN_PROGRESS: 'Em andamento', COMPLETED: 'Concluído', ARCHIVED: 'Arquivado' }[status];
  }
}
