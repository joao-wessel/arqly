import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ActivityFeedComponent } from '../../shared/components/activity-feed.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { FileExplorerComponent } from '../../shared/files/file-explorer.component';

type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'WAITING_APPROVAL' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type ProgressMode = 'MANUAL' | 'CHECKLIST' | 'DATES' | 'HYBRID';
type WorkspaceTab = 'summary' | 'checklist' | 'activities' | 'files' | 'diary';

interface ProjectInfo { id: string; code: string; name: string; clientName: string; proposalId: string; proposalNumber: string; }
interface PhaseInfo { id: string; name: string; color?: string | null; icon?: string | null; completionPercentage: number; }
interface ChecklistItem {
  id: string; description: string; completed: boolean; order: number; completedAt?: string | null; completedBy?: string | null; notes?: string | null;
}
interface Page<T> { content: T[]; }
interface TenantUserOption { id: string; name: string; email: string; tenantAdmin: boolean; }
interface StageWorkspace {
  id: string; name: string; description?: string | null; status: StageStatus; completionPercentage: number; weightPercentage: number;
  progressCalculationMode: ProgressMode; responsibleUserId?: string | null; responsibleName?: string | null; responsible?: string | null; plannedStart?: string | null; plannedEnd?: string | null;
  actualStart?: string | null; actualEnd?: string | null; remainingDays: number; notes?: string | null; project: ProjectInfo; phase: PhaseInfo;
  checklist: ChecklistItem[];
}
@Component({
  selector: 'app-stage-workspace',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, LucideAngularModule, ArqlySelectComponent, ArqlyDatePickerComponent, ActivityFeedComponent, FileExplorerComponent],
  template: `
    <section class="space-y-5">
      @if (workspace(); as stage) {
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div class="min-w-0">
            <nav class="flex flex-wrap items-center gap-2 text-xs font-extrabold uppercase tracking-[0.18em] text-slate-400">
              <a class="text-arqly-700" routerLink="/app/projects">Projetos</a>
              <span>/</span>
              <a class="text-arqly-700" [routerLink]="['/app/projects', stage.project.id]">{{ stage.project.name }}</a>
              <span>/</span>
              <span>{{ stage.phase.name }}</span>
              <span>/</span>
              <span>{{ stage.name }}</span>
            </nav>
            <div class="mt-4 flex flex-wrap items-center gap-3">
              <span class="grid h-12 w-12 place-items-center rounded-2xl text-white" [style.background]="stage.phase.color || 'var(--arqly-600)'">
                <lucide-icon [name]="stage.phase.icon || 'ListChecks'" size="20"></lucide-icon>
              </span>
              <div>
                <h2 class="text-3xl font-extrabold tracking-tight">{{ stage.name }}</h2>
                <div class="mt-2 flex flex-wrap gap-2 text-xs font-bold">
                  <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-500">Cliente: {{ stage.project.clientName }}</span>
                  <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-500">Projeto: {{ stage.project.code }}</span>
                  <span class="rounded-full bg-slate-100 px-3 py-1 text-slate-500">Proposta: {{ stage.project.proposalNumber }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex flex-wrap gap-2">
            <a class="btn-secondary" [routerLink]="['/app/projects', stage.project.id]"><lucide-icon name="ArrowLeft" size="16"></lucide-icon>Voltar</a>
            <button class="btn-secondary" type="button" (click)="startEdit(stage)"><lucide-icon name="Pencil" size="16"></lucide-icon>Editar</button>
            <button class="btn-primary" type="button" (click)="complete()"><lucide-icon name="Check" size="16"></lucide-icon>Concluir etapa</button>
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Status</p><span class="mt-3 inline-flex rounded-full px-3 py-1 text-xs font-bold" [class]="stageStatusClass(stage.status)">{{ stageStatusLabel(stage.status) }}</span></div>
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Percentual</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stage.completionPercentage || 0 | number:'1.0-0' }}%</strong></div>
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Responsável</p><strong class="mt-2 block truncate text-lg">{{ stage.responsible || 'A definir' }}</strong></div>
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Início previsto</p><strong class="mt-2 block text-lg">{{ stage.plannedStart ? (stage.plannedStart | date:'dd/MM/yyyy') : '-' }}</strong></div>
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Fim previsto</p><strong class="mt-2 block text-lg">{{ stage.plannedEnd ? (stage.plannedEnd | date:'dd/MM/yyyy') : '-' }}</strong></div>
          <div class="card p-5"><p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Dias restantes</p><strong class="mt-2 block text-3xl" [class.text-red-600]="stage.remainingDays < 0">{{ stage.remainingDays }}</strong></div>
        </div>

        <div class="card overflow-hidden">
          <div class="border-b border-slate-200 p-2">
            <div class="flex gap-2 overflow-x-auto">
              @for (tab of tabs; track tab.value) {
                <button class="whitespace-nowrap rounded-2xl px-4 py-3 text-sm font-bold transition"
                  type="button"
                  [class.bg-arqly-600]="activeTab() === tab.value"
                  [class.text-white]="activeTab() === tab.value"
                  [class.text-slate-500]="activeTab() !== tab.value"
                  (click)="activeTab.set(tab.value)">
                  {{ tab.label }}
                </button>
              }
            </div>
          </div>

          @if (activeTab() === 'summary') {
            <div class="grid gap-5 p-5 xl:grid-cols-[1.2fr_0.8fr]" [class.items-stretch]="!editingStage()">
              <div [class.space-y-5]="editingStage()" [class.grid]="!editingStage()" [class.h-full]="!editingStage()" [class.grid-rows-2]="!editingStage()" [class.gap-5]="!editingStage()">
                @if (editingStage()) {
                  <form class="card grid gap-4 bg-slate-50/70 p-5" [formGroup]="stageForm" (ngSubmit)="saveStage()">
                    <div>
                      <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Editar etapa</p>
                      <h3 class="mt-1 text-xl font-extrabold">Informações operacionais</h3>
                    </div>
                    <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Nome da etapa *</span><input class="field w-full" formControlName="name" placeholder="Ex.: Anteprojeto" /></label>
                    <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Descrição</span><textarea class="field w-full min-h-24" formControlName="description" placeholder="Descreva o objetivo desta etapa"></textarea></label>
                    <div class="grid gap-4 md:grid-cols-2">
                      <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Status</span><app-arqly-select formControlName="status" placeholder="Selecione" [options]="stageStatusOptions" panelMode="fixed" /></label>
                      <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Modo de cálculo</span><app-arqly-select formControlName="progressCalculationMode" placeholder="Selecione" [options]="progressModeOptions" panelMode="fixed" /></label>
                    </div>
                    <div class="grid gap-4 md:grid-cols-3">
                      <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Percentual</span><input class="field w-full" type="number" min="0" max="100" formControlName="completionPercentage" [readonly]="stageForm.controls.progressCalculationMode.value === 'CHECKLIST'" /></label>
                      <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Peso</span><input class="field w-full" type="number" min="0" max="100" formControlName="weightPercentage" readonly /></label>
                      <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Responsável</span><app-arqly-select formControlName="responsibleUserId" placeholder="Herdar do projeto" [options]="tenantUserOptions(true)" panelMode="fixed" /></label>
                    </div>
                    <div class="grid gap-4 md:grid-cols-4">
                      <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Início previsto</span><app-arqly-date-picker formControlName="plannedStart" placeholder="Selecione" /></div>
                      <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Fim previsto</span><app-arqly-date-picker formControlName="plannedEnd" placeholder="Selecione" /></div>
                      <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Início real</span><app-arqly-date-picker formControlName="actualStart" placeholder="Selecione" /></div>
                      <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Fim real</span><app-arqly-date-picker formControlName="actualEnd" placeholder="Selecione" /></div>
                    </div>
                    <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Observações</span><textarea class="field w-full min-h-24" formControlName="notes" placeholder="Observações internas da etapa"></textarea></label>
                    <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="editingStage.set(false)">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="16"></lucide-icon>Salvar</button></div>
                  </form>
                } @else {
                  <div class="card bg-slate-50/70 p-5">
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Resumo da etapa</p>
                        <h3 class="mt-1 text-xl font-extrabold">{{ stage.name }}</h3>
                      </div>
                      <span class="w-fit rounded-full bg-white px-3 py-1 text-xs font-bold text-slate-500">{{ progressModeLabel(stage.progressCalculationMode) }}</span>
                    </div>
                    <div class="mt-5 grid gap-4 lg:grid-cols-2">
                      <div class="rounded-2xl border border-slate-200 bg-white p-4">
                        <p class="text-xs font-bold uppercase tracking-[0.16em] text-slate-400">Descrição</p>
                        <p class="mt-3 text-sm leading-6 text-slate-600">{{ stage.description || 'Sem descrição cadastrada.' }}</p>
                      </div>
                      <div class="rounded-2xl border border-slate-200 bg-white p-4">
                        <p class="text-xs font-bold uppercase tracking-[0.16em] text-slate-400">Observações</p>
                        <p class="mt-3 text-sm leading-6 text-slate-600">{{ stage.notes || 'Nenhuma observação registrada.' }}</p>
                      </div>
                    </div>
                  </div>
                  <div class="grid h-full gap-4 lg:grid-cols-2">
                    <div class="card bg-slate-50/70 p-5">
                      <p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Datas</p>
                      <div class="mt-4 grid gap-3 text-sm">
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Início previsto</span><strong>{{ stage.plannedStart ? (stage.plannedStart | date:'dd/MM/yyyy') : '-' }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Fim previsto</span><strong>{{ stage.plannedEnd ? (stage.plannedEnd | date:'dd/MM/yyyy') : '-' }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Início real</span><strong>{{ stage.actualStart ? (stage.actualStart | date:'dd/MM/yyyy') : '-' }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Fim real</span><strong>{{ stage.actualEnd ? (stage.actualEnd | date:'dd/MM/yyyy') : '-' }}</strong></div>
                      </div>
                    </div>
                    <div class="card bg-slate-50/70 p-5">
                      <p class="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Contexto</p>
                      <div class="mt-4 grid gap-3 text-sm">
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Projeto</span><strong class="text-right">{{ stage.project.name }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Fase</span><strong class="text-right">{{ stage.phase.name }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Cliente</span><strong class="text-right">{{ stage.project.clientName }}</strong></div>
                        <div class="flex justify-between gap-4"><span class="font-bold text-slate-400">Proposta</span><strong class="text-right">{{ stage.project.proposalNumber }}</strong></div>
                      </div>
                    </div>
                  </div>
                }
              </div>
              <aside [class.space-y-4]="editingStage()" [class.grid]="!editingStage()" [class.h-full]="!editingStage()" [class.grid-rows-3]="!editingStage()" [class.gap-4]="!editingStage()">
                <div class="card p-5">
                  <div class="flex items-center justify-between"><p class="text-sm font-extrabold">Progresso da etapa</p><strong class="text-arqly-700">{{ stage.completionPercentage || 0 | number:'1.0-0' }}%</strong></div>
                  <div class="mt-4 h-3 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="stage.completionPercentage || 0"></div></div>
                  <p class="mt-3 text-xs font-semibold text-slate-500">Cálculo: {{ progressModeLabel(stage.progressCalculationMode) }}</p>
                </div>
                <div class="card p-5">
                  <p class="text-sm font-extrabold">Checklist</p>
                  <strong class="mt-3 block text-3xl text-arqly-700">{{ completedChecklistCount(stage) }} de {{ stage.checklist.length }}</strong>
                  <p class="mt-2 text-xs font-semibold text-slate-500">Itens concluídos nesta etapa.</p>
                </div>
                <div class="card p-5">
                  <p class="text-sm font-extrabold">Peso no projeto</p>
                  <strong class="mt-3 block text-3xl">{{ stage.weightPercentage || 0 | number:'1.0-0' }}%</strong>
                  <p class="mt-2 text-xs font-semibold text-slate-500">A fase está em {{ stage.phase.completionPercentage || 0 | number:'1.0-0' }}%.</p>
                </div>
              </aside>
            </div>
          }

          @if (activeTab() === 'checklist') {
            <div class="space-y-5 p-5">
              <form class="card grid gap-3 bg-slate-50/70 p-4 md:grid-cols-[1fr_260px_auto]" [formGroup]="checklistForm" (ngSubmit)="addChecklistItem()">
                <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Novo item</span><input class="field w-full" formControlName="description" placeholder="Ex.: Planta baixa finalizada" /></label>
                <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Observações</span><input class="field w-full" formControlName="notes" placeholder="Opcional" /></label>
                <button class="btn-primary self-end" type="submit"><lucide-icon name="Plus" size="16"></lucide-icon>Adicionar</button>
              </form>
              <div class="space-y-3">
                @for (item of stage.checklist; track item.id) {
                  <div class="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-[auto_1fr_auto]" draggable="true" (dragstart)="startChecklistDrag(item)" (dragover)="allowDrop($event)" (drop)="dropChecklistItem(item)">
                    <input class="checkbox mt-1" type="checkbox" [checked]="item.completed" (change)="toggleChecklist(item, $any($event.target).checked)" />
                    <div>
                      @if (editingChecklistId() === item.id) {
                        <div class="grid gap-3 rounded-2xl bg-slate-50/70 p-3 md:grid-cols-2">
                          <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Descrição</span><input class="field w-full" [value]="item.description" #itemDescription placeholder="Descrição do item" /></label>
                          <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Observações</span><input class="field w-full" [value]="item.notes || ''" #itemNotes placeholder="Observações" /></label>
                          <div class="flex gap-2 md:col-span-2">
                            <button class="btn-primary py-2" type="button" (click)="saveChecklistItem(item, itemDescription.value, itemNotes.value)">Salvar</button>
                            <button class="btn-secondary py-2" type="button" (click)="editingChecklistId.set(null)">Cancelar</button>
                          </div>
                        </div>
                      } @else {
                        <p class="font-bold" [class.line-through]="item.completed" [class.text-slate-400]="item.completed">{{ item.description }}</p>
                        <p class="mt-1 text-xs font-semibold text-slate-400">{{ item.notes || 'Sem observações' }}</p>
                        @if (item.completed) {
                          <p class="mt-1 text-xs font-bold text-arqly-700">Concluído por {{ item.completedBy || 'sistema' }} em {{ item.completedAt | date:'dd/MM/yyyy HH:mm' }}</p>
                        }
                      }
                    </div>
                    <div class="flex items-start justify-end gap-2">
                      <button class="btn-secondary px-3 py-2" type="button" (click)="editingChecklistId.set(item.id)"><lucide-icon name="Pencil" size="15"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateChecklistItem(item)"><lucide-icon name="Copy" size="15"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteChecklistItem(item)"><lucide-icon name="Trash2" size="15"></lucide-icon></button>
                    </div>
                  </div>
                } @empty {
                  <p class="rounded-2xl bg-slate-50/70 py-12 text-center text-sm text-slate-500">Nenhum item de checklist cadastrado para esta etapa.</p>
                }
              </div>
            </div>
          }

          @if (activeTab() === 'activities') {
            <div class="p-5">
              <app-activity-feed
                [projectId]="stage.project.id"
                [phaseId]="stage.phase.id"
                [stageId]="stage.id" />
            </div>
          }

          @if (activeTab() === 'files') {
            <div class="p-5">
              <app-file-explorer ownerType="PROJECT_STAGE" [ownerId]="stage.id" title="Arquivos da etapa" eyebrow="Workspace da etapa" />
            </div>
          }

          @if (activeTab() === 'diary') {
            <div class="p-5">
              <div class="rounded-3xl border border-dashed border-slate-300 bg-slate-50/70 py-16 text-center">
                <lucide-icon class="mx-auto text-slate-400" name="ClipboardList" size="34"></lucide-icon>
                <p class="mt-4 font-extrabold">O Diário de Obra estará disponível em breve.</p>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="card p-8 text-center text-sm text-slate-500">Carregando workspace da etapa...</div>
      }
    </section>
  `
})
export class StageWorkspaceComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = 'http://localhost:8080/api/tenant/stage-workspaces';

  readonly workspace = signal<StageWorkspace | null>(null);
  readonly activeTab = signal<WorkspaceTab>('summary');
  readonly editingStage = signal(false);
  readonly editingChecklistId = signal<string | null>(null);
  readonly draggedChecklistItem = signal<ChecklistItem | null>(null);
  readonly tenantUsers = signal<TenantUserOption[]>([]);
  readonly projectId = signal<string>('');
  readonly stageId = signal<string>('');

  readonly tabs: { label: string; value: WorkspaceTab }[] = [
    { label: 'Resumo', value: 'summary' },
    { label: 'Checklist', value: 'checklist' },
    { label: 'Atividades', value: 'activities' },
    { label: 'Arquivos', value: 'files' },
    { label: 'Diário de Obra', value: 'diary' }
  ];
  readonly stageStatusOptions = [
    { label: 'Não iniciada', value: 'NOT_STARTED' },
    { label: 'Em andamento', value: 'IN_PROGRESS' },
    { label: 'Aguardando cliente', value: 'WAITING_CLIENT' },
    { label: 'Aguardando aprovação', value: 'WAITING_APPROVAL' },
    { label: 'Pausada', value: 'ON_HOLD' },
    { label: 'Concluída', value: 'COMPLETED' },
    { label: 'Cancelada', value: 'CANCELLED' }
  ];
  readonly progressModeOptions = [
    { label: 'Manual', value: 'MANUAL' },
    { label: 'Checklist', value: 'CHECKLIST' },
    { label: 'Datas', value: 'DATES' },
    { label: 'Híbrido', value: 'HYBRID' }
  ];

  readonly stageForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    status: ['NOT_STARTED'],
    completionPercentage: [0],
    weightPercentage: [0],
    progressCalculationMode: ['MANUAL'],
    responsibleUserId: [''],
    responsible: [''],
    plannedStart: [''],
    plannedEnd: [''],
    actualStart: [''],
    actualEnd: [''],
    notes: ['']
  });
  readonly checklistForm = this.fb.nonNullable.group({ description: ['', Validators.required], notes: [''] });

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      this.projectId.set(params.get('projectId') || '');
      this.stageId.set(params.get('stageId') || '');
      this.loadUsers();
      this.load();
    });
  }

  load() {
    if (!this.stageId()) return;
    this.http.get<ApiResponse<StageWorkspace>>(`${this.baseUrl}/${this.stageId()}`).subscribe({
      next: (response) => this.workspace.set(response.data || null),
      error: () => this.toast.error('Não foi possível carregar o workspace da etapa.')
    });
  }

  loadUsers() {
    this.http.get<ApiResponse<Page<TenantUserOption>>>('http://localhost:8080/api/tenant/users?size=200&sort=name,asc')
      .subscribe((response) => this.tenantUsers.set(response.data.content));
  }

  startEdit(stage: StageWorkspace) {
    this.stageForm.reset({
      name: stage.name || '',
      description: stage.description || '',
      status: stage.status,
      completionPercentage: stage.completionPercentage || 0,
      weightPercentage: stage.weightPercentage || 0,
      progressCalculationMode: stage.progressCalculationMode || 'MANUAL',
      responsibleUserId: stage.responsibleUserId || '',
      responsible: stage.responsible || '',
      plannedStart: stage.plannedStart || '',
      plannedEnd: stage.plannedEnd || '',
      actualStart: stage.actualStart || '',
      actualEnd: stage.actualEnd || '',
      notes: stage.notes || ''
    });
    this.activeTab.set('summary');
    this.editingStage.set(true);
  }

  saveStage() {
    if (this.stageForm.invalid) {
      this.stageForm.markAllAsTouched();
      this.toast.validation('Informe o nome da etapa.');
      return;
    }
    const raw = this.stageForm.getRawValue();
    this.http.put<ApiResponse<StageWorkspace>>(`${this.baseUrl}/${this.stageId()}`, { ...raw, responsibleUserId: raw.responsibleUserId || null }).subscribe({
      next: (response) => {
        this.workspace.set(response.data || null);
        this.editingStage.set(false);
        this.toast.success('Etapa atualizada.');
      },
      error: () => this.toast.error('Não foi possível salvar a etapa.')
    });
  }

  complete() { this.stageAction('complete', 'Etapa concluída.'); }
  pause() { this.stageAction('pause', 'Etapa pausada.'); }
  cancel() { this.stageAction('cancel', 'Etapa cancelada.'); }

  stageAction(action: string, message: string) {
    this.http.patch<ApiResponse<StageWorkspace>>(`${this.baseUrl}/${this.stageId()}/${action}`, {}).subscribe({
      next: (response) => { this.workspace.set(response.data || null); this.toast.success(message); },
      error: () => this.toast.error('Não foi possível atualizar a etapa.')
    });
  }

  addChecklistItem() {
    if (this.checklistForm.invalid) {
      this.checklistForm.markAllAsTouched();
      this.toast.validation('Informe a descrição do item.');
      return;
    }
    const current = this.workspace();
    const payload = { ...this.checklistForm.getRawValue(), order: (current?.checklist.length || 0) + 1 };
    this.http.post<ApiResponse<ChecklistItem>>(`${this.baseUrl}/${this.stageId()}/checklist`, payload).subscribe({
      next: () => { this.checklistForm.reset(); this.load(); this.toast.success('Item adicionado.'); },
      error: () => this.toast.error('Não foi possível adicionar o item.')
    });
  }

  saveChecklistItem(item: ChecklistItem, description: string, notes: string) {
    if (!description.trim()) {
      this.toast.validation('Informe a descrição do item.');
      return;
    }
    this.http.put<ApiResponse<ChecklistItem>>(`${this.baseUrl}/${this.stageId()}/checklist/${item.id}`, { description, notes, order: item.order }).subscribe({
      next: () => { this.editingChecklistId.set(null); this.load(); this.toast.success('Item atualizado.'); },
      error: () => this.toast.error('Não foi possível atualizar o item.')
    });
  }

  toggleChecklist(item: ChecklistItem, completed: boolean) {
    this.http.patch<ApiResponse<ChecklistItem>>(`${this.baseUrl}/${this.stageId()}/checklist/${item.id}/toggle`, { completed }).subscribe({
      next: () => this.load(),
      error: () => this.toast.error('Não foi possível atualizar o checklist.')
    });
  }

  duplicateChecklistItem(item: ChecklistItem) {
    this.http.post<ApiResponse<ChecklistItem>>(`${this.baseUrl}/${this.stageId()}/checklist/${item.id}/duplicate`, {}).subscribe({
      next: () => { this.load(); this.toast.success('Item duplicado.'); },
      error: () => this.toast.error('Não foi possível duplicar o item.')
    });
  }

  deleteChecklistItem(item: ChecklistItem) {
    this.http.patch<ApiResponse<void>>(`${this.baseUrl}/${this.stageId()}/checklist/${item.id}/delete`, {}).subscribe({
      next: () => { this.load(); this.toast.success('Item removido.'); },
      error: () => this.toast.error('Não foi possível remover o item.')
    });
  }

  startChecklistDrag(item: ChecklistItem) { this.draggedChecklistItem.set(item); }
  allowDrop(event: DragEvent) { event.preventDefault(); }

  dropChecklistItem(target: ChecklistItem) {
    const dragged = this.draggedChecklistItem();
    const current = this.workspace();
    if (!dragged || !current || dragged.id === target.id) return;
    const items = current.checklist.filter((item) => item.id !== dragged.id);
    const targetIndex = items.findIndex((item) => item.id === target.id);
    items.splice(targetIndex, 0, dragged);
    const payload = { items: items.map((item, index) => ({ id: item.id, order: index + 1 })) };
    this.http.patch<ApiResponse<ChecklistItem[]>>(`${this.baseUrl}/${this.stageId()}/checklist/reorder`, payload).subscribe({
      next: () => this.load(),
      error: () => this.toast.error('Não foi possível reordenar o checklist.')
    });
  }

  stageStatusLabel(status: StageStatus) {
    return ({ NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING_CLIENT: 'Aguardando cliente', WAITING_APPROVAL: 'Aguardando aprovação', ON_HOLD: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }[status]);
  }

  stageStatusClass(status: StageStatus) {
    if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700';
    if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700';
    if (status === 'ON_HOLD' || status.startsWith('WAITING')) return 'bg-amber-50 text-amber-700';
    if (status === 'CANCELLED') return 'bg-red-50 text-red-700';
    return 'bg-slate-100 text-slate-600';
  }

  progressModeLabel(mode: ProgressMode) {
    return ({ MANUAL: 'Manual', CHECKLIST: 'Checklist', DATES: 'Datas', HYBRID: 'Híbrido' }[mode]);
  }

  tenantUserOptions(includeEmpty = false) {
    const options = this.tenantUsers().map((user) => ({ label: `${user.name} · ${user.tenantAdmin ? 'Administrador' : 'Usuário comum'}`, value: user.id }));
    return includeEmpty ? [{ label: 'Herdar do projeto', value: '' }, ...options] : options;
  }

  completedChecklistCount(stage: StageWorkspace) {
    return stage.checklist.filter((item) => item.completed).length;
  }

}
