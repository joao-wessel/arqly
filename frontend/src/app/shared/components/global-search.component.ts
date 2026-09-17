import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, HostListener, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { ApiResponse } from '../../core/auth/auth.models';

export type SearchResultType = 'CLIENT' | 'BRIEFING' | 'PROPOSAL' | 'PROJECT' | 'PROJECT_STAGE' | 'DOCUMENT' | 'FILE';
export interface GlobalSearchResult { id: string; type: SearchResultType; title: string; subtitle?: string | null; description?: string | null; actionUrl: string; icon: string; metadata?: Record<string, string>; }
interface SearchResponse { results: GlobalSearchResult[]; hasMore: boolean; }

const labels: Record<SearchResultType, string> = { CLIENT: 'Clientes', BRIEFING: 'Briefings', PROPOSAL: 'Propostas', PROJECT: 'Projetos', PROJECT_STAGE: 'Etapas', DOCUMENT: 'Documentos', FILE: 'Arquivos' };
const icons: Record<SearchResultType, string> = { CLIENT: 'UserRound', BRIEFING: 'ClipboardList', PROPOSAL: 'FileText', PROJECT: 'FolderKanban', PROJECT_STAGE: 'ListTodo', DOCUMENT: 'FileText', FILE: 'File' };

@Component({
  selector: 'app-global-search', standalone: true, imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="relative w-full" (click)="$event.stopPropagation()">
      <div class="flex h-12 items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 text-slate-400 shadow-sm transition focus-within:border-arqly-300 focus-within:ring-4 focus-within:ring-arqly-50">
        <lucide-icon name="Search" size="19"></lucide-icon>
        <input #searchInput class="min-w-0 flex-1 bg-transparent text-sm text-slate-800 outline-none placeholder:text-slate-400" type="search" autocomplete="off" role="combobox" aria-autocomplete="list" aria-controls="global-search-results" [attr.aria-expanded]="open()" [attr.aria-activedescendant]="activeId()" [value]="query()" placeholder="Pesquisar clientes, projetos, propostas..." (focus)="show()" (input)="onInput($event)" (keydown)="onKeydown($event)">
        @if (loading()) { <span class="h-4 w-4 animate-spin rounded-full border-2 border-arqly-200 border-t-arqly-700"></span> } @else { <kbd class="hidden rounded border border-slate-200 px-1.5 py-0.5 text-[10px] font-bold text-slate-400 lg:inline">Ctrl K</kbd> }
      </div>
      @if (open()) {
        <div id="global-search-results" role="listbox" class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-50 max-h-[min(32rem,calc(100vh-6rem))] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-2xl">
          @if (query().trim().length < 2) { <div class="flex items-center gap-3 px-4 py-5 text-sm text-slate-500"><lucide-icon name="Search" size="18" class="text-arqly-700"></lucide-icon>Digite ao menos 2 caracteres para pesquisar no Arqly.</div>
          } @else if (loading()) { <div class="space-y-2 p-2">@for (item of [1,2,3]; track item) { <div class="h-14 animate-pulse rounded-xl bg-slate-100"></div> }</div>
          } @else if (failed()) { <div class="px-4 py-5 text-sm text-slate-500">Não foi possível pesquisar agora.</div>
          } @else if (!results().length) { <div class="px-4 py-5 text-sm text-slate-500">Nenhum resultado para “{{ query() }}”.</div>
          } @else {
            @for (group of groups(); track group.type) {
              <div class="px-3 pb-1 pt-3 text-[11px] font-extrabold uppercase tracking-[.16em] text-slate-400">{{ group.label }}</div>
              @for (item of group.items; track item.type + item.id; let index = $index) {
                <button type="button" role="option" [id]="optionId(item)" [attr.aria-selected]="flatIndex(item) === selectedIndex()" class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition" [class.bg-arqly-50]="flatIndex(item) === selectedIndex()" (mouseenter)="selectedIndex.set(flatIndex(item))" (click)="openResult(item)">
                  <span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-slate-100 text-arqly-700"><lucide-icon [name]="icon(item)" size="17"></lucide-icon></span>
                  <span class="min-w-0 flex-1"><span class="block truncate text-sm font-bold text-slate-800">{{ item.title }}</span><span *ngIf="item.subtitle" class="mt-0.5 block truncate text-xs text-slate-500">{{ item.subtitle }}</span></span><lucide-icon name="ArrowUpRight" size="16" class="shrink-0 text-slate-400"></lucide-icon>
                </button>
              }
            }
            @if (hasMore()) { <button type="button" class="mt-2 flex w-full items-center justify-center gap-2 border-t border-slate-100 px-3 py-3 text-sm font-bold text-arqly-700" (click)="viewAll()">Ver todos os resultados <lucide-icon name="ArrowRight" size="16"></lucide-icon></button> }
          }
        </div>
      }
    </div>
  `
})
export class GlobalSearchComponent implements OnInit {
  private readonly http = inject(HttpClient); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef); private readonly element = inject(ElementRef<HTMLElement>);
  @ViewChild('searchInput') input?: ElementRef<HTMLInputElement>;
  private readonly inputChanges = new Subject<string>();
  readonly query = signal(''); readonly results = signal<GlobalSearchResult[]>([]); readonly hasMore = signal(false); readonly loading = signal(false); readonly failed = signal(false); readonly open = signal(false); readonly selectedIndex = signal(-1);
  readonly groups = computed(() => Object.entries(labels).map(([type, label]) => ({ type: type as SearchResultType, label, items: this.results().filter(item => item.type === type) })).filter(group => group.items.length));
  readonly activeId = computed(() => this.selectedIndex() >= 0 ? this.optionId(this.results()[this.selectedIndex()]) : null);
  ngOnInit() { this.inputChanges.pipe(debounceTime(300), distinctUntilChanged(), switchMap(query => this.request(query)), takeUntilDestroyed(this.destroyRef)).subscribe(result => { this.loading.set(false); this.failed.set(result === null); if (result) { this.results.set(result.results); this.hasMore.set(result.hasMore); this.selectedIndex.set(result.results.length ? 0 : -1); } }); }
  onInput(event: Event) { const value = (event.target as HTMLInputElement).value; this.query.set(value); this.open.set(true); this.selectedIndex.set(-1); this.inputChanges.next(value); }
  show() { this.open.set(true); }
  onKeydown(event: KeyboardEvent) { const items = this.results(); if (event.key === 'Escape') { this.close(); return; } if (!items.length) return; if (event.key === 'ArrowDown' || event.key === 'ArrowUp') { event.preventDefault(); const next = event.key === 'ArrowDown' ? this.selectedIndex() + 1 : this.selectedIndex() - 1; this.selectedIndex.set((next + items.length) % items.length); } if (event.key === 'Enter' && this.selectedIndex() >= 0) { event.preventDefault(); this.openResult(items[this.selectedIndex()]); } }
  @HostListener('document:keydown', ['$event']) keyboardShortcut(event: KeyboardEvent) { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); this.open.set(true); queueMicrotask(() => { this.input?.nativeElement.focus(); this.input?.nativeElement.select(); }); } }
  @HostListener('document:click', ['$event']) outsideClick(event: MouseEvent) { if (!this.element.nativeElement.contains(event.target as Node)) this.close(); }
  close() { this.open.set(false); this.selectedIndex.set(-1); }
  openResult(item: GlobalSearchResult) { this.close(); void this.router.navigateByUrl(item.actionUrl); }
  viewAll() { this.close(); void this.router.navigate(['/app/search'], { queryParams: { q: this.query() } }); }
  icon(item: GlobalSearchResult) { return item.icon || icons[item.type]; }
  optionId(item?: GlobalSearchResult) { return item ? `search-option-${item.type}-${item.id}` : null; }
  flatIndex(item: GlobalSearchResult) { return this.results().findIndex(candidate => candidate.type === item.type && candidate.id === item.id); }
  private request(query: string) { if (query.trim().length < 2) { this.loading.set(false); this.failed.set(false); this.results.set([]); this.hasMore.set(false); return of({ results: [], hasMore: false } as SearchResponse); } this.loading.set(true); this.failed.set(false); return this.http.get<ApiResponse<SearchResponse>>('/api/search', { params: new HttpParams().set('q', query).set('limit', '12') }).pipe(switchMap(response => of(response.data)), catchError(() => of(null))); }
}
