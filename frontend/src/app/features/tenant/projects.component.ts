import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { ContextDocumentsComponent } from '../../shared/components/context-documents.component';
import { FileExplorerComponent } from '../../shared/files/file-explorer.component';
import { RecentFilesComponent } from '../../shared/files/recent-files.component';

type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'WAITING_APPROVAL' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type ProgressMode = 'MANUAL' | 'CHECKLIST' | 'DATES' | 'HYBRID';
type OriginType = 'MANUAL' | 'BRIEFING' | 'PROPOSAL';
type ViewMode = 'projects' | 'templates';
type ProjectCreateMode = 'PROPOSAL' | 'MANUAL';
type ProjectCreateStep = 'client' | 'origin' | 'info' | 'summary';

interface Page<T> { content: T[]; number: number; totalElements: number; totalPages: number; }
interface ClientOption { id: string; displayName: string; }
interface TenantUserOption { id: string; name: string; email: string; tenantAdmin: boolean; }
interface ProposalOption { id: string; number: string; title: string; clientId: string; clientName: string; status: string; projectCreated: boolean; }
interface ChecklistItem { id?: string; title: string; order: number; completed?: boolean; completedAt?: string | null; }
interface StageTemplate {
  id: string; phaseTemplateId: string; name: string; description?: string; order: number; weightPercentage: number;
  progressCalculationMode: ProgressMode; active: boolean; checklist: ChecklistItem[]; createdAt: string; updatedAt: string;
}
interface PhaseTemplate {
  id: string; name: string; description?: string; order: number; color?: string; icon?: string; active: boolean;
  stageCount: number; stages: StageTemplate[]; createdAt: string; updatedAt: string;
}
interface ProjectTemplate {
  id: string; name: string; description?: string; active: boolean; phaseCount: number; stageCount: number;
  projectsUsing: number; phases: PhaseTemplate[]; createdAt: string; updatedAt: string;
}
interface ProjectSummary {
  id: string; code: string; name: string; clientId: string; clientName: string; proposalId?: string | null; proposalNumber?: string | null; originType: OriginType;
  templateId?: string | null; templateName?: string | null; responsibleUserId?: string | null; responsibleName?: string | null;
  projectManagerId?: string | null; projectManagerName?: string | null; responsibleArchitect?: string | null; status: ProjectStatus;
  expectedEndDate?: string | null; contractedValue: number; progressPercentage: number; createdAt: string; updatedAt: string;
}
interface ProjectStats {
  active: number; completed: number; paused: number; delayedStages: number;
}
interface ProjectStage {
  id: string; projectPhaseId: string; templateId?: string | null; dependsOnStageId?: string | null; name: string;
  description?: string; order: number; status: StageStatus; plannedStart?: string | null; plannedEnd?: string | null;
  actualStart?: string | null; actualEnd?: string | null; completionPercentage: number; weightPercentage: number;
  progressCalculationMode: ProgressMode; responsible?: string | null; notes?: string | null; fileCount: number;
  responsibleUserId?: string | null; responsibleName?: string | null;
  timelineEventCount: number; checklist: ChecklistItem[]; createdAt: string; updatedAt: string;
}
interface ProjectPhase {
  id: string; phaseTemplateId?: string | null; name: string; description?: string; order: number; color?: string;
  icon?: string; status: StageStatus; completionPercentage: number; notes?: string | null; stages: ProjectStage[];
  createdAt: string; updatedAt: string;
}
interface ProjectDetail {
  id: string; code: string; name: string; description?: string; clientName: string; proposalNumber?: string | null; originType: OriginType;
  templateId?: string | null; templateName?: string | null; responsibleUserId?: string | null; responsibleName?: string | null;
  projectManagerId?: string | null; projectManagerName?: string | null; responsibleArchitect?: string | null; status: ProjectStatus;
  startDate?: string | null; expectedEndDate?: string | null; completedAt?: string | null; contractedValue: number;
  progressPercentage: number; internalNotes?: string | null; services: any[]; phases: ProjectPhase[];
}

const PHASE_ICON_OPTIONS = [
  { label: 'Fases', value: 'Layers3' },
  { label: 'Checklist', value: 'ClipboardList' },
  { label: 'Projeto', value: 'DraftingCompass' },
  { label: 'Edificação', value: 'Building2' },
  { label: 'Aprovação', value: 'FileCheck2' },
  { label: 'Execução', value: 'Hammer' },
  { label: 'Entrega', value: 'Flag' }
];

