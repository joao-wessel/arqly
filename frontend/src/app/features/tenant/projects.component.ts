import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'WAITING_APPROVAL' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type ViewMode = 'projects' | 'templates';

interface Page<T> { content: T[]; number: number; totalElements: number; totalPages: number; }
interface ClientOption { id: string; displayName: string; }
interface ProjectTemplate { id: string; name: string; description?: string; active: boolean; stageCount: number; projectsUsing: number; createdAt: string; updatedAt: string; }
interface ProjectSummary {
  id: string; code: string; name: string; clientId: string; clientName: string; proposalId: string; proposalNumber: string;
  templateId?: string | null; templateName?: string | null; responsibleArchitect?: string | null; status: ProjectStatus;
  expectedEndDate?: string | null; contractedValue: number; progressPercentage: number; createdAt: string; updatedAt: string;
}
interface ProjectStats { active: number; completed: number; paused: number; cancelled: number; createdThisMonth: number; delayedStages: number; completedStages: number; activeStages: number; }
interface ChecklistItem { id?: string; title: string; order: number; completed?: boolean; completedAt?: string | null; }
interface StageTemplate {
  id: string; name: string; description?: string; order: number; color?: string; icon?: string; weightPercentage: number; active: boolean;
  checklist: ChecklistItem[]; createdAt: string; updatedAt: string;
}
interface ProjectStage {
  id: string; templateId?: string | null; dependsOnStageId?: string | null; name: string; description?: string; order: number; color?: string; icon?: string;
  status: StageStatus; plannedStart?: string | null; plannedEnd?: string | null; actualStart?: string | null; actualEnd?: string | null;
  completionPercentage: number; weightPercentage: number; responsible?: string | null; notes?: string | null; checklist: ChecklistItem[];
  createdAt: string; updatedAt: string;
}
interface ProjectDetail {
  id: string; code: string; name: string; description?: string; clientName: string; proposalNumber: string; templateId?: string | null; templateName?: string | null;
  responsibleArchitect?: string | null; status: ProjectStatus; startDate?: string | null; expectedEndDate?: string | null; completedAt?: string | null;
  contractedValue: number; progressPercentage: number; internalNotes?: string | null; services: any[]; stages: ProjectStage[];
}

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, RouterLink, ArqlySelectComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Execução</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Projetos</h2>
          <p class="mt-2 text-slate-500">Acompanhe projetos, modelos e etapas de execução.</p>
        </div>
        @if (mode() === 'templates') {
          <button class="btn-primary" type="button" (click)="openTemplateModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Novo modelo</button>
        }
      </div>

      <div class="grid gap-4 md:grid-cols-4 xl:grid-cols-8">
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Projetos ativos</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.active || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Concluídos</p><strong class="mt-2 block text-3xl">{{ stats()?.completed || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Pausados</p><strong class="mt-2 block text-3xl text-amber-600">{{ stats()?.paused || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Cancelados</p><strong class="mt-2 block text-3xl text-red-600">{{ stats()?.cancelled || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Este mês</p><strong class="mt-2 block text-3xl">{{ stats()?.createdThisMonth || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Etapas atrasadas</p><strong class="mt-2 block text-3xl text-red-600">{{ stats()?.delayedStages || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Etapas concluídas</p><strong class="mt-2 block text-3xl">{{ stats()?.completedStages || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Em andamento</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.activeStages || 0 }}</strong></div>
      </div>

      <div class="card overflow-hidden">
        <div class="flex flex-col gap-3 border-b border-slate-200 p-4 md:flex-row md:items-center md:justify-between">
          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-2">
            <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                    [class.bg-white]="mode() === 'projects'" [class.text-arqly-700]="mode() === 'projects'" [class.shadow-sm]="mode() === 'projects'" [class.text-slate-500]="mode() !== 'projects'"
                    (click)="switchMode('projects')">Projetos</button>
            <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                    [class.bg-white]="mode() === 'templates'" [class.text-arqly-700]="mode() === 'templates'" [class.shadow-sm]="mode() === 'templates'" [class.text-slate-500]="mode() !== 'templates'"
                    (click)="switchMode('templates')">Modelos de Projeto</button>
          </div>
        </div>

        @if (mode() === 'projects') {
          <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-7" [formGroup]="filterForm" (ngSubmit)="search()">
            <app-arqly-select formControlName="clientId" placeholder="Cliente" [options]="clientOptions()" />
            <app-arqly-select formControlName="status" placeholder="Situação" [options]="statusOptions" />
            <app-arqly-select formControlName="templateId" placeholder="Modelo" [options]="templateFilterOptions()" />
            <input class="field" placeholder="Responsável" formControlName="responsible">
            <input class="field" type="date" formControlName="from">
            <input class="field" type="date" formControlName="to">
            <button class="btn-secondary h-12 justify-center px-4" type="submit"><lucide-icon name="Search" size="18"></lucide-icon>Pesquisar</button>
          </form>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[1180px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th class="px-6 py-4">Código</th><th class="px-6 py-4">Nome</th><th class="px-6 py-4">Cliente</th><th class="px-6 py-4">Responsável</th><th class="px-6 py-4">Situação</th><th class="px-6 py-4">Progresso</th><th class="px-6 py-4">Previsão</th><th class="px-6 py-4">Valor</th><th class="px-6 py-4 text-right">Ações</th></tr>
              </thead>
              <tbody>
                @for (project of projects(); track project.id) {
                  <tr class="border-t border-slate-100">
                    <td class="px-6 py-4 font-extrabold">{{ project.code }}</td>
                    <td class="px-6 py-4"><p class="font-bold">{{ project.name }}</p><p class="text-xs text-slate-500">{{ project.templateName || 'Sem modelo' }}</p></td>
                    <td class="px-6 py-4">{{ project.clientName }}</td>
                    <td class="px-6 py-4 text-slate-600">{{ project.responsibleArchitect || '-' }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(project.status)">{{ statusLabel(project.status) }}</span></td>
                    <td class="px-6 py-4">
                      <div class="h-2 w-32 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="project.progressPercentage || 0"></div></div>
                      <span class="mt-1 block text-xs font-bold text-slate-500">{{ project.progressPercentage || 0 | number:'1.0-0' }}%</span>
                    </td>
                    <td class="px-6 py-4 text-slate-500">{{ project.expectedEndDate ? (project.expectedEndDate | date:'dd/MM/yyyy') : '-' }}</td>
                    <td class="px-6 py-4">R$ {{ project.contractedValue || 0 | number:'1.2-2' }}</td>
                    <td class="px-6 py-4 text-right">
                      <div class="flex justify-end gap-2">
                        <a class="btn-secondary px-3 py-2" [routerLink]="['/app/projects', project.id]"><lucide-icon name="Search" size="16"></lucide-icon>Ver</a>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" title="Arquivar" (click)="openProjectDeleteModal(project)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="9" class="px-6 py-12 text-center text-slate-500">Nenhum projeto encontrado.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="flex flex-col gap-3 border-t border-slate-200 p-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <span>{{ totalElements() }} projeto(s) encontrado(s)</span>
            <div class="flex items-center gap-2">
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() === 0" (click)="previousPage()">Anterior</button>
              <span class="font-bold text-slate-700">Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span>
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">Próxima</button>
            </div>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full min-w-[860px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th class="px-6 py-4">Nome</th><th class="px-6 py-4">Etapas</th><th class="px-6 py-4">Projetos usando</th><th class="px-6 py-4">Status</th><th class="px-6 py-4 text-right">Ações</th></tr>
              </thead>
              <tbody>
                @for (template of templates(); track template.id) {
                  <tr class="border-t border-slate-100" [class.bg-arqly-50]="selectedTemplate()?.id === template.id">
                    <td class="px-6 py-4"><p class="font-bold">{{ template.name }}</p><p class="text-xs text-slate-500">{{ template.description || '-' }}</p></td>
                    <td class="px-6 py-4">{{ template.stageCount || 0 }}</td>
                    <td class="px-6 py-4">{{ template.projectsUsing || 0 }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class.bg-arqly-50]="template.active" [class.text-arqly-700]="template.active" [class.bg-slate-100]="!template.active" [class.text-slate-500]="!template.active">{{ template.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td class="px-6 py-4">
                      <div class="flex justify-end gap-2">
                        <button class="btn-secondary px-3 py-2" type="button" (click)="selectTemplate(template)"><lucide-icon name="ListChecks" size="16"></lucide-icon>Etapas</button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="openTemplateModal(template)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateTemplate(template)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteTemplate(template)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="px-6 py-12 text-center text-slate-500">Nenhum modelo cadastrado.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      @if (mode() === 'templates' && selectedTemplate()) {
        <div class="card overflow-hidden">
          <div class="flex flex-col gap-3 border-b border-slate-200 p-5 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Editor do modelo</p>
              <h3 class="mt-1 text-xl font-extrabold">{{ selectedTemplate()!.name }}</h3>
            </div>
            <button class="btn-primary" type="button" (click)="openTemplateStageModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Adicionar etapa</button>
          </div>
          <div class="divide-y divide-slate-100 p-4">
            @for (stage of templateStages(); track stage.id) {
              <div class="grid gap-4 py-4 md:grid-cols-[auto_1fr_auto]" draggable="true" (dragstart)="startTemplateDrag(stage)" (dragover)="allowDrop($event)" (drop)="dropTemplateStage(stage)">
                <div class="grid h-11 w-11 place-items-center rounded-2xl text-white" [style.background]="stage.color || '#0f766e'"><lucide-icon [name]="stage.icon || 'ListChecks'" size="20"></lucide-icon></div>
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <strong>{{ stage.order }}. {{ stage.name }}</strong>
                    <span class="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-500">{{ stage.weightPercentage || 0 }}%</span>
                    @if (!stage.active) { <span class="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-500">Inativa</span> }
                  </div>
                  <p class="mt-1 text-sm text-slate-500">{{ stage.description || 'Sem descrição' }}</p>
                  <p class="mt-2 text-xs font-bold text-slate-400">{{ stage.checklist.length }} item(ns) de checklist</p>
                </div>
                <div class="flex items-center justify-end gap-2">
                  <button class="btn-secondary px-3 py-2" type="button" (click)="openTemplateStageModal(stage)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                  <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateTemplateStage(stage)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                  <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteTemplateStage(stage)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                </div>
              </div>
            } @empty {
              <p class="py-10 text-center text-sm text-slate-500">Nenhuma etapa cadastrada para este modelo.</p>
            }
          </div>
        </div>
      }
    </section>

    @if (templateModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-xl space-y-5 p-6" [formGroup]="templateForm" (ngSubmit)="saveTemplate()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ editingTemplate() ? 'Editar modelo' : 'Novo modelo' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeTemplateModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Residencial"></label>
          <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-28" formControlName="description" placeholder="Descrição do modelo"></textarea></label>
          <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3"><span><span class="block text-sm font-bold text-slate-800">Modelo ativo</span><span class="mt-1 block text-xs text-slate-500">Disponível para novos projetos.</span></span><input class="checkbox" type="checkbox" formControlName="active"></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeTemplateModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button></div>
        </form>
      </div>
    }

    @if (templateStageModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-3xl space-y-5 overflow-y-auto p-6" [formGroup]="stageTemplateForm" (ngSubmit)="saveTemplateStage()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ editingTemplateStage() ? 'Editar etapa' : 'Nova etapa' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeTemplateStageModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cor</span><input class="field h-12" type="color" formControlName="color"></label>
            <app-arqly-select formControlName="icon" placeholder="Ícone" [options]="iconOptions" panelMode="fixed" />
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Peso (%)</span><input class="field" type="number" min="0" max="100" formControlName="weightPercentage"></label>
            <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3"><span class="text-sm font-bold text-slate-800">Ativa</span><input class="checkbox" type="checkbox" formControlName="active"></label>
          </div>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Checklist padrão</span><textarea class="field min-h-32" formControlName="checklistText" placeholder="Um item por linha"></textarea></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeTemplateStageModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar etapa</button></div>
        </form>
      </div>
    }

    @if (projectDeleteModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <div class="modal-panel card w-full max-w-md space-y-5 p-6">
          <div class="grid h-14 w-14 place-items-center rounded-2xl bg-red-50 text-red-600"><lucide-icon name="Trash2" size="24"></lucide-icon></div>
          <div><h3 class="text-xl font-extrabold">Arquivar projeto</h3><p class="mt-2 text-sm leading-6 text-slate-500">O projeto será removido da lista sem exclusão física.</p></div>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeProjectDeleteModal()">Cancelar</button><button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="deleteProject()">Arquivar</button></div>
        </div>
      </div>
    }
  `
})
export class ProjectsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  readonly baseUrl = 'http://localhost:8080/api/tenant/projects';
  readonly mode = signal<ViewMode>('projects');
  readonly projects = signal<ProjectSummary[]>([]);
  readonly templates = signal<ProjectTemplate[]>([]);
  readonly templateOptions = signal<ProjectTemplate[]>([]);
  readonly clients = signal<ClientOption[]>([]);
  readonly stats = signal<ProjectStats | null>(null);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly templateModalOpen = signal(false);
  readonly editingTemplate = signal<ProjectTemplate | null>(null);
  readonly selectedTemplate = signal<ProjectTemplate | null>(null);
  readonly templateStages = signal<StageTemplate[]>([]);
  readonly templateStageModalOpen = signal(false);
  readonly editingTemplateStage = signal<StageTemplate | null>(null);
  readonly draggedTemplateStage = signal<StageTemplate | null>(null);
  readonly projectDeleteModalOpen = signal(false);
  readonly selectedProject = signal<ProjectSummary | null>(null);
  readonly filterForm = this.fb.nonNullable.group({ clientId: [''], status: [''], templateId: [''], responsible: [''], from: [''], to: [''] });
  readonly templateForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], active: [true] });
  readonly stageTemplateForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], color: ['#0f766e'], icon: ['ListChecks'], weightPercentage: [0], active: [true], checklistText: [''] });
  readonly statusOptions = [
    { label: 'Todos', value: '' }, { label: 'Planejamento', value: 'PLANNING' }, { label: 'Em andamento', value: 'IN_PROGRESS' },
    { label: 'Pausado', value: 'ON_HOLD' }, { label: 'Concluído', value: 'COMPLETED' }, { label: 'Cancelado', value: 'CANCELLED' }
  ];
  readonly iconOptions = [
    { label: 'Checklist', value: 'ListChecks' }, { label: 'Régua', value: 'Ruler' }, { label: 'Casa', value: 'Home' },
    { label: 'Arquivo', value: 'FileText' }, { label: 'Calendário', value: 'CalendarDays' }, { label: 'Entrega', value: 'PackageCheck' }
  ];

  ngOnInit() { this.loadAll(); }
  loadAll() { this.loadProjects(); this.loadTemplates(); this.loadTemplateOptions(); this.loadClients(); this.loadStats(); }
  switchMode(mode: ViewMode) { this.mode.set(mode); mode === 'projects' ? this.loadProjects() : this.loadTemplates(); }
  search() { this.page.set(0); this.loadProjects(); }
  loadProjects() {
    const params = this.paramsFrom(this.filterForm.getRawValue()); params.set('page', String(this.page())); params.set('size', '20'); params.set('sort', 'createdAt,desc');
    this.http.get<ApiResponse<Page<ProjectSummary>>>(`${this.baseUrl}?${params}`).subscribe((r) => { this.projects.set(r.data.content); this.totalElements.set(r.data.totalElements); this.totalPages.set(r.data.totalPages); });
  }
  loadTemplates() { this.http.get<ApiResponse<Page<ProjectTemplate>>>(`${this.baseUrl}/templates?page=0&size=50&sort=name,asc`).subscribe((r) => this.templates.set(r.data.content)); }
  loadTemplateOptions() { this.http.get<ApiResponse<ProjectTemplate[]>>(`${this.baseUrl}/templates/options`).subscribe((r) => this.templateOptions.set(r.data)); }
  loadClients() { this.http.get<ApiResponse<Page<ClientOption>>>('http://localhost:8080/api/tenant/clients?page=0&size=200&sort=createdAt,desc').subscribe((r) => this.clients.set(r.data.content)); }
  loadStats() { this.http.get<ApiResponse<ProjectStats>>(`${this.baseUrl}/stats`).subscribe((r) => this.stats.set(r.data)); }
  openTemplateModal(template?: ProjectTemplate) { this.editingTemplate.set(template || null); this.templateForm.reset({ name: template?.name || '', description: template?.description || '', active: template?.active ?? true }); this.templateModalOpen.set(true); }
  closeTemplateModal() { this.templateModalOpen.set(false); }
  saveTemplate() {
    if (this.templateForm.invalid) { this.templateForm.markAllAsTouched(); this.toast.validation('Informe o nome do modelo.'); return; }
    const template = this.editingTemplate();
    const request = template ? this.http.put<ApiResponse<ProjectTemplate>>(`${this.baseUrl}/templates/${template.id}`, this.templateForm.getRawValue()) : this.http.post<ApiResponse<ProjectTemplate>>(`${this.baseUrl}/templates`, this.templateForm.getRawValue());
    request.subscribe({ next: (response) => { this.closeTemplateModal(); this.loadTemplates(); this.loadTemplateOptions(); this.toast.success(template ? 'Modelo atualizado' : 'Modelo criado'); if (!template) this.selectTemplate(response.data); }, error: () => this.toast.error('Não foi possível salvar o modelo') });
  }
  duplicateTemplate(template: ProjectTemplate) { this.http.post<ApiResponse<ProjectTemplate>>(`${this.baseUrl}/templates/${template.id}/duplicate`, {}).subscribe({ next: (r) => { this.loadTemplates(); this.toast.success('Modelo duplicado'); this.selectTemplate(r.data); }, error: () => this.toast.error('Não foi possível duplicar o modelo') }); }
  deleteTemplate(template: ProjectTemplate) { this.http.patch(`${this.baseUrl}/templates/${template.id}/delete`, {}).subscribe({ next: () => { this.loadTemplates(); this.loadTemplateOptions(); this.toast.success('Modelo removido'); if (this.selectedTemplate()?.id === template.id) this.selectedTemplate.set(null); }, error: () => this.toast.error('Não foi possível remover o modelo') }); }
  selectTemplate(template: ProjectTemplate) { this.selectedTemplate.set(template); this.loadTemplateStages(); }
  loadTemplateStages() { const template = this.selectedTemplate(); if (!template) return; this.http.get<ApiResponse<StageTemplate[]>>(`${this.baseUrl}/templates/${template.id}/stages`).subscribe((r) => this.templateStages.set(r.data)); }
  openTemplateStageModal(stage?: StageTemplate) {
    this.editingTemplateStage.set(stage || null);
    this.stageTemplateForm.reset({ name: stage?.name || '', description: stage?.description || '', order: stage?.order || this.templateStages().length + 1, color: stage?.color || '#0f766e', icon: stage?.icon || 'ListChecks', weightPercentage: stage?.weightPercentage || 0, active: stage?.active ?? true, checklistText: this.checklistToText(stage?.checklist || []) });
    this.templateStageModalOpen.set(true);
  }
  closeTemplateStageModal() { this.templateStageModalOpen.set(false); }
  saveTemplateStage() {
    const template = this.selectedTemplate();
    if (!template) return;
    if (this.stageTemplateForm.invalid) { this.stageTemplateForm.markAllAsTouched(); this.toast.validation('Informe o nome da etapa.'); return; }
    const stage = this.editingTemplateStage();
    const payload = this.stageTemplatePayload();
    const request = stage ? this.http.put(`${this.baseUrl}/templates/${template.id}/stages/${stage.id}`, payload) : this.http.post(`${this.baseUrl}/templates/${template.id}/stages`, payload);
    request.subscribe({ next: () => { this.closeTemplateStageModal(); this.loadTemplateStages(); this.loadTemplates(); this.toast.success(stage ? 'Etapa atualizada' : 'Etapa criada'); }, error: () => this.toast.error('Não foi possível salvar a etapa') });
  }
  duplicateTemplateStage(stage: StageTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.post(`${this.baseUrl}/templates/${template.id}/stages/${stage.id}/duplicate`, {}).subscribe({ next: () => { this.loadTemplateStages(); this.loadTemplates(); this.toast.success('Etapa duplicada'); }, error: () => this.toast.error('Não foi possível duplicar a etapa') }); }
  deleteTemplateStage(stage: StageTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.patch(`${this.baseUrl}/templates/${template.id}/stages/${stage.id}/delete`, {}).subscribe({ next: () => { this.loadTemplateStages(); this.loadTemplates(); this.toast.success('Etapa removida'); }, error: () => this.toast.error('Não foi possível remover a etapa') }); }
  startTemplateDrag(stage: StageTemplate) { this.draggedTemplateStage.set(stage); }
  allowDrop(event: DragEvent) { event.preventDefault(); }
  dropTemplateStage(target: StageTemplate) {
    const dragged = this.draggedTemplateStage(); const template = this.selectedTemplate();
    if (!dragged || !template || dragged.id === target.id) return;
    const ordered = [...this.templateStages()];
    const from = ordered.findIndex((stage) => stage.id === dragged.id); const to = ordered.findIndex((stage) => stage.id === target.id);
    ordered.splice(to, 0, ordered.splice(from, 1)[0]);
    const items = ordered.map((stage, index) => ({ id: stage.id, order: index + 1 }));
    this.http.patch<ApiResponse<StageTemplate[]>>(`${this.baseUrl}/templates/${template.id}/stages/reorder`, { items }).subscribe((r) => this.templateStages.set(r.data));
  }
  openProjectDeleteModal(project: ProjectSummary) { this.selectedProject.set(project); this.projectDeleteModalOpen.set(true); }
  closeProjectDeleteModal() { this.projectDeleteModalOpen.set(false); this.selectedProject.set(null); }
  deleteProject() {
    const project = this.selectedProject(); if (!project) return;
    this.http.delete(`${this.baseUrl}/${project.id}`).subscribe({ next: () => { this.closeProjectDeleteModal(); this.loadProjects(); this.loadStats(); this.toast.success('Projeto arquivado'); }, error: () => this.toast.error('Não foi possível arquivar o projeto') });
  }
  previousPage() { if (this.page() > 0) { this.page.update((value) => value - 1); this.loadProjects(); } }
  nextPage() { if (this.page() + 1 < this.totalPages()) { this.page.update((value) => value + 1); this.loadProjects(); } }
  clientOptions() { return [{ label: 'Todos', value: '' }, ...this.clients().map((c) => ({ label: c.displayName, value: c.id }))]; }
  templateFilterOptions() { return [{ label: 'Todos', value: '' }, ...this.templateOptions().map((t) => ({ label: t.name, value: t.id }))]; }
  statusLabel(status: ProjectStatus) { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' }[status]); }
  statusClass(status: ProjectStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'ON_HOLD') return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
  checklistToText(items: ChecklistItem[]) { return items.map((item) => item.title).join('\n'); }
  stageTemplatePayload() {
    const raw = this.stageTemplateForm.getRawValue();
    return { ...raw, checklist: this.textToChecklist(raw.checklistText) };
  }
  textToChecklist(text: string) { return (text || '').split('\n').map((line) => line.trim()).filter(Boolean).map((title, index) => ({ title, order: index + 1, completed: false })); }
  private paramsFrom(values: Record<string, unknown>) { const p = new URLSearchParams(); Object.entries(values).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') p.set(k, String(v)); }); return p; }
}

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, RouterLink, ArqlySelectComponent],
  template: `
    @if (project()) {
      <section class="space-y-5">
        <a class="inline-flex items-center gap-2 text-sm font-bold text-arqly-700" routerLink="/app/projects"><lucide-icon name="ArrowLeft" size="16"></lucide-icon>Voltar</a>
        <div class="card p-6">
          <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div><p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">{{ project()!.code }}</p><h2 class="mt-2 text-3xl font-extrabold">{{ project()!.name }}</h2><p class="mt-2 text-slate-500">{{ project()!.clientName }} · {{ project()!.proposalNumber }}</p></div>
            <div class="grid gap-2 text-sm md:text-right"><span class="rounded-full bg-arqly-50 px-3 py-1 font-bold text-arqly-700">{{ statusLabel(project()!.status) }}</span><strong>R$ {{ project()!.contractedValue || 0 | number:'1.2-2' }}</strong><span class="text-slate-500">{{ project()!.responsibleArchitect || 'Sem responsável' }}</span></div>
          </div>
          <div class="mt-5">
            <div class="flex items-center justify-between text-xs font-bold text-slate-500"><span>Progresso geral</span><span>{{ project()!.progressPercentage || 0 | number:'1.0-0' }}%</span></div>
            <div class="mt-2 h-3 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="project()!.progressPercentage || 0"></div></div>
          </div>
        </div>
        <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-8">
          @for (tab of tabs; track tab.value) {
            <button class="rounded-xl px-3 py-3 text-sm font-bold transition" type="button" [disabled]="tab.disabled" [class.bg-white]="activeTab() === tab.value" [class.text-arqly-700]="activeTab() === tab.value" [class.text-slate-400]="tab.disabled" (click)="!tab.disabled && activeTab.set(tab.value)">{{ tab.label }}</button>
          }
        </div>
        @if (activeTab() === 'summary') {
          <section class="grid gap-4 md:grid-cols-3">
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Cliente</p><strong class="mt-2 block">{{ project()!.clientName }}</strong></div>
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Proposta origem</p><strong class="mt-2 block">{{ project()!.proposalNumber }}</strong></div>
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Modelo</p><strong class="mt-2 block">{{ project()!.templateName || 'Sem modelo' }}</strong></div>
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Status</p><strong class="mt-2 block">{{ statusLabel(project()!.status) }}</strong></div>
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Datas</p><strong class="mt-2 block">{{ project()!.startDate ? (project()!.startDate | date:'dd/MM/yyyy') : '-' }} → {{ project()!.expectedEndDate ? (project()!.expectedEndDate | date:'dd/MM/yyyy') : '-' }}</strong></div>
            <div class="card p-5"><p class="text-sm font-bold text-slate-500">Responsável</p><strong class="mt-2 block">{{ project()!.responsibleArchitect || '-' }}</strong></div>
          </section>
        }
        @if (activeTab() === 'stages') {
          <div class="card overflow-hidden">
            <div class="flex flex-col gap-3 border-b border-slate-200 p-5 md:flex-row md:items-center md:justify-between">
              <div><h3 class="text-xl font-extrabold">Etapas do projeto</h3><p class="mt-1 text-sm text-slate-500">Timeline vertical da execução. Arraste para reordenar.</p></div>
              <button class="btn-primary" type="button" (click)="openStageModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Adicionar etapa</button>
            </div>
            <div class="relative p-5">
              <div class="absolute bottom-8 left-10 top-8 w-px bg-slate-200"></div>
              @for (stage of stages(); track stage.id) {
                <div class="relative grid gap-4 py-4 pl-14 md:grid-cols-[1fr_auto]" draggable="true" (dragstart)="startStageDrag(stage)" (dragover)="allowDrop($event)" (drop)="dropStage(stage)">
                  <div class="absolute left-0 top-5 grid h-10 w-10 place-items-center rounded-2xl text-white shadow-sm" [style.background]="stage.color || '#0f766e'"><lucide-icon [name]="stage.icon || 'ListChecks'" size="18"></lucide-icon></div>
                  <div>
                    <div class="flex flex-wrap items-center gap-2">
                      <strong>{{ stage.order }}. {{ stage.name }}</strong>
                      <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="stageStatusClass(stage.status)">{{ stageStatusLabel(stage.status) }}</span>
                      <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-500">{{ stage.completionPercentage || 0 }}%</span>
                    </div>
                    <p class="mt-1 text-sm text-slate-500">{{ stage.description || 'Sem descrição' }}</p>
                    <div class="mt-3 h-2 max-w-xl overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="stage.completionPercentage || 0"></div></div>
                    <div class="mt-3 flex flex-wrap gap-3 text-xs font-bold text-slate-400">
                      <span>Previsto: {{ stage.plannedStart ? (stage.plannedStart | date:'dd/MM/yyyy') : '-' }} → {{ stage.plannedEnd ? (stage.plannedEnd | date:'dd/MM/yyyy') : '-' }}</span>
                      <span>Responsável: {{ stage.responsible || 'A definir' }}</span>
                    </div>
                    @if (stage.checklist.length) {
                      <div class="mt-3 grid gap-2 sm:grid-cols-2">
                        @for (item of stage.checklist; track item.id || item.title) {
                          <div class="rounded-2xl border border-slate-200 bg-slate-50/70 px-3 py-2 text-xs font-bold text-slate-600">{{ item.completed ? '✓' : '○' }} {{ item.title }}</div>
                        }
                      </div>
                    }
                  </div>
                  <div class="flex items-start justify-end gap-2">
                    <button class="btn-secondary px-3 py-2" type="button" (click)="openStageModal(stage)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                    <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateStage(stage)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                    <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteStage(stage)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                  </div>
                </div>
              } @empty {
                <p class="py-12 text-center text-sm text-slate-500">Nenhuma etapa cadastrada para este projeto.</p>
              }
            </div>
          </div>
        }
        @if (activeTab() === 'services') {
          <div class="card overflow-x-auto"><table class="w-full min-w-[760px] text-left text-sm"><thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-6 py-4">Serviço</th><th class="px-6 py-4">Qtd.</th><th class="px-6 py-4">Unidade</th><th class="px-6 py-4">Valor contratado</th></tr></thead><tbody>@for (service of project()!.services; track service.id) {<tr class="border-t border-slate-100"><td class="px-6 py-4"><p class="font-bold">{{ service.name }}</p><p class="text-xs text-slate-500">{{ service.description || '-' }}</p></td><td class="px-6 py-4">{{ service.quantity }}</td><td class="px-6 py-4">{{ service.unit }}</td><td class="px-6 py-4">R$ {{ service.contractedValue || 0 | number:'1.2-2' }}</td></tr>}</tbody></table></div>
        }
        @if (activeTab() === 'info') {
          <form class="card space-y-4 p-6" [formGroup]="form" (ngSubmit)="save()">
            <div class="grid gap-4 md:grid-cols-2"><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome</span><input class="field" formControlName="name"></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Status</span><app-arqly-select formControlName="status" [options]="statusOptions" panelMode="fixed" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Responsável</span><input class="field" formControlName="responsibleArchitect"></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Previsão</span><input class="field" type="date" formControlName="expectedEndDate"></label></div>
            <label class="space-y-1 block"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-28" formControlName="description"></textarea></label>
            <label class="space-y-1 block"><span class="text-xs font-bold text-slate-500">Dados internos</span><textarea class="field min-h-28" formControlName="internalNotes"></textarea></label>
            <div class="flex justify-end"><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button></div>
          </form>
        }
      </section>
    }
    @if (stageModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-3xl space-y-5 overflow-y-auto p-6" [formGroup]="stageForm" (ngSubmit)="saveStage()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingStage() ? 'Editar etapa' : 'Nova etapa' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closeStageModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name"></label>
            <app-arqly-select formControlName="status" placeholder="Status" [options]="stageStatusOptions" panelMode="fixed" />
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Percentual</span><input class="field" type="number" min="0" max="100" formControlName="completionPercentage"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Início previsto</span><input class="field" type="date" formControlName="plannedStart"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Término previsto</span><input class="field" type="date" formControlName="plannedEnd"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Responsável</span><input class="field" formControlName="responsible"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cor</span><input class="field h-12" type="color" formControlName="color"></label>
          </div>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Observações</span><textarea class="field min-h-24" formControlName="notes"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Checklist</span><textarea class="field min-h-32" formControlName="checklistText" placeholder="Um item por linha"></textarea></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeStageModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar etapa</button></div>
        </form>
      </div>
    }
  `
})
export class ProjectDetailComponent implements OnInit {
  private readonly http = inject(HttpClient); private readonly route = inject(ActivatedRoute); private readonly fb = inject(FormBuilder); private readonly toast = inject(ToastService);
  readonly baseUrl = 'http://localhost:8080/api/tenant/projects';
  readonly project = signal<ProjectDetail | null>(null); readonly stages = signal<ProjectStage[]>([]); readonly activeTab = signal('summary');
  readonly stageModalOpen = signal(false); readonly editingStage = signal<ProjectStage | null>(null); readonly draggedStage = signal<ProjectStage | null>(null);
  readonly tabs = [{ label: 'Resumo', value: 'summary' }, { label: 'Etapas', value: 'stages' }, { label: 'Serviços', value: 'services' }, { label: 'Informações', value: 'info' }, { label: 'Timeline', value: 'timeline', disabled: true }, { label: 'Arquivos', value: 'files', disabled: true }, { label: 'Contratos', value: 'contracts', disabled: true }, { label: 'Financeiro', value: 'finance', disabled: true }];
  readonly statusOptions = [{ label: 'Planejamento', value: 'PLANNING' }, { label: 'Em andamento', value: 'IN_PROGRESS' }, { label: 'Pausado', value: 'ON_HOLD' }, { label: 'Concluído', value: 'COMPLETED' }, { label: 'Cancelado', value: 'CANCELLED' }];
  readonly stageStatusOptions = [{ label: 'Não iniciada', value: 'NOT_STARTED' }, { label: 'Em andamento', value: 'IN_PROGRESS' }, { label: 'Aguardando cliente', value: 'WAITING_CLIENT' }, { label: 'Aguardando aprovação', value: 'WAITING_APPROVAL' }, { label: 'Pausada', value: 'ON_HOLD' }, { label: 'Concluída', value: 'COMPLETED' }, { label: 'Cancelada', value: 'CANCELLED' }];
  readonly form = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], status: ['PLANNING'], responsibleArchitect: [''], expectedEndDate: [''], internalNotes: [''] });
  readonly stageForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], color: ['#0f766e'], icon: ['ListChecks'], status: ['NOT_STARTED'], plannedStart: [''], plannedEnd: [''], actualStart: [''], actualEnd: [''], completionPercentage: [0], weightPercentage: [0], responsible: [''], notes: [''], checklistText: [''] });
  ngOnInit() { this.load(); }
  projectId() { return this.route.snapshot.paramMap.get('id')!; }
  load() { this.http.get<ApiResponse<ProjectDetail>>(`${this.baseUrl}/${this.projectId()}`).subscribe((r) => { this.project.set(r.data); this.stages.set(r.data.stages || []); this.form.patchValue({ name: r.data.name, description: r.data.description || '', status: r.data.status, responsibleArchitect: r.data.responsibleArchitect || '', expectedEndDate: r.data.expectedEndDate || '', internalNotes: r.data.internalNotes || '' }); }); }
  save() { const p = this.project(); if (!p) return; this.http.put<ApiResponse<ProjectDetail>>(`${this.baseUrl}/${p.id}`, { ...this.form.getRawValue(), templateId: p.templateId, startDate: p.startDate, completedAt: p.completedAt }).subscribe({ next: () => { this.toast.success('Projeto atualizado'); this.load(); }, error: () => this.toast.error('Não foi possível salvar o projeto') }); }
  openStageModal(stage?: ProjectStage) { this.editingStage.set(stage || null); this.stageForm.reset({ name: stage?.name || '', description: stage?.description || '', order: stage?.order || this.stages().length + 1, color: stage?.color || '#0f766e', icon: stage?.icon || 'ListChecks', status: stage?.status || 'NOT_STARTED', plannedStart: stage?.plannedStart || '', plannedEnd: stage?.plannedEnd || '', actualStart: stage?.actualStart || '', actualEnd: stage?.actualEnd || '', completionPercentage: stage?.completionPercentage || 0, weightPercentage: stage?.weightPercentage || 0, responsible: stage?.responsible || '', notes: stage?.notes || '', checklistText: this.checklistToText(stage?.checklist || []) }); this.stageModalOpen.set(true); }
  closeStageModal() { this.stageModalOpen.set(false); }
  saveStage() {
    if (this.stageForm.invalid) { this.stageForm.markAllAsTouched(); this.toast.validation('Informe o nome da etapa.'); return; }
    const stage = this.editingStage(); const payload = this.stagePayload();
    const request = stage ? this.http.put(`${this.baseUrl}/${this.projectId()}/stages/${stage.id}`, payload) : this.http.post(`${this.baseUrl}/${this.projectId()}/stages`, payload);
    request.subscribe({ next: () => { this.closeStageModal(); this.toast.success(stage ? 'Etapa atualizada' : 'Etapa criada'); this.load(); }, error: () => this.toast.error('Não foi possível salvar a etapa') });
  }
  duplicateStage(stage: ProjectStage) { this.http.post(`${this.baseUrl}/${this.projectId()}/stages/${stage.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Etapa duplicada'); this.load(); }, error: () => this.toast.error('Não foi possível duplicar a etapa') }); }
  deleteStage(stage: ProjectStage) { this.http.patch(`${this.baseUrl}/${this.projectId()}/stages/${stage.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Etapa removida'); this.load(); }, error: () => this.toast.error('Não foi possível remover a etapa') }); }
  startStageDrag(stage: ProjectStage) { this.draggedStage.set(stage); }
  allowDrop(event: DragEvent) { event.preventDefault(); }
  dropStage(target: ProjectStage) { const dragged = this.draggedStage(); if (!dragged || dragged.id === target.id) return; const ordered = [...this.stages()]; const from = ordered.findIndex((stage) => stage.id === dragged.id); const to = ordered.findIndex((stage) => stage.id === target.id); ordered.splice(to, 0, ordered.splice(from, 1)[0]); const items = ordered.map((stage, index) => ({ id: stage.id, order: index + 1 })); this.http.patch<ApiResponse<ProjectStage[]>>(`${this.baseUrl}/${this.projectId()}/stages/reorder`, { items }).subscribe((r) => { this.stages.set(r.data); this.load(); }); }
  stagePayload() { const raw = this.stageForm.getRawValue(); return { ...raw, checklist: this.textToChecklist(raw.checklistText) }; }
  checklistToText(items: ChecklistItem[]) { return items.map((item) => item.title).join('\n'); }
  textToChecklist(text: string) { return (text || '').split('\n').map((line) => line.trim()).filter(Boolean).map((title, index) => ({ title, order: index + 1, completed: false })); }
  statusLabel(status: ProjectStatus) { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' }[status]); }
  stageStatusLabel(status: StageStatus) { return ({ NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING_CLIENT: 'Aguardando cliente', WAITING_APPROVAL: 'Aguardando aprovação', ON_HOLD: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }[status]); }
  stageStatusClass(status: StageStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700'; if (status === 'ON_HOLD' || status.startsWith('WAITING')) return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
}
