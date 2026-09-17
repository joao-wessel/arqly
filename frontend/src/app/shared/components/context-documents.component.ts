import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';

interface Page<T> { content: T[]; }
interface DocumentItem {
  id: string; title: string; templateName: string; category: string; version: number;
  status: string; generatedByName: string; generatedAt: string;
}

@Component({
  selector: 'app-context-documents',
  standalone: true,
  imports: [DatePipe, RouterLink, LucideAngularModule],
  template: `
    <section class="card overflow-hidden">
      <div class="flex flex-col gap-3 border-b border-slate-200 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div><h3 class="text-xl font-extrabold">Documentos</h3><p class="mt-1 text-sm text-slate-500">Documentos gerados e vinculados a este registro.</p></div>
        <a class="btn-primary" [routerLink]="['/app/documents/generated']" [queryParams]="queryParams()"><lucide-icon name="Plus" size="17" />Gerar documento</a>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full min-w-[760px] text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-5 py-4">Tipo</th><th class="px-5 py-4">Nome</th><th class="px-5 py-4">Versão</th><th class="px-5 py-4">Data</th><th class="px-5 py-4">Autor</th><th class="px-5 py-4">Status</th></tr></thead>
          <tbody>
            @for (document of documents(); track document.id) {
              <tr class="border-t border-slate-100"><td class="px-5 py-4 font-bold text-arqly-700">{{ categoryLabel(document.category) }}</td><td class="px-5 py-4"><strong>{{ document.title }}</strong><p class="mt-1 text-xs text-slate-400">{{ document.templateName }}</p></td><td class="px-5 py-4">v{{ document.version }}</td><td class="px-5 py-4">{{ document.generatedAt | date:'dd/MM/yyyy HH:mm' }}</td><td class="px-5 py-4">{{ document.generatedByName }}</td><td class="px-5 py-4"><span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold">{{ statusLabel(document.status) }}</span></td></tr>
            } @empty {
              <tr><td class="px-5 py-12 text-center text-slate-500" colspan="6">Nenhum documento vinculado.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `
})
export class ContextDocumentsComponent implements OnChanges {
  @Input() projectId = '';
  @Input() proposalId = '';
  @Input() clientId = '';
  private readonly http = inject(HttpClient);
  readonly documents = signal<DocumentItem[]>([]);

  ngOnChanges(changes: SimpleChanges) {
    if (changes['projectId'] || changes['proposalId'] || changes['clientId']) this.load();
  }

  queryParams() {
    return { projectId: this.projectId || null, proposalId: this.proposalId || null, clientId: this.clientId || null, create: 'true' };
  }

  private load() {
    const params = new URLSearchParams({ page: '0', size: '20', sort: 'generatedAt,desc' });
    if (this.projectId) params.set('projectId', this.projectId);
    if (this.proposalId) params.set('proposalId', this.proposalId);
    if (this.clientId) params.set('clientId', this.clientId);
    this.http.get<ApiResponse<Page<DocumentItem>>>(`/api/tenant/documents/generated?${params}`)
      .subscribe({ next: response => this.documents.set(response.data.content) });
  }

  categoryLabel(value: string) { return ({ CONTRACT: 'Contrato', PROPOSAL: 'Proposta', MEMORIAL: 'Memorial', DECLARATION: 'Declaração', RECEIPT: 'Recibo', REPORT: 'Relatório', CHECKLIST: 'Checklist', OTHER: 'Outro' } as Record<string, string>)[value] || value; }
  statusLabel(value: string) { return ({ DRAFT: 'Rascunho', GENERATED: 'Gerado', ARCHIVED: 'Arquivado' } as Record<string, string>)[value] || value; }
}
