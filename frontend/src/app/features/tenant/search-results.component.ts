import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { GlobalSearchResult, SearchResultType } from '../../shared/components/global-search.component';

interface SearchResponse { results: GlobalSearchResult[]; hasMore: boolean; }
const types: { value: SearchResultType | null; label: string }[] = [
  { value: null, label: 'Todos' }, { value: 'CLIENT', label: 'Clientes' }, { value: 'BRIEFING', label: 'Briefings' },
  { value: 'PROPOSAL', label: 'Propostas' }, { value: 'PROJECT', label: 'Projetos' }, { value: 'PROJECT_STAGE', label: 'Etapas' },
  { value: 'DOCUMENT', label: 'Documentos' }, { value: 'FILE', label: 'Arquivos' }
];

@Component({
  selector: 'app-search-results-page', standalone: true, imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <section class="space-y-5">
      <div><p class="text-xs font-extrabold uppercase tracking-[.2em] text-arqly-700">Busca global</p><h2 class="mt-1 text-3xl font-extrabold">Resultados de pesquisa</h2></div>
      <form class="relative max-w-3xl" (submit)="submit($event)"><lucide-icon name="Search" size="19" class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"></lucide-icon><input class="field pr-32 pl-11" [(ngModel)]="query" name="q" placeholder="Pesquisar clientes, projetos, propostas..."><button class="btn-primary absolute right-1 top-1/2 -translate-y-1/2 py-2" type="submit">Pesquisar</button></form>
      <nav class="flex flex-wrap gap-2" aria-label="Filtrar resultados">@for (item of filters; track item.label) { <button type="button" class="rounded-xl px-4 py-2 text-sm font-bold transition" [class.bg-arqly-600]="activeType() === item.value" [class.text-white]="activeType() === item.value" [class.bg-slate-100]="activeType() !== item.value" (click)="setType(item.value)">{{ item.label }}</button> }</nav>
      @if (loading()) { <div class="card space-y-3 p-6">@for (item of [1, 2, 3, 4]; track item) { <div class="h-16 animate-pulse rounded-xl bg-slate-100"></div> }</div>
      } @else if (failed()) { <div class="card p-12 text-center text-slate-500">Não foi possível realizar a pesquisa agora.</div>
      } @else if (searched() && !results().length) { <div class="card p-12 text-center text-slate-500">Nenhum resultado para “{{ query }}”.</div>
      } @else if (results().length) {
        <div class="card divide-y divide-slate-100 overflow-hidden">@for (item of results(); track item.type + item.id) { <button class="flex w-full items-center gap-4 p-5 text-left transition hover:bg-slate-50" (click)="open(item)"><span class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-arqly-50 text-arqly-700"><lucide-icon [name]="icon(item.type)" size="18"></lucide-icon></span><span class="min-w-0 flex-1"><span class="block truncate font-bold">{{ item.title }}</span><span class="mt-1 block truncate text-sm text-slate-500">{{ item.subtitle || item.description }}</span></span><span class="hidden rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-500 sm:inline">{{ label(item.type) }}</span><lucide-icon name="ChevronRight" size="18" class="shrink-0 text-slate-400"></lucide-icon></button> }</div>
        @if (hasMore()) { <button type="button" class="btn-secondary mx-auto" (click)="loadMore()">Carregar mais resultados</button> }
      }
    </section>
  `
})
export class SearchResultsPageComponent implements OnInit {
  private readonly http = inject(HttpClient); private readonly route = inject(ActivatedRoute); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef);
  readonly filters = types; query = ''; readonly activeType = signal<SearchResultType | null>(null); readonly results = signal<GlobalSearchResult[]>([]); readonly loading = signal(false); readonly searched = signal(false); readonly failed = signal(false); readonly hasMore = signal(false); private limit = 50;
  ngOnInit() { this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => { this.query = params['q'] || ''; this.activeType.set(params['type'] || null); this.limit = 50; this.load(); }); }
  submit(event: Event) { event.preventDefault(); void this.router.navigate([], { relativeTo: this.route, queryParams: { q: this.query, type: this.activeType() || null } }); }
  setType(type: SearchResultType | null) { void this.router.navigate([], { relativeTo: this.route, queryParams: { q: this.query, type: type || null } }); }
  loadMore() { this.limit = Math.min(this.limit + 50, 100); this.load(); }
  load() { if (this.query.trim().length < 2) { this.results.set([]); this.searched.set(false); this.hasMore.set(false); return; } this.loading.set(true); this.failed.set(false); this.searched.set(true); let params = new HttpParams().set('q', this.query).set('limit', String(this.limit)); if (this.activeType()) params = params.set('types', this.activeType()!); this.http.get<ApiResponse<SearchResponse>>('/api/search', { params }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: response => { this.results.set(response.data.results); this.hasMore.set(response.data.hasMore && this.limit < 100); this.loading.set(false); }, error: () => { this.results.set([]); this.hasMore.set(false); this.failed.set(true); this.loading.set(false); } }); }
  open(item: GlobalSearchResult) { void this.router.navigateByUrl(item.actionUrl); }
  label(type: SearchResultType) { return types.find(item => item.value === type)?.label || type; }
  icon(type: SearchResultType) { return ({ CLIENT: 'UserRound', BRIEFING: 'ClipboardList', PROPOSAL: 'FileText', PROJECT: 'FolderKanban', PROJECT_STAGE: 'ListTodo', DOCUMENT: 'FileText', FILE: 'File' } as Record<string, string>)[type]; }
}
