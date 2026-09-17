import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { MarkdownPreviewComponent } from '../../shared/components/markdown-preview.component';
import { MarkdownEditorComponent } from '../../shared/components/markdown-editor.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type DocumentView = 'templates' | 'generated';
type TemplateMode = 'create' | 'edit' | 'version';
type SourceType = 'PROJECT' | 'PROPOSAL' | 'CLIENT';
interface Page<T> { content: T[]; number: number; totalPages: number; totalElements: number; }
interface TemplateSummary { id: string; seriesId: string; name: string; description?: string; category: string; active: boolean; archived: boolean; version: number; createdAt: string; updatedAt: string; }
interface TemplateDetail extends TemplateSummary { previousVersionId?: string; content: string; }
interface GeneratedSummary { id: string; seriesId: string; templateId: string; templateName: string; category: string; projectId?: string; projectName?: string; proposalId?: string; proposalNumber?: string; clientId: string; clientName: string; title: string; version: number; status: string; generatedById?: string; generatedByName: string; clientVisible: boolean; generatedAt: string; }
interface GeneratedDetail extends GeneratedSummary { previousVersionId?: string; content: string; createdAt: string; updatedAt: string; }
interface VariableItem { group: string; label: string; placeholder: string; description: string; }
interface Preview { templateId: string; title: string; content: string; unresolvedVariables: string[]; projectId?: string; projectName?: string; proposalId?: string; proposalNumber?: string; clientId: string; clientName: string; }
interface OptionEntity { id: string; displayName?: string; name?: string; code?: string; number?: string; title?: string; clientName?: string; }

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, LucideAngularModule, ArqlySelectComponent, MarkdownPreviewComponent, MarkdownEditorComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Gestão de documentos</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Documentos</h2>
          <p class="mt-2 text-slate-500">Crie modelos reutilizáveis e acompanhe os documentos gerados pelo escritório.</p>
        </div>
        <button class="btn-primary" type="button" (click)="view() === 'templates' ? openTemplate() : openGeneration()">
          <lucide-icon name="Plus" size="18" />{{ view() === 'templates' ? 'Novo modelo' : 'Gerar documento' }}
        </button>
      </div>

      <div class="card overflow-hidden">
        <div class="flex flex-col gap-3 border-b border-slate-200 p-4 md:flex-row md:items-center md:justify-between">
          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-2">
            <a class="rounded-xl px-4 py-3 text-center text-sm font-bold transition" routerLink="/app/documents/generated"
              [class.bg-white]="view() === 'generated'" [class.text-arqly-700]="view() === 'generated'" [class.shadow-sm]="view() === 'generated'" [class.text-slate-500]="view() !== 'generated'">Documentos gerados</a>
            <a class="rounded-xl px-4 py-3 text-center text-sm font-bold transition" routerLink="/app/documents/templates"
              [class.bg-white]="view() === 'templates'" [class.text-arqly-700]="view() === 'templates'" [class.shadow-sm]="view() === 'templates'" [class.text-slate-500]="view() !== 'templates'">Modelos</a>
          </div>
        </div>

        <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-[minmax(0,1fr)_230px_180px_auto]" [formGroup]="filterForm" (ngSubmit)="reload()">
          <input class="field" formControlName="search" [placeholder]="view() === 'templates' ? 'Nome ou descrição' : 'Título do documento'">
          <app-arqly-select formControlName="category" placeholder="Categoria" [options]="categoryOptions(true)" panelMode="fixed" />
          @if (view() === 'templates') {
            <app-arqly-select formControlName="active" placeholder="Status" [options]="templateStatusOptions" panelMode="fixed" />
          } @else {
            <app-arqly-select formControlName="status" placeholder="Status" [options]="generatedStatusOptions(true)" panelMode="fixed" />
          }
          <button class="btn-secondary h-12 justify-center px-4" type="submit"><lucide-icon name="Search" size="18" />Pesquisar</button>
        </form>

        @if (view() === 'templates') {
          <div class="overflow-x-auto">
          <table class="w-full min-w-[900px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-6 py-4">Modelo</th><th class="px-6 py-4">Categoria</th><th class="px-6 py-4">Versão</th><th class="px-6 py-4">Status</th><th class="px-6 py-4">Atualizado</th><th class="px-6 py-4 text-right">Ações</th></tr></thead>
            <tbody>
              @for (template of templates(); track template.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-6 py-4"><p class="font-bold">{{ template.name }}</p><p class="mt-1 max-w-md truncate text-xs text-slate-500">{{ template.description || 'Sem descrição' }}</p></td>
                  <td class="px-6 py-4"><span class="rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ categoryLabel(template.category) }}</span></td>
                  <td class="px-6 py-4 font-bold">v{{ template.version }}</td>
                  <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class.bg-arqly-50]="template.active && !template.archived" [class.text-arqly-700]="template.active && !template.archived" [class.bg-slate-100]="!template.active || template.archived" [class.text-slate-500]="!template.active || template.archived">{{ template.archived ? 'Arquivado' : template.active ? 'Ativo' : 'Inativo' }}</span></td>
                  <td class="px-6 py-4 text-slate-500">{{ template.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td class="px-6 py-4"><div class="flex justify-end gap-2"><button class="btn-secondary px-3 py-2" title="Editar" type="button" (click)="editTemplate(template)"><lucide-icon name="Pencil" size="15" /></button><button class="btn-secondary px-3 py-2" title="Nova versão" type="button" (click)="newTemplateVersion(template)"><lucide-icon name="FileCheck2" size="15" /></button><button class="btn-secondary px-3 py-2" title="Duplicar" type="button" (click)="duplicateTemplate(template)"><lucide-icon name="Copy" size="15" /></button><button class="btn-secondary px-3 py-2" title="Arquivar" type="button" (click)="askConfirm('archive-template', template.id, template.name)"><lucide-icon name="Folder" size="15" /></button><button class="btn-secondary px-3 py-2 text-red-600" title="Excluir" type="button" (click)="askConfirm('delete-template', template.id, template.name)"><lucide-icon name="Trash2" size="15" /></button></div></td>
                </tr>
              } @empty { <tr><td colspan="6" class="px-6 py-12 text-center text-slate-500">Nenhum modelo encontrado.</td></tr> }
            </tbody>
          </table>
          </div>
        } @else {
          <div class="overflow-x-auto">
          <table class="w-full min-w-[1040px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-6 py-4">Tipo</th><th class="px-6 py-4">Documento</th><th class="px-6 py-4">Origem</th><th class="px-6 py-4">Versão</th><th class="px-6 py-4">Data</th><th class="px-6 py-4">Autor</th><th class="px-6 py-4">Status</th><th class="px-6 py-4 text-right">Ações</th></tr></thead>
            <tbody>
              @for (document of documents(); track document.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-6 py-4"><span class="rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ categoryLabel(document.category) }}</span></td>
                  <td class="px-6 py-4"><p class="font-bold">{{ document.title }}</p><p class="mt-1 text-xs text-slate-500">{{ document.templateName }}</p></td>
                  <td class="px-6 py-4"><p class="font-bold">{{ document.projectName || document.proposalNumber || document.clientName }}</p><p class="mt-1 text-xs text-slate-400">{{ document.projectName ? 'Projeto' : document.proposalNumber ? 'Proposta' : 'Cliente' }}</p></td>
                  <td class="px-6 py-4 font-bold">v{{ document.version }}</td><td class="px-6 py-4 text-slate-500">{{ document.generatedAt | date:'dd/MM/yyyy HH:mm' }}</td><td class="px-6 py-4 text-slate-600">{{ document.generatedByName }}</td><td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="generatedStatusClass(document.status)">{{ generatedStatusLabel(document.status) }}</span></td>
                  <td class="px-6 py-4"><div class="flex justify-end gap-2"><button class="btn-secondary px-3 py-2" title="Visualizar" type="button" (click)="viewDocument(document)"><lucide-icon name="Search" size="15" />Ver</button><a class="btn-secondary px-3 py-2" title="Arquivos vinculados" [routerLink]="['/app/files', 'DOCUMENT', document.id]"><lucide-icon name="Archive" size="15" /></a><button class="btn-secondary px-3 py-2" title="Exportar PDF" type="button" (click)="downloadPdf(document)"><lucide-icon name="FileText" size="15" /></button><button class="btn-secondary px-3 py-2" [title]="document.clientVisible ? 'Ocultar do portal' : 'Publicar no portal'" type="button" (click)="togglePortalDocument(document)"><lucide-icon [name]="document.clientVisible ? 'Eye' : 'Send'" size="15" /></button><button class="btn-secondary px-3 py-2" title="Nova versão" type="button" (click)="newDocumentVersion(document)"><lucide-icon name="FileCheck2" size="15" /></button><button class="btn-secondary px-3 py-2" title="Duplicar" type="button" (click)="duplicateDocument(document)"><lucide-icon name="Copy" size="15" /></button><button class="btn-secondary px-3 py-2" title="Arquivar" type="button" (click)="askConfirm('archive-document', document.id, document.title)"><lucide-icon name="Folder" size="15" /></button></div></td>
                </tr>
              } @empty { <tr><td colspan="8" class="px-6 py-12 text-center text-slate-500">Nenhum documento gerado.</td></tr> }
            </tbody>
          </table>
          </div>
        }

        <div class="flex flex-col gap-3 border-t border-slate-200 p-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <span>{{ totalElements() }} {{ view() === 'templates' ? 'modelo(s)' : 'documento(s)' }} encontrado(s)</span>
          <div class="flex items-center gap-2">
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() === 0" (click)="changePage(-1)">Anterior</button>
            <span class="font-bold text-slate-700">Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span>
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() + 1 >= totalPages()" (click)="changePage(1)">Próxima</button>
          </div>
        </div>
      </div>
    </section>

    @if (templateModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card flex max-h-[94vh] w-full max-w-6xl flex-col overflow-hidden" [formGroup]="templateForm" (ngSubmit)="saveTemplate()">
          <div class="flex items-center justify-between border-b border-slate-200 px-6 py-5"><div><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Modelo de documento</p><h3 class="mt-1 text-xl font-extrabold">{{ templateModalTitle() }}</h3></div><button class="btn-secondary px-3 py-2" type="button" (click)="closeTemplateModal()"><lucide-icon name="X" size="18" /></button></div>
          <div class="grid min-h-0 flex-1 overflow-hidden lg:grid-cols-[1fr_320px]">
            <div class="space-y-4 overflow-y-auto p-6">
              <div class="grid gap-4 md:grid-cols-2"><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Contrato de prestação de serviços"></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Categoria <span class="text-red-500">*</span></span><app-arqly-select formControlName="category" placeholder="Selecione" [options]="categoryOptions(false)" panelMode="fixed" /></label></div>
              <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Descrição</span><input class="field" formControlName="description" placeholder="Explique quando este modelo deve ser utilizado"></label>
              <label class="flex items-center gap-3"><input class="checkbox" type="checkbox" formControlName="active"><span class="text-sm font-bold">Modelo ativo e disponível para geração</span></label>
              <label class="block space-y-1">
                <span class="text-xs font-bold text-slate-500">Conteúdo <span class="text-red-500">*</span></span>
                <app-markdown-editor #templateEditor formControlName="content" [minHeight]="420" [placeholder]="templateContentPlaceholder" />
              </label>
            </div>
            <aside class="overflow-y-auto border-l border-slate-200 bg-slate-50/70 p-5"><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Placeholders</p><h4 class="mt-1 font-extrabold">Variáveis disponíveis</h4><input class="field mt-4" [value]="variableSearch()" (input)="variableSearch.set($any($event.target).value)" placeholder="Pesquisar variável"><div class="mt-5 space-y-5">@for (group of variableGroups(); track group.name) {<div><p class="mb-2 text-xs font-extrabold uppercase tracking-[0.14em] text-slate-400">{{ group.name }}</p><div class="space-y-2">@for (variable of group.items; track variable.placeholder) {<button class="w-full rounded-xl border border-slate-200 bg-white p-3 text-left transition hover:border-arqly-300 hover:bg-arqly-50" type="button" (mousedown)="$event.preventDefault()" (click)="insertVariable(variable.placeholder)"><strong class="block text-sm">{{ variable.label }}</strong><code class="mt-1 block text-xs text-arqly-700">{{ variable.placeholder }}</code><span class="mt-1 block text-xs text-slate-400">{{ variable.description }}</span></button>}</div></div>}</div></aside>
          </div>
          <div class="flex justify-end gap-3 border-t border-slate-200 px-6 py-4"><button class="btn-secondary" type="button" (click)="closeTemplateModal()">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="17" />Salvar</button></div>
        </form>
      </div>
    }

    @if (generationModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <section class="modal-panel card flex max-h-[94vh] w-full max-w-6xl flex-col overflow-hidden">
          <div class="flex items-center justify-between border-b border-slate-200 px-6 py-5"><div><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Gerar documento</p><h3 class="mt-1 text-xl font-extrabold">{{ generationStepTitle() }}</h3></div><button class="btn-secondary px-3 py-2" type="button" (click)="closeGeneration()"><lucide-icon name="X" size="18" /></button></div>
          <div class="grid grid-cols-3 gap-2 border-b border-slate-200 bg-slate-50/70 p-3">@for (step of [1,2,3]; track step) {<div class="rounded-xl px-4 py-3 text-center text-xs font-extrabold" [class.bg-white]="generationStep() === step" [class.text-arqly-700]="generationStep() === step" [class.shadow-sm]="generationStep() === step" [class.text-slate-400]="generationStep() !== step">{{ step }}. {{ step === 1 ? 'Origem' : step === 2 ? 'Revisão' : 'Confirmação' }}</div>}</div>
          <div class="min-h-0 flex-1 overflow-y-auto p-6">
            @if (generationStep() === 1) {
              <form class="mx-auto max-w-3xl space-y-5" [formGroup]="generationForm">
                <div class="grid gap-4 md:grid-cols-2"><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Gerar a partir de <span class="text-red-500">*</span></span><app-arqly-select formControlName="sourceType" placeholder="Selecione" [options]="sourceTypeOptions" panelMode="fixed" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">{{ sourceLabel() }} <span class="text-red-500">*</span></span><app-arqly-select formControlName="sourceId" [placeholder]="'Selecione ' + sourceLabel().toLowerCase()" [options]="sourceOptions()" panelMode="fixed" /></label></div>
                <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Modelo <span class="text-red-500">*</span></span><app-arqly-select formControlName="templateId" placeholder="Selecione um modelo" [options]="templateOptions()" panelMode="fixed" /></label>
                <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Título</span><input class="field" formControlName="title" placeholder="Será sugerido a partir do modelo"></label>
                <div class="rounded-2xl border border-arqly-100 bg-arqly-50 p-4 text-sm text-arqly-800"><strong>Pré-visualização segura</strong><p class="mt-1 leading-6">As variáveis serão resolvidas agora. O documento só será salvo após sua confirmação.</p></div>
              </form>
            }
            @if (generationStep() === 2) {
              <div class="grid gap-5 lg:grid-cols-2"><div><label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Título <span class="text-red-500">*</span></span><input class="field" [formControl]="generationForm.controls.title" placeholder="Título final"></label><label class="mt-4 block space-y-1"><span class="text-xs font-bold text-slate-500">Conteúdo final <span class="text-red-500">*</span></span><textarea class="field min-h-[460px] w-full font-mono text-sm leading-6" [formControl]="generationForm.controls.content"></textarea></label></div><div><p class="mb-1 text-xs font-bold text-slate-500">Pré-visualização</p><div class="min-h-[520px] rounded-2xl border border-slate-200 bg-white p-8"><app-markdown-preview [content]="generationForm.controls.content.value" /></div></div></div>
            }
            @if (generationStep() === 3) {
              <div class="mx-auto grid max-w-4xl gap-5 lg:grid-cols-[0.7fr_1.3fr]"><div class="space-y-4"><div class="card bg-slate-50/70 p-5"><p class="text-xs font-bold uppercase tracking-[0.16em] text-slate-400">Documento</p><h4 class="mt-2 text-xl font-extrabold">{{ generationForm.controls.title.value }}</h4><div class="mt-4 space-y-2 text-sm"><p><strong>Cliente:</strong> {{ preview()?.clientName }}</p><p><strong>Projeto:</strong> {{ preview()?.projectName || '-' }}</p><p><strong>Proposta:</strong> {{ preview()?.proposalNumber || '-' }}</p></div></div><label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Status inicial</span><app-arqly-select [formControl]="generationForm.controls.status" placeholder="Selecione" [options]="generatedStatusOptions(false)" panelMode="fixed" /></label></div><div class="rounded-2xl border border-slate-200 bg-white p-8"><app-markdown-preview [content]="generationForm.controls.content.value" /></div></div>
            }
          </div>
          <div class="flex justify-between border-t border-slate-200 px-6 py-4"><button class="btn-secondary" type="button" [disabled]="generationStep() === 1" (click)="generationStep.set(generationStep() - 1)"><lucide-icon name="ChevronLeft" size="17" />Voltar</button>@if (generationStep() < 3) {<button class="btn-primary" type="button" (click)="nextGenerationStep()">Continuar<lucide-icon name="ChevronRight" size="17" /></button>} @else {<button class="btn-primary" type="button" (click)="generateDocument()"><lucide-icon name="FileCheck2" size="17" />Gerar documento</button>}</div>
        </section>
      </div>
    }

    @if (documentModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4"><section class="modal-panel card flex max-h-[94vh] w-full max-w-5xl flex-col overflow-hidden"><div class="flex items-center justify-between border-b border-slate-200 px-6 py-5"><div><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">{{ editingDocumentVersion() ? 'Nova versão' : 'Documento gerado' }}</p><h3 class="mt-1 text-xl font-extrabold">{{ selectedDocument()?.title }}</h3></div><button class="btn-secondary px-3 py-2" type="button" (click)="closeDocumentModal()"><lucide-icon name="X" size="18" /></button></div><div class="min-h-0 flex-1 overflow-y-auto p-6">@if (editingDocumentVersion()) {<form class="space-y-4" [formGroup]="versionForm"><label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Título <span class="text-red-500">*</span></span><input class="field" formControlName="title"></label><div class="grid gap-5 lg:grid-cols-2"><label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Conteúdo <span class="text-red-500">*</span></span><textarea class="field min-h-[500px] font-mono text-sm" formControlName="content"></textarea></label><div class="rounded-2xl border border-slate-200 p-7"><app-markdown-preview [content]="versionForm.controls.content.value" /></div></div></form>} @else {<div class="mb-5 flex flex-wrap gap-2">@for (version of documentVersions(); track version.id) {<button class="rounded-full px-3 py-1 text-xs font-bold" type="button" [class.bg-arqly-600]="selectedDocument()?.id === version.id" [class.text-white]="selectedDocument()?.id === version.id" [class.bg-slate-100]="selectedDocument()?.id !== version.id" (click)="selectedDocument.set(version)">v{{ version.version }}</button>}</div><div class="rounded-2xl border border-slate-200 bg-white p-8"><app-markdown-preview [content]="selectedDocument()?.content || ''" /></div>}</div><div class="flex justify-end gap-3 border-t border-slate-200 px-6 py-4"><button class="btn-secondary" type="button" (click)="closeDocumentModal()">Fechar</button>@if (!editingDocumentVersion() && selectedDocument()) {<button class="btn-primary" type="button" (click)="downloadPdf(selectedDocument()!)"><lucide-icon name="FileText" size="17" />Exportar PDF</button>}@if (editingDocumentVersion()) {<button class="btn-primary" type="button" (click)="saveDocumentVersion()"><lucide-icon name="Save" size="17" />Salvar nova versão</button>}</div></section></div>
    }

    @if (confirmOpen()) {
      <div class="modal-overlay fixed inset-0 z-[70] grid place-items-center p-4"><section class="modal-panel card w-full max-w-md p-6"><span class="grid h-12 w-12 place-items-center rounded-2xl bg-amber-50 text-amber-700"><lucide-icon name="Info" size="22" /></span><h3 class="mt-4 text-xl font-extrabold">Confirmar ação</h3><p class="mt-2 text-sm leading-6 text-slate-500">Deseja continuar com <strong>{{ confirmName() }}</strong>?</p><div class="mt-6 flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="confirmOpen.set(false)">Cancelar</button><button class="btn-primary" type="button" (click)="executeConfirm()">Confirmar</button></div></section></div>
    }
  `
})
export class DocumentsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = '/api/tenant/documents';
  readonly templateContentPlaceholder = '# Título do documento\n\nCliente: {{cliente.nome}}';
  @ViewChild('templateEditor') private templateEditor?: MarkdownEditorComponent;

  readonly view = signal<DocumentView>('templates');
  readonly templates = signal<TemplateSummary[]>([]);
  readonly documents = signal<GeneratedSummary[]>([]);
  readonly variables = signal<VariableItem[]>([]);
  readonly clients = signal<OptionEntity[]>([]);
  readonly projects = signal<OptionEntity[]>([]);
  readonly proposals = signal<OptionEntity[]>([]);
  readonly page = signal(0); readonly totalPages = signal(0); readonly totalElements = signal(0);
  readonly templateModalOpen = signal(false); readonly templateMode = signal<TemplateMode>('create'); readonly editingTemplateId = signal<string | null>(null);
  readonly variableSearch = signal('');
  readonly generationModalOpen = signal(false); readonly generationStep = signal(1); readonly preview = signal<Preview | null>(null);
  readonly documentModalOpen = signal(false); readonly selectedDocument = signal<GeneratedDetail | null>(null); readonly documentVersions = signal<GeneratedDetail[]>([]); readonly editingDocumentVersion = signal(false);
  readonly confirmOpen = signal(false); readonly confirmAction = signal(''); readonly confirmTarget = signal(''); readonly confirmName = signal('');

  readonly filterForm = this.fb.nonNullable.group({ search: [''], category: [''], active: [''], status: [''] });
  readonly templateForm = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''], category: ['CONTRACT', Validators.required], content: ['', Validators.required], active: [true] });
  readonly generationForm = this.fb.nonNullable.group({ sourceType: ['PROJECT', Validators.required], sourceId: ['', Validators.required], templateId: ['', Validators.required], title: [''], content: [''], status: ['GENERATED'] });
  readonly versionForm = this.fb.nonNullable.group({ title: ['', Validators.required], content: ['', Validators.required], status: ['GENERATED'] });
  readonly categories = ['CONTRACT','PROPOSAL','MEMORIAL','DECLARATION','RECEIPT','REPORT','CHECKLIST','OTHER'];
  readonly sourceTypeOptions = [{ label: 'Projeto', value: 'PROJECT' }, { label: 'Proposta', value: 'PROPOSAL' }, { label: 'Cliente', value: 'CLIENT' }];
  readonly templateStatusOptions = [{ label: 'Todos', value: '' }, { label: 'Ativos', value: 'true' }, { label: 'Inativos', value: 'false' }];
  readonly variableGroups = computed(() => {
    const term = this.variableSearch().toLowerCase();
    const filtered = this.variables().filter(item => !term || `${item.group} ${item.label} ${item.placeholder}`.toLowerCase().includes(term));
    return [...new Set(filtered.map(item => item.group))].map(name => ({ name, items: filtered.filter(item => item.group === name) }));
  });

  ngOnInit() {
    this.route.data.subscribe(data => { this.view.set((data['documentView'] || 'templates') as DocumentView); this.page.set(0); this.reload(); });
    this.loadSupportData();
    this.generationForm.controls.sourceType.valueChanges.subscribe(() => this.generationForm.controls.sourceId.setValue(''));
    this.route.queryParamMap.subscribe(params => {
      const projectId = params.get('projectId'); const proposalId = params.get('proposalId'); const clientId = params.get('clientId');
      if (projectId) this.generationForm.patchValue({ sourceType: 'PROJECT', sourceId: projectId });
      else if (proposalId) this.generationForm.patchValue({ sourceType: 'PROPOSAL', sourceId: proposalId });
      else if (clientId) this.generationForm.patchValue({ sourceType: 'CLIENT', sourceId: clientId });
      if (params.get('create') === 'true') this.openGeneration();
    });
  }

  reload() { this.view() === 'templates' ? this.loadTemplates() : this.loadDocuments(); }
  changePage(delta: number) { this.page.update(value => value + delta); this.reload(); }

  loadTemplates() {
    const raw = this.filterForm.getRawValue(); const params = new URLSearchParams({ page: String(this.page()), size: '12', sort: 'updatedAt,desc' });
    if (raw.search) params.set('search', raw.search); if (raw.category) params.set('category', raw.category); if (raw.active) params.set('active', raw.active);
    this.http.get<ApiResponse<Page<TemplateSummary>>>(`${this.baseUrl}/templates?${params}`).subscribe({ next: response => this.applyPage(response.data, this.templates), error: () => this.toast.error('Não foi possível carregar os modelos.') });
  }

  loadDocuments() {
    const raw = this.filterForm.getRawValue(); const params = new URLSearchParams({ page: String(this.page()), size: '12', sort: 'generatedAt,desc' });
    if (raw.search) params.set('search', raw.search); if (raw.category) params.set('category', raw.category); if (raw.status) params.set('status', raw.status);
    const query = this.route.snapshot.queryParamMap; ['projectId','proposalId','clientId'].forEach(key => { const value = query.get(key); if (value) params.set(key, value); });
    this.http.get<ApiResponse<Page<GeneratedSummary>>>(`${this.baseUrl}/generated?${params}`).subscribe({ next: response => this.applyPage(response.data, this.documents), error: () => this.toast.error('Não foi possível carregar os documentos.') });
  }

  private applyPage<T>(page: Page<T>, target: { set(value: T[]): void }) { target.set(page.content); this.totalPages.set(page.totalPages); this.totalElements.set(page.totalElements); }

  private loadSupportData() {
    this.http.get<ApiResponse<VariableItem[]>>(`${this.baseUrl}/variables`).subscribe(response => this.variables.set(response.data));
    this.http.get<ApiResponse<TemplateSummary[]>>(`${this.baseUrl}/templates/options`).subscribe(response => this.templateOptionsData.set(response.data));
    this.http.get<ApiResponse<Page<OptionEntity>>>('/api/tenant/clients?page=0&size=200&sort=createdAt,desc').subscribe({
      next: response => this.clients.set(response.data.content),
      error: () => this.toast.error('Não foi possível carregar os clientes.')
    });
    this.http.get<ApiResponse<Page<OptionEntity>>>('/api/tenant/projects?page=0&size=200&sort=name,asc').subscribe(response => this.projects.set(response.data.content));
    this.http.get<ApiResponse<Page<OptionEntity>>>('/api/tenant/proposals?page=0&size=200&sort=createdAt,desc').subscribe(response => this.proposals.set(response.data.content));
  }
  readonly templateOptionsData = signal<TemplateSummary[]>([]);

  openTemplate() { this.templateMode.set('create'); this.editingTemplateId.set(null); this.templateForm.reset({ name: '', description: '', category: 'CONTRACT', content: '# TÍTULO DO DOCUMENTO\n\nCliente: {{cliente.nome}}\n\nProjeto: {{projeto.nome}}\n\nData: {{dataAtual}}', active: true }); this.templateModalOpen.set(true); }
  editTemplate(template: TemplateSummary) { this.loadTemplateIntoModal(template.id, 'edit'); }
  newTemplateVersion(template: TemplateSummary) { this.loadTemplateIntoModal(template.id, 'version'); }
  private loadTemplateIntoModal(id: string, mode: TemplateMode) { this.http.get<ApiResponse<TemplateDetail>>(`${this.baseUrl}/templates/${id}`).subscribe({ next: response => { const value = response.data; this.templateMode.set(mode); this.editingTemplateId.set(value.id); this.templateForm.reset({ name: value.name, description: value.description || '', category: value.category, content: value.content, active: true }); this.templateModalOpen.set(true); } }); }
  closeTemplateModal() { this.templateModalOpen.set(false); }
  templateModalTitle() { return this.templateMode() === 'create' ? 'Novo modelo' : this.templateMode() === 'version' ? 'Nova versão do modelo' : 'Editar modelo'; }

  saveTemplate() {
    if (this.templateForm.invalid) { this.templateForm.markAllAsTouched(); this.toast.validation('Preencha nome, categoria e conteúdo.'); return; }
    const id = this.editingTemplateId(); const mode = this.templateMode(); const url = mode === 'create' ? `${this.baseUrl}/templates` : mode === 'version' ? `${this.baseUrl}/templates/${id}/versions` : `${this.baseUrl}/templates/${id}`;
    const request = mode === 'edit' ? this.http.put<ApiResponse<TemplateDetail>>(url, this.templateForm.getRawValue()) : this.http.post<ApiResponse<TemplateDetail>>(url, this.templateForm.getRawValue());
    request.subscribe({ next: () => { this.closeTemplateModal(); this.loadTemplates(); this.refreshTemplateOptions(); this.toast.success('Modelo salvo.'); }, error: () => this.toast.error('Não foi possível salvar o modelo.') });
  }
  duplicateTemplate(template: TemplateSummary) { this.http.post(`${this.baseUrl}/templates/${template.id}/duplicate`, {}).subscribe({ next: () => { this.loadTemplates(); this.refreshTemplateOptions(); this.toast.success('Modelo duplicado.'); }, error: () => this.toast.error('Não foi possível duplicar o modelo.') }); }
  private refreshTemplateOptions() { this.http.get<ApiResponse<TemplateSummary[]>>(`${this.baseUrl}/templates/options`).subscribe(response => this.templateOptionsData.set(response.data)); }
  insertVariable(value: string) {
    if (this.templateEditor) {
      this.templateEditor.insertText(value);
      return;
    }
    const control = this.templateForm.controls.content;
    control.setValue(`${control.value}${control.value ? '\n' : ''}${value}`);
  }

  openGeneration() { this.generationStep.set(1); this.preview.set(null); this.generationForm.patchValue({ templateId: '', title: '', content: '', status: 'GENERATED' }); this.generationModalOpen.set(true); }
  closeGeneration() { this.generationModalOpen.set(false); }
  generationStepTitle() { return this.generationStep() === 1 ? 'Escolha a origem e o modelo' : this.generationStep() === 2 ? 'Revise e edite o conteúdo' : 'Confirme a geração'; }
  nextGenerationStep() { if (this.generationStep() === 1) this.requestPreview(); else this.generationStep.update(step => step + 1); }
  requestPreview() {
    const raw = this.generationForm.getRawValue(); if (!raw.sourceId || !raw.templateId) { this.toast.validation('Selecione a origem e o modelo.'); return; }
    const payload: Record<string, string> = { templateId: raw.templateId, title: raw.title }; payload[this.sourceIdKey()] = raw.sourceId;
    this.http.post<ApiResponse<Preview>>(`${this.baseUrl}/preview`, payload).subscribe({ next: response => { this.preview.set(response.data); this.generationForm.patchValue({ title: response.data.title, content: response.data.content }); this.generationStep.set(2); if (response.data.unresolvedVariables.length) this.toast.validation('A prévia contém variáveis ainda não reconhecidas.'); }, error: () => this.toast.error('Não foi possível gerar a pré-visualização.') });
  }
  generateDocument() {
    const raw = this.generationForm.getRawValue(); if (!raw.title.trim() || !raw.content.trim()) { this.toast.validation('Título e conteúdo são obrigatórios.'); return; }
    const payload: Record<string, string> = { templateId: raw.templateId, title: raw.title, content: raw.content, status: raw.status }; payload[this.sourceIdKey()] = raw.sourceId;
    this.http.post(`${this.baseUrl}/generated`, payload).subscribe({ next: () => { this.closeGeneration(); this.loadDocuments(); this.toast.success('Documento gerado.'); }, error: () => this.toast.error('Não foi possível gerar o documento.') });
  }

  viewDocument(document: GeneratedSummary) { this.loadDocumentModal(document.id, false); }
  newDocumentVersion(document: GeneratedSummary) { this.loadDocumentModal(document.id, true); }
  private loadDocumentModal(id: string, editing: boolean) { this.http.get<ApiResponse<GeneratedDetail>>(`${this.baseUrl}/generated/${id}`).subscribe({ next: response => { this.selectedDocument.set(response.data); this.editingDocumentVersion.set(editing); this.versionForm.reset({ title: response.data.title, content: response.data.content, status: 'GENERATED' }); this.documentModalOpen.set(true); this.http.get<ApiResponse<GeneratedDetail[]>>(`${this.baseUrl}/generated/${id}/versions`).subscribe(history => this.documentVersions.set(history.data)); } }); }
  closeDocumentModal() { this.documentModalOpen.set(false); this.editingDocumentVersion.set(false); }
  saveDocumentVersion() { if (this.versionForm.invalid || !this.selectedDocument()) { this.toast.validation('Preencha título e conteúdo.'); return; } this.http.post(`${this.baseUrl}/generated/${this.selectedDocument()!.id}/versions`, this.versionForm.getRawValue()).subscribe({ next: () => { this.closeDocumentModal(); this.loadDocuments(); this.toast.success('Nova versão criada.'); }, error: () => this.toast.error('Não foi possível criar a versão.') }); }
  duplicateDocument(document: GeneratedSummary) { this.http.post(`${this.baseUrl}/generated/${document.id}/duplicate`, {}).subscribe({ next: () => { this.loadDocuments(); this.toast.success('Documento duplicado como rascunho.'); }, error: () => this.toast.error('Não foi possível duplicar o documento.') }); }
  togglePortalDocument(document: GeneratedSummary) {
    const action = document.clientVisible ? 'hide' : 'publish';
    this.http.patch(`${this.baseUrl}/generated/${document.id}/${action}`, {}).subscribe({
      next: () => { this.loadDocuments(); this.toast.success(document.clientVisible ? 'Documento ocultado do portal.' : 'Documento publicado no portal.'); },
      error: () => this.toast.error('Não foi possível atualizar a publicação do documento.')
    });
  }
  downloadPdf(document: GeneratedSummary) {
    this.http.get(`${this.baseUrl}/generated/${document.id}/pdf`, { responseType: 'blob' }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = window.document.createElement('a');
        anchor.href = url;
        anchor.download = `${document.title.replace(/[^a-zA-Z0-9À-ÿ._-]+/g, '-')}-v${document.version}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Não foi possível exportar o documento em PDF.')
    });
  }

  askConfirm(action: string, id: string, name: string) { this.confirmAction.set(action); this.confirmTarget.set(id); this.confirmName.set(name); this.confirmOpen.set(true); }
  executeConfirm() { const action = this.confirmAction(); const id = this.confirmTarget(); const url = action === 'archive-document' ? `${this.baseUrl}/generated/${id}/archive` : action === 'archive-template' ? `${this.baseUrl}/templates/${id}/archive` : `${this.baseUrl}/templates/${id}`; const request = action === 'delete-template' ? this.http.delete(url) : this.http.patch(url, {}); request.subscribe({ next: () => { this.confirmOpen.set(false); this.reload(); this.refreshTemplateOptions(); this.toast.success('Ação concluída.'); }, error: () => this.toast.error('Não foi possível concluir a ação.') }); }

  sourceOptions() { const type = this.generationForm.controls.sourceType.value as SourceType; if (type === 'PROJECT') return this.projects().map(item => ({ label: `${item.code} · ${item.name}`, value: item.id })); if (type === 'PROPOSAL') return this.proposals().map(item => ({ label: `${item.number} · ${item.title}`, value: item.id })); return this.clients().map(item => ({ label: item.displayName || item.name || 'Cliente', value: item.id })); }
  sourceLabel() { return ({ PROJECT: 'Projeto', PROPOSAL: 'Proposta', CLIENT: 'Cliente' } as Record<string,string>)[this.generationForm.controls.sourceType.value] || 'Origem'; }
  sourceIdKey() { return ({ PROJECT: 'projectId', PROPOSAL: 'proposalId', CLIENT: 'clientId' } as Record<string,string>)[this.generationForm.controls.sourceType.value]; }
  templateOptions() { return this.templateOptionsData().map(item => ({ label: `${item.name} · v${item.version}`, value: item.id })); }
  categoryOptions(includeAll: boolean) { const options = this.categories.map(value => ({ label: this.categoryLabel(value), value })); return includeAll ? [{ label: 'Todas', value: '' }, ...options] : options; }
  generatedStatusOptions(includeAll: boolean) { const options = [{ label: 'Rascunho', value: 'DRAFT' }, { label: 'Gerado', value: 'GENERATED' }, { label: 'Arquivado', value: 'ARCHIVED' }]; return includeAll ? [{ label: 'Todos', value: '' }, ...options] : options; }
  categoryLabel(value: string) { return ({ CONTRACT: 'Contrato', PROPOSAL: 'Proposta', MEMORIAL: 'Memorial', DECLARATION: 'Declaração', RECEIPT: 'Recibo', REPORT: 'Relatório', CHECKLIST: 'Checklist', OTHER: 'Outro' } as Record<string,string>)[value] || value; }
  generatedStatusLabel(value: string) { return ({ DRAFT: 'Rascunho', GENERATED: 'Gerado', ARCHIVED: 'Arquivado' } as Record<string,string>)[value] || value; }
  generatedStatusClass(value: string) {
    if (value === 'GENERATED') return 'bg-arqly-50 text-arqly-700';
    if (value === 'DRAFT') return 'bg-amber-50 text-amber-700';
    return 'bg-slate-100 text-slate-500';
  }
}