function normalizedPhaseIcon(icon?: string | null) {
  const aliases: Record<string, string> = {
    'layers-3': 'Layers3',
    'clipboard-list': 'ClipboardList',
    'pencil-ruler': 'DraftingCompass',
    'drafting-compass': 'DraftingCompass',
    'building-2': 'Building2',
    'file-check-2': 'FileCheck2',
    'stamp': 'FileCheck2',
    'package-check': 'Flag',
    'hammer': 'Hammer',
    'flag': 'Flag'
  };
  const candidate = aliases[(icon || '').toLowerCase()] || icon;
  return PHASE_ICON_OPTIONS.some((option) => option.value === candidate) ? candidate! : 'Layers3';
}
interface ProjectApproval {
  id: string; projectId: string; projectName: string; stageId?: string | null; stageName?: string | null;
  documentId?: string | null; documentTitle?: string | null; status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';
  description: string; deadline?: string | null; approvedAt?: string | null; approvedBy?: string | null;
  createdByName: string; clientComment?: string | null; createdAt: string; updatedAt: string;
}

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, RouterLink, ArqlySelectComponent, ArqlyDatePickerComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Execução</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Projetos</h2>
          <p class="mt-2 text-slate-500">Acompanhe projetos, modelos, fases e etapas de execução.</p>
        </div>
        @if (mode() === 'templates') {
          <button class="btn-primary" type="button" (click)="openTemplateModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Novo modelo</button>
        } @else {
          <button class="btn-primary" type="button" (click)="openProjectCreateModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Novo projeto</button>
        }
      </div>

      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Projetos ativos</p><strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.active || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Concluídos</p><strong class="mt-2 block text-3xl">{{ stats()?.completed || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Pausados</p><strong class="mt-2 block text-3xl text-amber-600">{{ stats()?.paused || 0 }}</strong></div>
        <div class="card p-5"><p class="text-sm font-bold text-slate-500">Etapas atrasadas</p><strong class="mt-2 block text-3xl text-red-600">{{ stats()?.delayedStages || 0 }}</strong></div>
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
            <app-arqly-date-picker formControlName="from" placeholder="Data inicial" />
            <app-arqly-date-picker formControlName="to" placeholder="Data final" />
            <button class="btn-secondary h-12 justify-center px-4" type="submit"><lucide-icon name="Search" size="18"></lucide-icon>Pesquisar</button>
          </form>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[1180px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th class="px-6 py-4">Código</th><th class="px-6 py-4">Nome</th><th class="px-6 py-4">Cliente</th><th class="px-6 py-4">Origem</th><th class="px-6 py-4">Responsável</th><th class="px-6 py-4">Situação</th><th class="px-6 py-4">Progresso</th><th class="px-6 py-4">Previsão</th><th class="px-6 py-4">Valor</th><th class="px-6 py-4 text-right">Ações</th></tr>
              </thead>
              <tbody>
                @for (project of projects(); track project.id) {
                  <tr class="border-t border-slate-100">
                    <td class="px-6 py-4 font-extrabold">{{ project.code }}</td>
                    <td class="px-6 py-4"><p class="font-bold">{{ project.name }}</p><p class="text-xs text-slate-500">{{ project.templateName || 'Sem modelo' }}</p></td>
                    <td class="px-6 py-4">{{ project.clientName }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="originClass(project.originType)">{{ originLabel(project.originType) }}</span><p class="mt-1 text-xs text-slate-400">{{ project.proposalNumber || 'Sem proposta' }}</p></td>
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
                  <tr><td colspan="10" class="px-6 py-12 text-center text-slate-500">Nenhum projeto encontrado.</td></tr>
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
            <table class="w-full min-w-[920px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th class="px-6 py-4">Nome</th><th class="px-6 py-4">Fases</th><th class="px-6 py-4">Etapas</th><th class="px-6 py-4">Projetos usando</th><th class="px-6 py-4">Status</th><th class="px-6 py-4 text-right">Ações</th></tr>
              </thead>
              <tbody>
                @for (template of templates(); track template.id) {
                  <tr class="border-t border-slate-100" [class.bg-arqly-50]="selectedTemplate()?.id === template.id">
                    <td class="px-6 py-4"><p class="font-bold">{{ template.name }}</p><p class="text-xs text-slate-500">{{ template.description || '-' }}</p></td>
                    <td class="px-6 py-4">{{ template.phaseCount || 0 }}</td>
                    <td class="px-6 py-4">{{ template.stageCount || 0 }}</td>
                    <td class="px-6 py-4">{{ template.projectsUsing || 0 }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class.bg-arqly-50]="template.active" [class.text-arqly-700]="template.active" [class.bg-slate-100]="!template.active" [class.text-slate-500]="!template.active">{{ template.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td class="px-6 py-4">
                      <div class="flex justify-end gap-2">
                        <button class="btn-secondary px-3 py-2" type="button" (click)="selectTemplate(template)"><lucide-icon name="ListChecks" size="16"></lucide-icon>Fases</button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="openTemplateModal(template)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateTemplate(template)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteTemplate(template)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="px-6 py-12 text-center text-slate-500">Nenhum modelo cadastrado.</td></tr>
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
            <button class="btn-primary" type="button" (click)="openPhaseModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Adicionar fase</button>
          </div>
          <div class="space-y-3 p-4">
            @for (phase of templatePhases(); track phase.id) {
              <div class="rounded-3xl border border-slate-200 bg-white" draggable="true" (dragstart)="startPhaseTemplateDrag(phase)" (dragover)="allowDrop($event)" (drop)="dropPhaseTemplate(phase)">
                <button class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left" type="button" (click)="toggleTemplatePhase(phase.id)">
                  <span class="flex min-w-0 items-center gap-3">
                    <span class="grid h-10 w-10 place-items-center rounded-2xl text-white" [style.background]="phase.color || '#0f766e'"><lucide-icon [name]="phaseIcon(phase.icon)" size="18"></lucide-icon></span>
                    <span class="min-w-0"><strong class="block truncate">{{ phase.order }}. {{ phase.name }}</strong><span class="text-xs font-bold text-slate-400">{{ phase.stageCount }} etapa(s)</span></span>
                  </span>
                  <span class="flex items-center gap-2">
                    <button class="btn-secondary px-3 py-2" type="button" (click)="openTemplateStageModal(phase); $event.stopPropagation()"><lucide-icon name="Plus" size="16"></lucide-icon>Etapa</button>
                    <button class="btn-secondary px-3 py-2" type="button" (click)="openPhaseModal(phase); $event.stopPropagation()"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                    <button class="btn-secondary px-3 py-2" type="button" (click)="duplicatePhaseTemplate(phase); $event.stopPropagation()"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                    <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deletePhaseTemplate(phase); $event.stopPropagation()"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                    <lucide-icon [name]="expandedTemplatePhases().has(phase.id) ? 'ChevronUp' : 'ChevronDown'" size="18"></lucide-icon>
                  </span>
                </button>
                @if (expandedTemplatePhases().has(phase.id)) {
                  <div class="space-y-3 border-t border-slate-100 p-4">
                    @for (stage of phase.stages; track stage.id) {
                      <div class="grid gap-4 rounded-2xl bg-slate-50/70 p-4 md:grid-cols-[1fr_auto]" draggable="true" (dragstart)="startTemplateStageDrag(stage)" (dragover)="allowDrop($event)" (drop)="dropTemplateStage(phase, stage)">
                        <div>
                          <div class="flex flex-wrap items-center gap-2"><strong>{{ stage.order }}. {{ stage.name }}</strong><span class="rounded-full bg-white px-2 py-1 text-xs font-bold text-slate-500">{{ stage.weightPercentage || 0 }}%</span>@if (!stage.active) { <span class="rounded-full bg-white px-2 py-1 text-xs font-bold text-slate-500">Inativa</span> }</div>
                          <p class="mt-1 text-sm text-slate-500">{{ stage.description || 'Sem descrição' }}</p>
                          <p class="mt-2 text-xs font-bold text-slate-400">{{ stage.checklist.length }} item(ns) de checklist</p>
                        </div>
                        <div class="flex items-center justify-end gap-2">
                          <button class="btn-secondary px-3 py-2" type="button" (click)="openTemplateStageModal(phase, stage)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                          <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateTemplateStage(phase, stage)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                          <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteTemplateStage(phase, stage)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                        </div>
                      </div>
                    } @empty {
                      <p class="rounded-2xl bg-slate-50/70 py-8 text-center text-sm text-slate-500">Nenhuma etapa nesta fase.</p>
                    }
                  </div>
                }
              </div>
            } @empty {
              <p class="py-10 text-center text-sm text-slate-500">Nenhuma fase cadastrada para este modelo.</p>
            }
          </div>
        </div>
      }
    </section>

    @if (templateModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-xl space-y-5 p-6" [formGroup]="templateForm" (ngSubmit)="saveTemplate()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingTemplate() ? 'Editar modelo' : 'Novo modelo' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closeTemplateModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Projeto Arquitetônico Residencial"></label>
          <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-28" formControlName="description" placeholder="Descreva quando este modelo deve ser utilizado"></textarea></label>
          <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3"><span><span class="block text-sm font-bold text-slate-800">Modelo ativo</span><span class="mt-1 block text-xs text-slate-500">Disponível para novos projetos.</span></span><input class="checkbox" type="checkbox" formControlName="active"></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeTemplateModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button></div>
        </form>
      </div>
    }

    @if (phaseModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-2xl space-y-5 p-6" [formGroup]="phaseForm" (ngSubmit)="savePhaseTemplate()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingPhaseTemplate() ? 'Editar fase' : 'Nova fase' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closePhaseModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Planejamento"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cor</span><input class="field h-12" type="color" formControlName="color"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ícone</span><app-arqly-select formControlName="icon" placeholder="Selecione um ícone" [options]="iconOptions" panelMode="fixed" /></label>
          </div>
          <label class="space-y-1 block"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description" placeholder="Resumo da fase dentro do fluxo de trabalho"></textarea></label>
          <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3"><span class="text-sm font-bold text-slate-800">Fase ativa</span><input class="checkbox" type="checkbox" formControlName="active"></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closePhaseModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar fase</button></div>
        </form>
      </div>
    }

    @if (templateStageModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-3xl space-y-5 overflow-y-auto p-6" [formGroup]="stageTemplateForm" (ngSubmit)="saveTemplateStage()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingTemplateStage() ? 'Editar etapa' : 'Nova etapa' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closeTemplateStageModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Levantamento"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Peso (%)</span><input class="field" type="number" min="0" max="100" formControlName="weightPercentage"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cálculo de progresso</span><app-arqly-select formControlName="progressCalculationMode" placeholder="Modo de cálculo" [options]="progressModeOptions" panelMode="fixed" /></label>
          </div>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description" placeholder="Descreva o objetivo desta etapa"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Checklist padrão</span><textarea class="field min-h-32" formControlName="checklistText" placeholder="Um item por linha"></textarea></label>
          <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3"><span class="text-sm font-bold text-slate-800">Etapa ativa</span><input class="checkbox" type="checkbox" formControlName="active"></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeTemplateStageModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar etapa</button></div>
        </form>
      </div>
    }

    @if (projectCreateModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-3xl space-y-5 p-6" [formGroup]="projectCreateForm" (ngSubmit)="createProject()">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Novo projeto</p>
              <h3 class="mt-2 text-xl font-extrabold">Como deseja criar este projeto?</h3>
            </div>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeProjectCreateModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>

          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-4">
            @for (step of createSteps; track step.value) {
              <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                [class.bg-white]="projectCreateStep() === step.value"
                [class.text-arqly-700]="projectCreateStep() === step.value"
                [class.shadow-sm]="projectCreateStep() === step.value"
                [class.text-slate-500]="projectCreateStep() !== step.value"
                (click)="goToProjectCreateStep(step.value)">{{ step.label }}</button>
            }
          </div>

          @if (projectCreateStep() === 'client') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Cliente</p>
              <label class="mt-4 block space-y-1">
                <span class="text-xs font-bold text-slate-500">Cliente <span class="text-red-500">*</span></span>
                <app-arqly-select formControlName="clientId" placeholder="Selecione o cliente" [options]="clientOptions()" panelMode="fixed" />
              </label>
            </section>
          }

          @if (projectCreateStep() === 'origin') {
            <section class="space-y-4">
              <div class="grid gap-3 md:grid-cols-2">
                <button class="rounded-2xl border p-4 text-left transition" type="button"
                  [class.border-arqly-500]="projectCreateMode() === 'PROPOSAL'"
                  [class.bg-arqly-50]="projectCreateMode() === 'PROPOSAL'"
                  [class.border-slate-200]="projectCreateMode() !== 'PROPOSAL'"
                  (click)="setProjectCreateMode('PROPOSAL')">
                  <strong class="block">A partir de uma proposta</strong>
                  <span class="mt-1 block text-sm text-slate-500">Mantém a rastreabilidade comercial completa.</span>
                </button>
                <button class="rounded-2xl border p-4 text-left transition" type="button"
                  [class.border-arqly-500]="projectCreateMode() === 'MANUAL'"
                  [class.bg-arqly-50]="projectCreateMode() === 'MANUAL'"
                  [class.border-slate-200]="projectCreateMode() !== 'MANUAL'"
                  (click)="setProjectCreateMode('MANUAL')">
                  <strong class="block">Criar manualmente</strong>
                  <span class="mt-1 block text-sm text-slate-500">Para migrações, projetos antigos e clientes recorrentes.</span>
                </button>
              </div>
              @if (projectCreateMode() === 'PROPOSAL') {
                <label class="block space-y-1">
                  <span class="text-xs font-bold text-slate-500">Proposta aprovada <span class="text-red-500">*</span></span>
                  <app-arqly-select formControlName="proposalId" placeholder="Selecione a proposta aprovada" [options]="approvedProposalOptions()" panelMode="fixed" />
                </label>
              } @else {
                <p class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-500">Projetos criados manualmente não possuem rastreabilidade completa do processo comercial.</p>
              }
            </section>
          }

          @if (projectCreateStep() === 'info') {
            <section class="grid gap-4 md:grid-cols-2">
              <label class="space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Título <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Residência Vila Nova"></label>
              <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Modelo do projeto</span><app-arqly-select formControlName="templateId" placeholder="Sem modelo" [options]="templateCreateOptions()" panelMode="fixed" /></label>
              <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Responsável</span><app-arqly-select formControlName="responsibleUserId" placeholder="Selecione um usuário" [options]="tenantUserOptions()" panelMode="fixed" /></label>
              <div class="space-y-1"><span class="text-xs font-bold text-slate-500">Início previsto</span><app-arqly-date-picker formControlName="startDate" placeholder="Selecione" /></div>
              <div class="space-y-1"><span class="text-xs font-bold text-slate-500">Previsão de conclusão</span><app-arqly-date-picker formControlName="expectedEndDate" placeholder="Selecione" /></div>
              <label class="space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description" placeholder="Resumo do escopo e objetivo do projeto"></textarea></label>
            </section>
          }

          @if (projectCreateStep() === 'summary') {
            <section class="grid gap-4 md:grid-cols-[1fr_18rem]">
              <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Resumo</p>
                <h4 class="mt-3 text-2xl font-extrabold">{{ projectCreateForm.controls.name.value || 'Novo projeto' }}</h4>
                <p class="mt-2 text-sm text-slate-500">{{ selectedProjectClientName() || 'Cliente não selecionado' }}</p>
                <p class="mt-4 text-sm text-slate-600">{{ projectCreateMode() === 'PROPOSAL' ? 'Projeto criado a partir de proposta aprovada.' : 'Projeto criado manualmente, sem proposta vinculada.' }}</p>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-white p-5 text-sm">
                <div class="flex justify-between gap-4 py-2"><span class="font-bold text-slate-400">Origem</span><strong>{{ originLabel(projectCreateMode() === 'PROPOSAL' ? 'PROPOSAL' : 'MANUAL') }}</strong></div>
                <div class="flex justify-between gap-4 py-2"><span class="font-bold text-slate-400">Modelo</span><strong class="text-right">{{ selectedProjectTemplateName() || 'Sem modelo' }}</strong></div>
                <div class="flex justify-between gap-4 py-2"><span class="font-bold text-slate-400">Responsável</span><strong class="text-right">{{ selectedProjectResponsibleName() || 'A definir' }}</strong></div>
              </div>
            </section>
          }

          <div class="flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center md:justify-between">
            <p class="text-sm text-slate-500">O fluxo completo é recomendado, mas projetos manuais são permitidos.</p>
            <div class="flex justify-end gap-3">
              <button class="btn-secondary" type="button" (click)="previousProjectCreateStep()" [disabled]="projectCreateStep() === 'client'">Anterior</button>
              @if (projectCreateStep() !== 'summary') {
                <button class="btn-primary" type="button" (click)="nextProjectCreateStep()">Próxima</button>
              } @else {
                <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Criar projeto</button>
              }
            </div>
          </div>
        </form>
      </div>
    }

    @if (deleteProjectOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <div class="modal-panel card w-full max-w-md space-y-4 p-6">
          <h3 class="text-xl font-extrabold">Arquivar projeto</h3>
          <p class="text-sm text-slate-500">O projeto será removido da lista, sem exclusão física dos dados.</p>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="deleteProjectOpen.set(false)">Cancelar</button><button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="deleteProject()">Arquivar</button></div>
        </div>
      </div>
    }
  `
})
export class ProjectsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly baseUrl = 'http://localhost:8080/api/tenant/projects';
  readonly clientsUrl = 'http://localhost:8080/api/tenant/clients';

  readonly mode = signal<ViewMode>('projects');
  readonly projectCreateModalOpen = signal(false);
  readonly projectCreateMode = signal<ProjectCreateMode>('PROPOSAL');
  readonly projectCreateStep = signal<ProjectCreateStep>('client');
  readonly stats = signal<ProjectStats | null>(null);
  readonly projects = signal<ProjectSummary[]>([]);
  readonly templates = signal<ProjectTemplate[]>([]);
  readonly approvedProposals = signal<ProposalOption[]>([]);
  readonly tenantUsers = signal<TenantUserOption[]>([]);
  readonly templatePhases = signal<PhaseTemplate[]>([]);
  readonly selectedTemplate = signal<ProjectTemplate | null>(null);
  readonly clientOptions = signal<{ label: string; value: string }[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly templateModalOpen = signal(false);
  readonly phaseModalOpen = signal(false);
  readonly templateStageModalOpen = signal(false);
  readonly deleteProjectOpen = signal(false);
  readonly editingTemplate = signal<ProjectTemplate | null>(null);
  readonly editingPhaseTemplate = signal<PhaseTemplate | null>(null);
  readonly editingTemplateStage = signal<StageTemplate | null>(null);
  readonly selectedStagePhase = signal<PhaseTemplate | null>(null);
  readonly deletingProject = signal<ProjectSummary | null>(null);
  readonly expandedTemplatePhases = signal(new Set<string>());
  readonly draggedPhaseTemplate = signal<PhaseTemplate | null>(null);
  readonly draggedTemplateStage = signal<StageTemplate | null>(null);
  readonly createSteps: { label: string; value: ProjectCreateStep }[] = [
    { label: 'Cliente', value: 'client' },
    { label: 'Origem', value: 'origin' },
    { label: 'Informações', value: 'info' },
    { label: 'Resumo', value: 'summary' }
  ];

  readonly statusOptions = [
    { label: 'Planejamento', value: 'PLANNING' }, { label: 'Em andamento', value: 'IN_PROGRESS' },
    { label: 'Pausado', value: 'ON_HOLD' }, { label: 'Concluído', value: 'COMPLETED' }, { label: 'Cancelado', value: 'CANCELLED' }
  ];
  readonly iconOptions = PHASE_ICON_OPTIONS;
  readonly progressModeOptions = [
    { label: 'Manual', value: 'MANUAL' }, { label: 'Checklist', value: 'CHECKLIST' },
    { label: 'Datas', value: 'DATES' }, { label: 'Híbrido', value: 'HYBRID' }
  ];

  readonly filterForm = this.fb.nonNullable.group({ clientId: [''], status: [''], templateId: [''], responsible: [''], from: [''], to: [''] });
  readonly projectCreateForm = this.fb.nonNullable.group({
    clientId: ['', Validators.required],
    proposalId: [''],
    name: ['', Validators.required],
    templateId: [''],
    startDate: [''],
    expectedEndDate: [''],
    responsibleUserId: [''],
    projectManagerId: [''],
    description: [''],
    internalNotes: ['']
  });
  readonly templateForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], active: [true] });
  readonly phaseForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], color: ['#0f766e'], icon: ['Layers3'], active: [true] });
  readonly stageTemplateForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], weightPercentage: [0], progressCalculationMode: ['MANUAL'], active: [true], checklistText: [''] });

  ngOnInit() { this.loadStats(); this.loadClients(); this.loadTemplates(); this.loadUsers(); this.loadApprovedProposals(); this.loadProjects(); }
  switchMode(mode: ViewMode) { this.mode.set(mode); if (mode === 'templates') this.loadTemplates(); }
  search() { this.page.set(0); this.loadProjects(); }
  previousPage() { if (this.page() > 0) { this.page.set(this.page() - 1); this.loadProjects(); } }
  nextPage() { if (this.page() + 1 < this.totalPages()) { this.page.set(this.page() + 1); this.loadProjects(); } }
  templateFilterOptions() { return this.templates().map((t) => ({ label: t.name, value: t.id })); }

  loadStats() { this.http.get<ApiResponse<ProjectStats>>(`${this.baseUrl}/stats`).subscribe((r) => this.stats.set(r.data)); }
  loadClients() {
    this.http.get<ApiResponse<Page<ClientOption>>>(`${this.clientsUrl}?size=200`).subscribe((r) => {
      this.clientOptions.set((r.data.content || []).map((client) => ({ label: client.displayName, value: client.id })));
    });
  }
  loadUsers() {
    this.http.get<ApiResponse<Page<TenantUserOption>>>('http://localhost:8080/api/tenant/users?size=200&sort=name,asc').subscribe((r) => this.tenantUsers.set(r.data.content || []));
  }
  loadApprovedProposals() {
    this.http.get<ApiResponse<Page<ProposalOption>>>('http://localhost:8080/api/tenant/proposals?status=ACCEPTED&size=200&sort=createdAt,desc').subscribe((r) => {
      this.approvedProposals.set((r.data.content || []).filter((proposal) => !proposal.projectCreated));
    });
  }
  loadProjects() {
    const params = this.paramsFrom({ ...this.filterForm.getRawValue(), page: this.page(), size: 10, sort: 'createdAt,desc' });
    this.http.get<ApiResponse<Page<ProjectSummary>>>(`${this.baseUrl}?${params}`).subscribe((r) => {
      this.projects.set(r.data.content || []);
      this.totalPages.set(r.data.totalPages || 0);
      this.totalElements.set(r.data.totalElements || 0);
    });
  }
  loadTemplates() {
    this.http.get<ApiResponse<Page<ProjectTemplate>>>(`${this.baseUrl}/templates?size=200&sort=name,asc`).subscribe((r) => {
      this.templates.set(r.data.content || []);
      const selected = this.selectedTemplate();
      if (selected) {
        const fresh = this.templates().find((item) => item.id === selected.id) || null;
        this.selectedTemplate.set(fresh);
        if (fresh) this.loadTemplatePhases();
      }
    });
  }
  selectTemplate(template: ProjectTemplate) { this.selectedTemplate.set(template); this.loadTemplatePhases(); }
  loadTemplatePhases() {
    const template = this.selectedTemplate(); if (!template) return;
    this.http.get<ApiResponse<PhaseTemplate[]>>(`${this.baseUrl}/templates/${template.id}/phases`).subscribe((r) => {
      this.templatePhases.set(r.data || []);
      this.expandedTemplatePhases.set(new Set((r.data || []).map((phase) => phase.id)));
    });
  }

  openTemplateModal(template?: ProjectTemplate) { this.editingTemplate.set(template || null); this.templateForm.reset({ name: template?.name || '', description: template?.description || '', active: template?.active ?? true }); this.templateModalOpen.set(true); }
  closeTemplateModal() { this.templateModalOpen.set(false); }
  saveTemplate() {
    if (this.templateForm.invalid) { this.templateForm.markAllAsTouched(); this.toast.validation('Informe o nome do modelo.'); return; }
    const template = this.editingTemplate(); const payload = this.templateForm.getRawValue();
    const request = template ? this.http.put(`${this.baseUrl}/templates/${template.id}`, payload) : this.http.post(`${this.baseUrl}/templates`, payload);
    request.subscribe({ next: () => { this.closeTemplateModal(); this.toast.success(template ? 'Modelo atualizado' : 'Modelo criado'); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível salvar o modelo') });
  }
  duplicateTemplate(template: ProjectTemplate) { this.http.post(`${this.baseUrl}/templates/${template.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Modelo duplicado'); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível duplicar o modelo') }); }
  deleteTemplate(template: ProjectTemplate) { this.http.patch(`${this.baseUrl}/templates/${template.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Modelo removido'); this.selectedTemplate.set(null); this.templatePhases.set([]); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível remover o modelo') }); }

  openPhaseModal(phase?: PhaseTemplate) {
    this.editingPhaseTemplate.set(phase || null);
    this.phaseForm.reset({ name: phase?.name || '', description: phase?.description || '', order: phase?.order || this.templatePhases().length + 1, color: phase?.color || '#0f766e', icon: normalizedPhaseIcon(phase?.icon), active: phase?.active ?? true });
    this.phaseModalOpen.set(true);
  }
  closePhaseModal() { this.phaseModalOpen.set(false); }
  phaseIcon(icon?: string | null) { return normalizedPhaseIcon(icon); }
  savePhaseTemplate() {
    if (this.phaseForm.invalid) { this.phaseForm.markAllAsTouched(); this.toast.validation('Informe o nome da fase.'); return; }
    const template = this.selectedTemplate(); if (!template) return;
    const phase = this.editingPhaseTemplate(); const payload = this.phaseForm.getRawValue();
    const request = phase ? this.http.put(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}`, payload) : this.http.post(`${this.baseUrl}/templates/${template.id}/phases`, payload);
    request.subscribe({ next: () => { this.closePhaseModal(); this.toast.success(phase ? 'Fase atualizada' : 'Fase criada'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível salvar a fase') });
  }
  duplicatePhaseTemplate(phase: PhaseTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.post(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Fase duplicada'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível duplicar a fase') }); }
  deletePhaseTemplate(phase: PhaseTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.patch(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Fase removida'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível remover a fase') }); }
  toggleTemplatePhase(id: string) { const next = new Set(this.expandedTemplatePhases()); next.has(id) ? next.delete(id) : next.add(id); this.expandedTemplatePhases.set(next); }

  openTemplateStageModal(phase: PhaseTemplate, stage?: StageTemplate) {
    this.selectedStagePhase.set(phase); this.editingTemplateStage.set(stage || null);
    this.stageTemplateForm.reset({ name: stage?.name || '', description: stage?.description || '', order: stage?.order || phase.stages.length + 1, weightPercentage: stage?.weightPercentage || 0, progressCalculationMode: stage?.progressCalculationMode || 'MANUAL', active: stage?.active ?? true, checklistText: this.checklistToText(stage?.checklist || []) });
    this.templateStageModalOpen.set(true);
  }
  closeTemplateStageModal() { this.templateStageModalOpen.set(false); }
  saveTemplateStage() {
    if (this.stageTemplateForm.invalid) { this.stageTemplateForm.markAllAsTouched(); this.toast.validation('Informe o nome da etapa.'); return; }
    const template = this.selectedTemplate(); const phase = this.selectedStagePhase(); if (!template || !phase) return;
    const stage = this.editingTemplateStage(); const raw = this.stageTemplateForm.getRawValue();
    const payload = { ...raw, checklist: this.textToChecklist(raw.checklistText) };
    const request = stage ? this.http.put(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/stages/${stage.id}`, payload) : this.http.post(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/stages`, payload);
    request.subscribe({ next: () => { this.closeTemplateStageModal(); this.toast.success(stage ? 'Etapa atualizada' : 'Etapa criada'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível salvar a etapa') });
  }
  duplicateTemplateStage(phase: PhaseTemplate, stage: StageTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.post(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/stages/${stage.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Etapa duplicada'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível duplicar a etapa') }); }
  deleteTemplateStage(phase: PhaseTemplate, stage: StageTemplate) { const template = this.selectedTemplate(); if (!template) return; this.http.patch(`${this.baseUrl}/templates/${template.id}/phases/${phase.id}/stages/${stage.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Etapa removida'); this.loadTemplatePhases(); this.loadTemplates(); }, error: () => this.toast.error('Não foi possível remover a etapa') }); }

  startPhaseTemplateDrag(phase: PhaseTemplate) { this.draggedPhaseTemplate.set(phase); }
  startTemplateStageDrag(stage: StageTemplate) { this.draggedTemplateStage.set(stage); }
  allowDrop(event: DragEvent) { event.preventDefault(); }
  dropPhaseTemplate(target: PhaseTemplate) {
    const dragged = this.draggedPhaseTemplate(); const template = this.selectedTemplate(); if (!dragged || !template || dragged.id === target.id) return;
    const ordered = this.reordered(this.templatePhases(), dragged.id, target.id);
    const items = ordered.map((phase, index) => ({ id: phase.id, order: index + 1 }));
    this.http.patch<ApiResponse<PhaseTemplate[]>>(`${this.baseUrl}/templates/${template.id}/phases/reorder`, { items }).subscribe((r) => this.templatePhases.set(r.data || []));
  }
  dropTemplateStage(targetPhase: PhaseTemplate, targetStage: StageTemplate) {
    const dragged = this.draggedTemplateStage(); const template = this.selectedTemplate(); if (!dragged || !template) return;
    const stageIds = this.templatePhases().flatMap((phase) => phase.id === targetPhase.id ? this.reordered([...phase.stages, dragged].filter((stage, index, arr) => arr.findIndex((s) => s.id === stage.id) === index), dragged.id, targetStage.id) : phase.stages)
      .filter((stage) => stage.phaseTemplateId === targetPhase.id || stage.id === dragged.id)
      .map((stage, index) => ({ id: stage.id, order: index + 1, projectPhaseId: targetPhase.id }));
    this.http.patch<ApiResponse<PhaseTemplate[]>>(`${this.baseUrl}/templates/${template.id}/phases/${targetPhase.id}/stages/reorder`, { items: stageIds }).subscribe((r) => this.templatePhases.set(r.data || []));
  }

  openProjectDeleteModal(project: ProjectSummary) { this.deletingProject.set(project); this.deleteProjectOpen.set(true); }
  deleteProject() {
    const project = this.deletingProject(); if (!project) return;
    this.http.delete(`${this.baseUrl}/${project.id}`).subscribe({ next: () => { this.deleteProjectOpen.set(false); this.toast.success('Projeto arquivado'); this.loadProjects(); this.loadStats(); }, error: () => this.toast.error('Não foi possível arquivar o projeto') });
  }

  openProjectCreateModal() {
    this.projectCreateMode.set('PROPOSAL');
    this.projectCreateStep.set('client');
    this.projectCreateForm.reset({ clientId: '', proposalId: '', name: '', templateId: '', startDate: '', expectedEndDate: '', responsibleUserId: '', projectManagerId: '', description: '', internalNotes: '' });
    this.loadApprovedProposals();
    this.projectCreateModalOpen.set(true);
  }
  closeProjectCreateModal() { this.projectCreateModalOpen.set(false); }
  setProjectCreateMode(mode: ProjectCreateMode) {
    this.projectCreateMode.set(mode);
    if (mode === 'MANUAL') this.projectCreateForm.patchValue({ proposalId: '' });
  }
  goToProjectCreateStep(step: ProjectCreateStep) {
    if (this.canEnterProjectCreateStep(step)) this.projectCreateStep.set(step);
  }
  previousProjectCreateStep() {
    const current = this.createSteps.findIndex((step) => step.value === this.projectCreateStep());
    if (current > 0) this.projectCreateStep.set(this.createSteps[current - 1].value);
  }
  nextProjectCreateStep() {
    const current = this.createSteps.findIndex((step) => step.value === this.projectCreateStep());
    if (!this.validateProjectCreateStep(this.projectCreateStep())) return;
    if (this.projectCreateStep() === 'origin') this.applySelectedProposalDefaults();
    this.projectCreateStep.set(this.createSteps[Math.min(current + 1, this.createSteps.length - 1)].value);
  }
  canEnterProjectCreateStep(step: ProjectCreateStep) {
    if (step === 'client') return true;
    if (!this.projectCreateForm.controls.clientId.value) return false;
    if (step === 'origin') return true;
    if (this.projectCreateMode() === 'PROPOSAL' && !this.projectCreateForm.controls.proposalId.value) return false;
    return true;
  }
  validateProjectCreateStep(step: ProjectCreateStep) {
    if (step === 'client' && !this.projectCreateForm.controls.clientId.value) {
      this.toast.validation('Selecione o cliente.');
      return false;
    }
    if (step === 'origin' && this.projectCreateMode() === 'PROPOSAL' && !this.projectCreateForm.controls.proposalId.value) {
      this.toast.validation('Selecione uma proposta aprovada.');
      return false;
    }
    if (step === 'info' && !this.projectCreateForm.controls.name.value) {
      this.projectCreateForm.controls.name.markAsTouched();
      this.toast.validation('Informe o título do projeto.');
      return false;
    }
    return true;
  }
  createProject() {
    if (!this.validateProjectCreateStep('info')) return;
    const raw = this.projectCreateForm.getRawValue();
    const payload = {
      name: raw.name,
      clientId: raw.clientId,
      templateId: raw.templateId || null,
      description: raw.description || null,
      startDate: raw.startDate || null,
      expectedEndDate: raw.expectedEndDate || null,
      responsibleUserId: raw.responsibleUserId || null,
      projectManagerId: raw.projectManagerId || null,
      internalNotes: raw.internalNotes || null
    };
    const request = this.projectCreateMode() === 'PROPOSAL'
      ? this.http.post<ApiResponse<ProjectDetail>>(`${this.baseUrl}/from-proposal/${raw.proposalId}`, payload)
      : this.http.post<ApiResponse<ProjectDetail>>(`${this.baseUrl}/manual`, payload);
    request.subscribe({
      next: () => {
        this.closeProjectCreateModal();
        this.toast.success('Projeto criado');
        this.loadProjects();
        this.loadStats();
        this.loadApprovedProposals();
      },
      error: () => this.toast.error('Não foi possível criar o projeto')
    });
  }

  statusLabel(status: ProjectStatus) { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' }[status]); }
  statusClass(status: ProjectStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700'; if (status === 'ON_HOLD') return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
  originLabel(origin: OriginType | ProjectCreateMode) { return origin === 'PROPOSAL' ? 'Proposta' : origin === 'BRIEFING' ? 'Briefing' : 'Manual'; }
  originClass(origin: OriginType) { return origin === 'PROPOSAL' ? 'bg-arqly-50 text-arqly-700' : 'bg-blue-50 text-blue-700'; }
  approvedProposalOptions() {
    const clientId = this.projectCreateForm.controls.clientId.value;
    return this.approvedProposals()
      .filter((proposal) => !clientId || proposal.clientId === clientId)
      .map((proposal) => ({ label: `${proposal.number} · ${proposal.title}`, value: proposal.id }));
  }
  templateCreateOptions() { return [{ label: 'Sem modelo', value: '' }, ...this.templates().map((template) => ({ label: template.name, value: template.id }))]; }
  tenantUserOptions(includeEmpty = false) { const options = this.tenantUsers().map((user) => ({ label: `${user.name} · ${user.tenantAdmin ? 'Administrador' : 'Usuário comum'}`, value: user.id })); return includeEmpty ? [{ label: 'A definir', value: '' }, ...options] : options; }
  selectedProjectClientName() { const id = this.projectCreateForm.controls.clientId.value; return this.clientOptions().find((client) => client.value === id)?.label; }
  selectedProjectTemplateName() { const id = this.projectCreateForm.controls.templateId.value; return this.templates().find((template) => template.id === id)?.name; }
  selectedProjectResponsibleName() { const id = this.projectCreateForm.controls.responsibleUserId.value; return this.tenantUsers().find((user) => user.id === id)?.name; }
  private applySelectedProposalDefaults() {
    if (this.projectCreateMode() !== 'PROPOSAL') return;
    const proposal = this.approvedProposals().find((item) => item.id === this.projectCreateForm.controls.proposalId.value);
    if (!proposal) return;
    this.projectCreateForm.patchValue({ name: this.projectCreateForm.controls.name.value || proposal.title, clientId: proposal.clientId });
  }
  checklistToText(items: ChecklistItem[]) { return items.map((item) => item.title).join('\n'); }
  textToChecklist(text: string) { return (text || '').split('\n').map((line) => line.trim()).filter(Boolean).map((title, index) => ({ title, order: index + 1, completed: false })); }
  reordered<T extends { id: string }>(items: T[], draggedId: string, targetId: string) { const ordered = [...items]; const from = ordered.findIndex((item) => item.id === draggedId); const to = ordered.findIndex((item) => item.id === targetId); if (from < 0 || to < 0) return ordered; ordered.splice(to, 0, ordered.splice(from, 1)[0]); return ordered; }
  private paramsFrom(values: Record<string, unknown>) { const p = new URLSearchParams(); Object.entries(values).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') p.set(k, String(v)); }); return p; }
}

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, RouterLink, ArqlySelectComponent, ArqlyDatePickerComponent, ContextDocumentsComponent, FileExplorerComponent, RecentFilesComponent],
  template: `
    @if (project()) {
      <section class="space-y-5">
        <a class="inline-flex items-center gap-2 text-sm font-bold text-arqly-700" routerLink="/app/projects"><lucide-icon name="ArrowLeft" size="16"></lucide-icon>Voltar</a>
        <div class="card p-6">
          <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div><p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">{{ project()!.code }}</p><h2 class="mt-2 text-3xl font-extrabold">{{ project()!.name }}</h2><p class="mt-2 text-slate-500">{{ project()!.clientName }} · {{ project()!.proposalNumber || 'Projeto manual' }}</p></div>
            <div class="grid gap-2 text-sm md:text-right"><span class="rounded-full bg-arqly-50 px-3 py-1 font-bold text-arqly-700">{{ statusLabel(project()!.status) }}</span><strong>R$ {{ project()!.contractedValue || 0 | number:'1.2-2' }}</strong><span class="text-slate-500">{{ project()!.responsibleArchitect || 'Sem responsável' }}</span></div>
          </div>
          <div class="mt-5">
            <div class="flex items-center justify-between text-xs font-bold text-slate-500"><span>Progresso geral</span><span>{{ project()!.progressPercentage || 0 | number:'1.0-0' }}%</span></div>
            <div class="mt-2 h-3 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="project()!.progressPercentage || 0"></div></div>
          </div>
        </div>
        <div class="rounded-3xl border border-slate-200 bg-white p-2 shadow-sm">
          <div class="flex gap-2 overflow-x-auto rounded-2xl bg-slate-50/70 p-2">
          @for (tab of tabs; track tab.value) {
            <button class="inline-flex min-w-fit items-center gap-2 whitespace-nowrap rounded-xl px-4 py-3 text-sm font-bold transition disabled:cursor-not-allowed"
              type="button"
              [disabled]="tab.disabled"
              [class.bg-white]="activeTab() === tab.value"
              [class.text-arqly-700]="activeTab() === tab.value"
              [class.shadow-sm]="activeTab() === tab.value"
              [class.text-slate-500]="activeTab() !== tab.value && !tab.disabled"
              [class.opacity-50]="tab.disabled"
              (click)="!tab.disabled && activeTab.set(tab.value)">
              {{ tab.label }}
              @if (tab.disabled) {
                <span class="rounded-full bg-white px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.12em] text-slate-400">Em breve</span>
              }
            </button>
          }
          </div>
        </div>
        @if (activeTab() === 'summary') {
          <section class="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
            <div class="card p-6">
              <div class="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
                <div>
                  <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Visão geral</p>
                  <h3 class="mt-2 text-2xl font-extrabold">{{ project()!.name }}</h3>
                  <p class="mt-3 max-w-3xl text-sm leading-6 text-slate-500">{{ project()!.description || 'Projeto criado a partir da proposta aceita. Use as fases e etapas para conduzir a execução.' }}</p>
                </div>
                <span class="w-fit rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(project()!.status)">{{ statusLabel(project()!.status) }}</span>
              </div>
              <div class="mt-6">
                <div class="flex items-center justify-between text-xs font-bold text-slate-500"><span>Progresso do projeto</span><span>{{ project()!.progressPercentage || 0 | number:'1.0-0' }}%</span></div>
                <div class="mt-2 h-3 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="project()!.progressPercentage || 0"></div></div>
              </div>
              <div class="mt-6 grid gap-3 md:grid-cols-3">
                <div class="rounded-2xl bg-slate-50/70 p-4"><p class="text-xs font-bold text-slate-400">Valor contratado</p><strong class="mt-2 block text-xl">R$ {{ project()!.contractedValue || 0 | number:'1.2-2' }}</strong></div>
                <div class="rounded-2xl bg-slate-50/70 p-4"><p class="text-xs font-bold text-slate-400">Fases</p><strong class="mt-2 block text-xl">{{ phases().length }}</strong></div>
                <div class="rounded-2xl bg-slate-50/70 p-4"><p class="text-xs font-bold text-slate-400">Etapas</p><strong class="mt-2 block text-xl">{{ totalStages() }}</strong></div>
              </div>
              <div class="mt-6 grid gap-4 lg:grid-cols-2">
                <div class="rounded-2xl border border-slate-200 bg-white p-4">
                  <div class="mb-4 flex items-center justify-between"><h4 class="font-extrabold">Andamento por fase</h4><span class="text-xs font-bold text-slate-400">{{ phases().length }} fase(s)</span></div>
                  <div class="space-y-3">
                    @for (phase of phases().slice(0, 5); track phase.id) {
                      <div>
                        <div class="flex items-center justify-between gap-3 text-sm"><span class="truncate font-bold">{{ phase.name }}</span><span class="text-xs font-bold text-slate-400">{{ phase.completionPercentage || 0 | number:'1.0-0' }}%</span></div>
                        <div class="mt-2 h-2 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="phase.completionPercentage || 0"></div></div>
                      </div>
                    } @empty {
                      <p class="text-sm text-slate-500">Nenhuma fase cadastrada.</p>
                    }
                  </div>
                </div>
                <div class="rounded-2xl border border-slate-200 bg-white p-4">
                  <div class="mb-4 flex items-center justify-between"><h4 class="font-extrabold">Próximas etapas</h4><span class="text-xs font-bold text-slate-400">{{ openStages().length }} aberta(s)</span></div>
                  <div class="space-y-3">
                    @for (stage of openStages().slice(0, 4); track stage.id) {
                      <a class="block rounded-2xl bg-slate-50/70 p-3 transition hover:bg-arqly-50" [routerLink]="['/app/projects', project()!.id, 'stages', stage.id]">
                        <div class="flex items-center justify-between gap-3"><strong class="truncate text-sm">{{ stage.name }}</strong><span class="rounded-full px-2 py-1 text-[11px] font-bold" [class]="stageStatusClass(stage.status)">{{ stageStatusLabel(stage.status) }}</span></div>
                        <p class="mt-1 text-xs font-semibold text-slate-400">{{ stage.responsible || 'Sem responsável' }} · {{ stage.completionPercentage || 0 | number:'1.0-0' }}%</p>
                      </a>
                    } @empty {
                      <p class="text-sm text-slate-500">Nenhuma etapa em aberto.</p>
                    }
                  </div>
                </div>
              </div>
            </div>
            <aside class="card h-fit p-6">
              <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Dados do projeto</p>
              <div class="mt-5 divide-y divide-slate-100 text-sm">
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Cliente</span><strong class="text-right">{{ project()!.clientName }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Origem</span><strong class="text-right">{{ originLabel(project()!.originType) }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Proposta</span><strong class="text-right">{{ project()!.proposalNumber || 'Sem proposta vinculada' }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Modelo</span><strong class="text-right">{{ project()!.templateName || 'Sem modelo' }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Responsável</span><strong class="text-right">{{ project()!.responsibleArchitect || 'A definir' }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Início</span><strong class="text-right">{{ project()!.startDate ? (project()!.startDate | date:'dd/MM/yyyy') : '-' }}</strong></div>
                <div class="flex items-start justify-between gap-4 py-3"><span class="font-bold text-slate-400">Previsão</span><strong class="text-right">{{ project()!.expectedEndDate ? (project()!.expectedEndDate | date:'dd/MM/yyyy') : '-' }}</strong></div>
              </div>
            </aside>
            <app-recent-files class="xl:col-span-2" ownerType="PROJECT" [ownerId]="project()!.id" />
          </section>
        }
        @if (activeTab() === 'stages') {
          <div class="card overflow-hidden">
            <div class="flex flex-col gap-3 border-b border-slate-200 p-5 md:flex-row md:items-center md:justify-between">
              <div><h3 class="text-xl font-extrabold">Fases e etapas</h3><p class="mt-1 text-sm text-slate-500">A execução agora fica organizada por fases expansíveis.</p></div>
              <button class="btn-primary" type="button" (click)="openPhaseModal()"><lucide-icon name="Plus" size="18"></lucide-icon>Adicionar fase</button>
            </div>
            <div class="space-y-4 p-5">
              @for (phase of phases(); track phase.id) {
                <div class="rounded-3xl border border-slate-200 bg-white" draggable="true" (dragstart)="startPhaseDrag(phase)" (dragover)="allowDrop($event)" (drop)="dropPhase(phase)">
                  <button class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left" type="button" (click)="togglePhase(phase.id)">
                    <span class="flex min-w-0 items-center gap-3">
                      <span class="grid h-10 w-10 place-items-center rounded-2xl text-white" [style.background]="phase.color || '#0f766e'"><lucide-icon [name]="phaseIcon(phase.icon)" size="18"></lucide-icon></span>
                      <span class="min-w-0"><strong class="block truncate">{{ phase.order }}. {{ phase.name }}</strong><span class="text-xs font-bold text-slate-400">{{ phase.completionPercentage || 0 | number:'1.0-0' }}% · {{ phase.stages.length }} etapa(s)</span></span>
                    </span>
                    <span class="flex items-center gap-2">
                      <button class="btn-secondary px-3 py-2" type="button" (click)="openStageModal(phase); $event.stopPropagation()"><lucide-icon name="Plus" size="16"></lucide-icon>Etapa</button>
                      <button class="btn-secondary px-3 py-2" type="button" (click)="openPhaseModal(phase); $event.stopPropagation()"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2" type="button" (click)="duplicatePhase(phase); $event.stopPropagation()"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                      <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deletePhase(phase); $event.stopPropagation()"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      <lucide-icon [name]="expandedPhases().has(phase.id) ? 'ChevronUp' : 'ChevronDown'" size="18"></lucide-icon>
                    </span>
                  </button>
                  <div class="px-5 pb-4"><div class="h-2 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="phase.completionPercentage || 0"></div></div></div>
                  @if (expandedPhases().has(phase.id)) {
                    <div class="space-y-3 border-t border-slate-100 p-4">
                      @for (stage of phase.stages; track stage.id) {
                        <div class="grid gap-4 rounded-2xl bg-slate-50/70 p-4 md:grid-cols-[1fr_auto]" draggable="true" (dragstart)="startStageDrag(stage)" (dragover)="allowDrop($event)" (drop)="dropStage(phase, stage)">
                          <div>
                            <div class="flex flex-wrap items-center gap-2"><a class="font-extrabold text-slate-900 transition hover:text-arqly-700" [routerLink]="['/app/projects', project()!.id, 'stages', stage.id]">{{ stage.order }}. {{ stage.name }}</a><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="stageStatusClass(stage.status)">{{ stageStatusLabel(stage.status) }}</span><span class="rounded-full bg-white px-3 py-1 text-xs font-bold text-slate-500">{{ stage.completionPercentage || 0 }}%</span></div>
                            <p class="mt-1 text-sm text-slate-500">{{ stage.description || 'Sem descrição' }}</p>
                            <div class="mt-3 h-2 max-w-xl overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="stage.completionPercentage || 0"></div></div>
                            <div class="mt-3 flex flex-wrap gap-3 text-xs font-bold text-slate-400">
                              <span>Previsto: {{ stage.plannedStart ? (stage.plannedStart | date:'dd/MM/yyyy') : '-' }} → {{ stage.plannedEnd ? (stage.plannedEnd | date:'dd/MM/yyyy') : '-' }}</span>
                              <span>Responsável: {{ stage.responsible || 'A definir' }}</span>
                              <span>Arquivos: {{ stage.fileCount || 0 }}</span>
                              <span>Timeline: {{ stage.timelineEventCount || 0 }}</span>
                              <span>Checklist: {{ completedChecklist(stage) }} de {{ stage.checklist.length }} itens concluídos</span>
                            </div>
                            @if (stage.checklist.length) {
                              <div class="mt-3 grid gap-2 sm:grid-cols-2">
                                @for (item of stage.checklist; track item.id || item.title) {
                                  <div class="rounded-2xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-600">{{ item.completed ? '✓' : '○' }} {{ item.title }}</div>
                                }
                              </div>
                            }
                          </div>
                          <div class="flex items-start justify-end gap-2">
                            <a class="btn-secondary px-3 py-2" [routerLink]="['/app/projects', project()!.id, 'stages', stage.id]"><lucide-icon name="Search" size="16"></lucide-icon>Abrir workspace</a>
                            <button class="btn-secondary px-3 py-2" type="button" (click)="openStageModal(phase, stage)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                            <button class="btn-secondary px-3 py-2" type="button" (click)="duplicateStage(phase, stage)"><lucide-icon name="Copy" size="16"></lucide-icon></button>
                            <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteStage(phase, stage)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                          </div>
                        </div>
                      } @empty {
                        <p class="rounded-2xl bg-slate-50/70 py-8 text-center text-sm text-slate-500">Nenhuma etapa nesta fase.</p>
                      }
                    </div>
                  }
                </div>
              } @empty {
                <p class="card py-12 text-center text-sm text-slate-500">Nenhuma fase cadastrada para este projeto.</p>
              }
            </div>
          </div>
        }
        @if (activeTab() === 'services') {
          <div class="card overflow-x-auto"><table class="w-full min-w-[760px] text-left text-sm"><thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-6 py-4">Serviço</th><th class="px-6 py-4">Qtd.</th><th class="px-6 py-4">Unidade</th><th class="px-6 py-4">Valor contratado</th></tr></thead><tbody>@for (service of project()!.services; track service.id) {<tr class="border-t border-slate-100"><td class="px-6 py-4"><p class="font-bold">{{ service.name }}</p><p class="text-xs text-slate-500">{{ service.description || '-' }}</p></td><td class="px-6 py-4">{{ service.quantity }}</td><td class="px-6 py-4">{{ billingUnitLabel(service.unit) }}</td><td class="px-6 py-4">R$ {{ service.contractedValue || 0 | number:'1.2-2' }}</td></tr>}</tbody></table></div>
        }
        @if (activeTab() === 'info') {
          <form class="card space-y-6 overflow-visible p-6" [formGroup]="form" (ngSubmit)="save()">
            <div class="flex flex-col gap-2 border-b border-slate-100 pb-5">
              <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Informações</p>
              <h3 class="text-xl font-extrabold">Dados editáveis</h3>
              <p class="max-w-3xl text-sm leading-6 text-slate-500">Atualize apenas os dados operacionais do projeto. Cliente, proposta e serviços contratados permanecem como histórico da conversão.</p>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Nome *</span><input class="field w-full" formControlName="name" placeholder="Ex.: Residência Vila Nova"></label>
              <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Status</span><app-arqly-select formControlName="status" placeholder="Selecione o status" [options]="statusOptions" panelMode="fixed" /></label>
              <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Responsável</span><app-arqly-select formControlName="responsibleUserId" placeholder="Selecione um usuário" [options]="tenantUserOptions()" panelMode="fixed" /></label>
              <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Gerente do projeto</span><app-arqly-select formControlName="projectManagerId" placeholder="Opcional" [options]="tenantUserOptions(true)" panelMode="fixed" /></label>
              <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Previsão de conclusão</span><app-arqly-date-picker formControlName="expectedEndDate" placeholder="Selecione" /></div>
            </div>
            <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-28 w-full" formControlName="description" placeholder="Resumo do escopo e objetivo do projeto"></textarea></label>
            <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Dados internos</span><textarea class="field min-h-28 w-full" formControlName="internalNotes" placeholder="Notas internas para a equipe"></textarea></label>
            <div class="flex justify-end"><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button></div>
          </form>
        }
          @if (activeTab() === 'documents') {
            <div>
              <app-context-documents [projectId]="project()!.id" />
            </div>
          }
          @if (activeTab() === 'files') {
            <div>
              <app-file-explorer ownerType="PROJECT" [ownerId]="project()!.id" title="Arquivos do projeto" eyebrow="Acervo técnico" />
            </div>
          }
          @if (activeTab() === 'approvals') {
            <div class="grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
              <form class="card space-y-4 p-6" [formGroup]="approvalForm" (ngSubmit)="createApproval()">
                <div>
                  <p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Portal do cliente</p>
                  <h3 class="mt-2 text-xl font-extrabold">Solicitar aprovação</h3>
                  <p class="mt-2 text-sm leading-6 text-slate-500">Crie uma pendência para o cliente aprovar ou solicitar ajustes pelo portal.</p>
                </div>
                <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Descrição *</span><textarea class="field min-h-28 w-full" formControlName="description" placeholder="Ex.: Aprovar estudo preliminar"></textarea></label>
                <label class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Etapa</span><app-arqly-select formControlName="stageId" placeholder="Projeto geral" [options]="stageApprovalOptions()" panelMode="fixed" /></label>
                <div class="block space-y-1"><span class="block text-xs font-bold text-slate-500">Prazo</span><app-arqly-date-picker formControlName="deadline" placeholder="Selecione a data" /></div>
                <div class="flex justify-end"><button class="btn-primary" type="submit"><lucide-icon name="Send" size="18"></lucide-icon>Solicitar</button></div>
              </form>
              <section class="card overflow-hidden">
                <div class="border-b border-slate-200 p-5"><h3 class="text-xl font-extrabold">Aprovações</h3></div>
                <div class="divide-y divide-slate-100">
                  @for (approval of approvals(); track approval.id) {
                    <article class="p-5">
                      <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                        <div>
                          <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="approvalStatusClass(approval.status)">{{ approvalStatusLabel(approval.status) }}</span>
                          <h4 class="mt-3 font-extrabold">{{ approval.description }}</h4>
                          <p class="mt-1 text-sm text-slate-500">{{ approval.stageName || 'Projeto geral' }} · criado por {{ approval.createdByName }}</p>
                          @if (approval.clientComment) { <p class="mt-3 rounded-2xl bg-slate-50 p-3 text-sm text-slate-600">{{ approval.clientComment }}</p> }
                        </div>
                        <p class="text-sm font-bold text-slate-500">{{ approval.deadline ? (approval.deadline | date:'dd/MM/yyyy') : 'Sem prazo' }}</p>
                      </div>
                    </article>
                  } @empty {
                    <p class="p-5 text-sm text-slate-500">Nenhuma aprovação solicitada ainda.</p>
                  }
                </div>
              </section>
            </div>
          }
      </section>
    }

    @if (phaseModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-2xl space-y-5 p-6" [formGroup]="phaseForm" (ngSubmit)="savePhase()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingPhase() ? 'Editar fase' : 'Nova fase' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closePhaseModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Planejamento"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cor</span><input class="field h-12" type="color" formControlName="color"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ícone</span><app-arqly-select formControlName="icon" placeholder="Selecione um ícone" [options]="iconOptions" panelMode="fixed" /></label>
          </div>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description" placeholder="Resumo da fase dentro deste projeto"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Observações</span><textarea class="field min-h-24" formControlName="notes" placeholder="Informações internas sobre a fase"></textarea></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closePhaseModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar fase</button></div>
        </form>
      </div>
    }

    @if (stageModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-3xl space-y-5 overflow-y-auto p-6" [formGroup]="stageForm" (ngSubmit)="saveStage()">
          <div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">{{ editingStage() ? 'Editar etapa' : 'Nova etapa' }}</h3><button class="btn-secondary px-3 py-2" type="button" (click)="closeStageModal()"><lucide-icon name="X" size="18"></lucide-icon></button></div>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Estudo Preliminar"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Status</span><app-arqly-select formControlName="status" placeholder="Selecione o status" [options]="stageStatusOptions" panelMode="fixed" /></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Ordem</span><input class="field" type="number" formControlName="order"></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Percentual</span><input class="field" type="number" min="0" max="100" formControlName="completionPercentage"></label>
            <div class="space-y-1"><span class="text-xs font-bold text-slate-500">Início previsto</span><app-arqly-date-picker formControlName="plannedStart" placeholder="Selecione" /></div>
            <div class="space-y-1"><span class="text-xs font-bold text-slate-500">Término previsto</span><app-arqly-date-picker formControlName="plannedEnd" placeholder="Selecione" /></div>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Responsável</span><app-arqly-select formControlName="responsibleUserId" placeholder="Herdar do projeto" [options]="tenantUserOptions(true)" panelMode="fixed" /></label>
            <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cálculo de progresso</span><app-arqly-select formControlName="progressCalculationMode" placeholder="Modo de cálculo" [options]="progressModeOptions" panelMode="fixed" /></label>
          </div>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><textarea class="field min-h-24" formControlName="description" placeholder="Descreva o objetivo desta etapa"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Observações</span><textarea class="field min-h-24" formControlName="notes" placeholder="Anotações internas da etapa"></textarea></label>
          <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Checklist</span><textarea class="field min-h-32" formControlName="checklistText" placeholder="Um item por linha"></textarea></label>
          <div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="closeStageModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar etapa</button></div>
        </form>
      </div>
    }
  `
})
export class ProjectDetailComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly baseUrl = 'http://localhost:8080/api/tenant/projects';
  readonly project = signal<ProjectDetail | null>(null);
  readonly phases = signal<ProjectPhase[]>([]);
  readonly activeTab = signal('summary');
  readonly phaseModalOpen = signal(false);
  readonly stageModalOpen = signal(false);
  readonly tenantUsers = signal<TenantUserOption[]>([]);
  readonly editingPhase = signal<ProjectPhase | null>(null);
  readonly editingStage = signal<ProjectStage | null>(null);
  readonly selectedStagePhase = signal<ProjectPhase | null>(null);
  readonly expandedPhases = signal(new Set<string>());
  readonly draggedPhase = signal<ProjectPhase | null>(null);
  readonly draggedStage = signal<ProjectStage | null>(null);
  readonly approvals = signal<ProjectApproval[]>([]);
    readonly tabs = [{ label: 'Resumo', value: 'summary' }, { label: 'Etapas', value: 'stages' }, { label: 'Serviços', value: 'services' }, { label: 'Informações', value: 'info' }, { label: 'Documentos', value: 'documents' }, { label: 'Arquivos', value: 'files' }, { label: 'Aprovações', value: 'approvals' }, { label: 'Financeiro', value: 'finance', disabled: true }];
  readonly statusOptions = [{ label: 'Planejamento', value: 'PLANNING' }, { label: 'Em andamento', value: 'IN_PROGRESS' }, { label: 'Pausado', value: 'ON_HOLD' }, { label: 'Concluído', value: 'COMPLETED' }, { label: 'Cancelado', value: 'CANCELLED' }];
  readonly stageStatusOptions = [{ label: 'Não iniciada', value: 'NOT_STARTED' }, { label: 'Em andamento', value: 'IN_PROGRESS' }, { label: 'Aguardando cliente', value: 'WAITING_CLIENT' }, { label: 'Aguardando aprovação', value: 'WAITING_APPROVAL' }, { label: 'Pausada', value: 'ON_HOLD' }, { label: 'Concluída', value: 'COMPLETED' }, { label: 'Cancelada', value: 'CANCELLED' }];
  readonly iconOptions = PHASE_ICON_OPTIONS;
  readonly progressModeOptions = [{ label: 'Manual', value: 'MANUAL' }, { label: 'Checklist', value: 'CHECKLIST' }, { label: 'Datas', value: 'DATES' }, { label: 'Híbrido', value: 'HYBRID' }];
  readonly form = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], status: ['PLANNING'], responsibleUserId: [''], projectManagerId: [''], responsibleArchitect: [''], expectedEndDate: [''], internalNotes: [''] });
  readonly phaseForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], color: ['#0f766e'], icon: ['Layers3'], notes: [''] });
  readonly stageForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], order: [1], status: ['NOT_STARTED'], plannedStart: [''], plannedEnd: [''], actualStart: [''], actualEnd: [''], completionPercentage: [0], weightPercentage: [0], progressCalculationMode: ['MANUAL'], responsibleUserId: [''], responsible: [''], notes: [''], checklistText: [''] });
  readonly approvalForm = this.fb.nonNullable.group({ description: ['', Validators.required], stageId: [''], deadline: [''] });

  ngOnInit() { this.loadUsers(); this.load(); this.loadApprovals(); }
  projectId() { return this.route.snapshot.paramMap.get('id')!; }
  load() {
    this.http.get<ApiResponse<ProjectDetail>>(`${this.baseUrl}/${this.projectId()}`).subscribe((r) => {
      this.project.set(r.data);
      this.phases.set(r.data.phases || []);
      this.expandedPhases.set(new Set((r.data.phases || []).map((phase) => phase.id)));
      this.form.patchValue({ name: r.data.name, description: r.data.description || '', status: r.data.status, responsibleUserId: r.data.responsibleUserId || '', projectManagerId: r.data.projectManagerId || '', responsibleArchitect: r.data.responsibleArchitect || '', expectedEndDate: r.data.expectedEndDate || '', internalNotes: r.data.internalNotes || '' });
    });
  }
  loadUsers() { this.http.get<ApiResponse<Page<TenantUserOption>>>('http://localhost:8080/api/tenant/users?size=200&sort=name,asc').subscribe((response) => this.tenantUsers.set(response.data.content)); }
  loadApprovals() { this.http.get<ApiResponse<ProjectApproval[]>>(`http://localhost:8080/api/tenant/approvals/projects/${this.projectId()}`).subscribe((response) => this.approvals.set(response.data)); }
  save() { const p = this.project(); if (!p) return; const raw = this.form.getRawValue(); this.http.put<ApiResponse<ProjectDetail>>(`${this.baseUrl}/${p.id}`, { ...raw, responsibleUserId: raw.responsibleUserId || null, projectManagerId: raw.projectManagerId || null, templateId: p.templateId, startDate: p.startDate, completedAt: p.completedAt }).subscribe({ next: () => { this.toast.success('Projeto atualizado'); this.load(); }, error: () => this.toast.error('Não foi possível salvar o projeto') }); }

  openPhaseModal(phase?: ProjectPhase) { this.editingPhase.set(phase || null); this.phaseForm.reset({ name: phase?.name || '', description: phase?.description || '', order: phase?.order || this.phases().length + 1, color: phase?.color || '#0f766e', icon: normalizedPhaseIcon(phase?.icon), notes: phase?.notes || '' }); this.phaseModalOpen.set(true); }
  closePhaseModal() { this.phaseModalOpen.set(false); }
  phaseIcon(icon?: string | null) { return normalizedPhaseIcon(icon); }
  savePhase() {
    if (this.phaseForm.invalid) { this.phaseForm.markAllAsTouched(); this.toast.validation('Informe o nome da fase.'); return; }
    const phase = this.editingPhase(); const payload = { ...this.phaseForm.getRawValue(), status: phase?.status || 'NOT_STARTED' };
    const request = phase ? this.http.put(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}`, payload) : this.http.post(`${this.baseUrl}/${this.projectId()}/phases`, payload);
    request.subscribe({ next: () => { this.closePhaseModal(); this.toast.success(phase ? 'Fase atualizada' : 'Fase criada'); this.load(); }, error: () => this.toast.error('Não foi possível salvar a fase') });
  }
  duplicatePhase(phase: ProjectPhase) { this.http.post(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Fase duplicada'); this.load(); }, error: () => this.toast.error('Não foi possível duplicar a fase') }); }
  deletePhase(phase: ProjectPhase) { this.http.patch(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Fase removida'); this.load(); }, error: () => this.toast.error('Não foi possível remover a fase') }); }
  togglePhase(id: string) { const next = new Set(this.expandedPhases()); next.has(id) ? next.delete(id) : next.add(id); this.expandedPhases.set(next); }

  openStageModal(phase: ProjectPhase, stage?: ProjectStage) { this.selectedStagePhase.set(phase); this.editingStage.set(stage || null); this.stageForm.reset({ name: stage?.name || '', description: stage?.description || '', order: stage?.order || phase.stages.length + 1, status: stage?.status || 'NOT_STARTED', plannedStart: stage?.plannedStart || '', plannedEnd: stage?.plannedEnd || '', actualStart: stage?.actualStart || '', actualEnd: stage?.actualEnd || '', completionPercentage: stage?.completionPercentage || 0, weightPercentage: stage?.weightPercentage || 0, progressCalculationMode: stage?.progressCalculationMode || 'MANUAL', responsibleUserId: stage?.responsibleUserId || '', responsible: stage?.responsible || '', notes: stage?.notes || '', checklistText: this.checklistToText(stage?.checklist || []) }); this.stageModalOpen.set(true); }
  closeStageModal() { this.stageModalOpen.set(false); }
  saveStage() {
    if (this.stageForm.invalid) { this.stageForm.markAllAsTouched(); this.toast.validation('Informe o nome da etapa.'); return; }
    const phase = this.selectedStagePhase(); if (!phase) return;
    const stage = this.editingStage(); const raw = this.stageForm.getRawValue();
    const payload = { ...raw, responsibleUserId: raw.responsibleUserId || null, projectPhaseId: phase.id, checklist: this.textToChecklist(raw.checklistText) };
    const request = stage ? this.http.put(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/stages/${stage.id}`, payload) : this.http.post(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/stages`, payload);
    request.subscribe({ next: () => { this.closeStageModal(); this.toast.success(stage ? 'Etapa atualizada' : 'Etapa criada'); this.load(); }, error: () => this.toast.error('Não foi possível salvar a etapa') });
  }
  duplicateStage(phase: ProjectPhase, stage: ProjectStage) { this.http.post(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/stages/${stage.id}/duplicate`, {}).subscribe({ next: () => { this.toast.success('Etapa duplicada'); this.load(); }, error: () => this.toast.error('Não foi possível duplicar a etapa') }); }
  deleteStage(phase: ProjectPhase, stage: ProjectStage) { this.http.patch(`${this.baseUrl}/${this.projectId()}/phases/${phase.id}/stages/${stage.id}/delete`, {}).subscribe({ next: () => { this.toast.success('Etapa removida'); this.load(); }, error: () => this.toast.error('Não foi possível remover a etapa') }); }
  createApproval() {
    if (this.approvalForm.invalid) { this.approvalForm.markAllAsTouched(); this.toast.validation('Informe a descrição da aprovação.'); return; }
    const raw = this.approvalForm.getRawValue();
    this.http.post<ApiResponse<ProjectApproval>>('http://localhost:8080/api/tenant/approvals', {
      projectId: this.projectId(), stageId: raw.stageId || null, description: raw.description, deadline: raw.deadline || null
    }).subscribe({ next: () => { this.toast.success('Aprovação solicitada'); this.approvalForm.reset({ description: '', stageId: '', deadline: '' }); this.loadApprovals(); }, error: () => this.toast.error('Não foi possível solicitar a aprovação') });
  }
  startPhaseDrag(phase: ProjectPhase) { this.draggedPhase.set(phase); }
  startStageDrag(stage: ProjectStage) { this.draggedStage.set(stage); }
  allowDrop(event: DragEvent) { event.preventDefault(); }
  dropPhase(target: ProjectPhase) {
    const dragged = this.draggedPhase(); if (!dragged || dragged.id === target.id) return;
    const ordered = this.reordered(this.phases(), dragged.id, target.id);
    const items = ordered.map((phase, index) => ({ id: phase.id, order: index + 1 }));
    this.http.patch<ApiResponse<ProjectPhase[]>>(`${this.baseUrl}/${this.projectId()}/phases/reorder`, { items }).subscribe((r) => this.phases.set(r.data || []));
  }
  dropStage(targetPhase: ProjectPhase, targetStage: ProjectStage) {
    const dragged = this.draggedStage(); if (!dragged) return;
    const stages = targetPhase.stages.some((stage) => stage.id === dragged.id) ? targetPhase.stages : [...targetPhase.stages, dragged];
    const items = this.reordered(stages, dragged.id, targetStage.id).map((stage, index) => ({ id: stage.id, order: index + 1, projectPhaseId: targetPhase.id }));
    this.http.patch<ApiResponse<ProjectPhase[]>>(`${this.baseUrl}/${this.projectId()}/phases/${targetPhase.id}/stages/reorder`, { items }).subscribe((r) => this.phases.set(r.data || []));
  }
  statusLabel(status: ProjectStatus) { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' }[status]); }
  statusClass(status: ProjectStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700'; if (status === 'ON_HOLD') return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
  originLabel(origin: OriginType | ProjectCreateMode) { return origin === 'PROPOSAL' ? 'Proposta' : origin === 'BRIEFING' ? 'Briefing' : 'Manual'; }
  stageStatusLabel(status: StageStatus) { return ({ NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING_CLIENT: 'Aguardando cliente', WAITING_APPROVAL: 'Aguardando aprovação', ON_HOLD: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }[status]); }
  completedChecklist(stage: ProjectStage) { return stage.checklist.filter((item) => item.completed).length; }
  stageStatusClass(status: StageStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700'; if (status === 'ON_HOLD' || status.startsWith('WAITING')) return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
  totalStages() { return this.phases().reduce((total, phase) => total + phase.stages.length, 0); }
  openStages() { return this.phases().flatMap((phase) => phase.stages).filter((stage) => stage.status !== 'COMPLETED' && stage.status !== 'CANCELLED'); }
  billingUnitLabel(unit: string) { return ({ UN: 'Unidade', M2: 'm²', M: 'Metro', HOUR: 'Hora', DAY: 'Dia', MONTH: 'Mês', PROJECT: 'Projeto', VISIT: 'Visita', OTHER: 'Outro' }[unit] || unit); }
  tenantUserOptions(includeEmpty = false) { const options = this.tenantUsers().map((user) => ({ label: `${user.name} · ${user.tenantAdmin ? 'Administrador' : 'Usuário comum'}`, value: user.id })); return includeEmpty ? [{ label: 'Herdar do projeto', value: '' }, ...options] : options; }
  stageApprovalOptions() { return [{ label: 'Projeto geral', value: '' }, ...this.phases().flatMap((phase) => phase.stages.map((stage) => ({ label: `${phase.name} · ${stage.name}`, value: stage.id })))]; }
  approvalStatusLabel(status: ProjectApproval['status']) { return ({ PENDING: 'Pendente', APPROVED: 'Aprovada', REJECTED: 'Ajustes solicitados', EXPIRED: 'Expirada', CANCELLED: 'Cancelada' }[status]); }
  approvalStatusClass(status: ProjectApproval['status']) { if (status === 'APPROVED') return 'bg-arqly-50 text-arqly-700'; if (status === 'REJECTED') return 'bg-red-50 text-red-700'; if (status === 'PENDING') return 'bg-amber-50 text-amber-700'; return 'bg-slate-100 text-slate-600'; }
  checklistToText(items: ChecklistItem[]) { return items.map((item) => item.title).join('\n'); }
  textToChecklist(text: string) { return (text || '').split('\n').map((line) => line.trim()).filter(Boolean).map((title, index) => ({ title, order: index + 1, completed: false })); }
  reordered<T extends { id: string }>(items: T[], draggedId: string, targetId: string) { const ordered = [...items]; const from = ordered.findIndex((item) => item.id === draggedId); const to = ordered.findIndex((item) => item.id === targetId); if (from < 0 || to < 0) return ordered; ordered.splice(to, 0, ordered.splice(from, 1)[0]); return ordered; }
}
