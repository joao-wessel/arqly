import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule } from 'lucide-angular';
import { NgApexchartsModule } from 'ng-apexcharts';
import { Observable } from 'rxjs';
import {
  HomeActivity,
  HomeApiService,
  HomeAttentionItem,
  HomeFinancialSummary,
  HomeIndicators,
  HomeProject,
  HomeTodayItem
} from './home-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ApiResponse } from '../../core/auth/auth.models';

interface SectionState<T> {
  loading: boolean;
  data: T | null;
  failed: boolean;
}

const loading = <T>(): SectionState<T> => ({ loading: true, data: null, failed: false });

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, NgApexchartsModule],
  template: `
    <section class="space-y-5">
      <header class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[.2em] text-arqly-700">Visão do escritório</p>
          <h2 class="mt-1 text-3xl font-extrabold">{{ greeting() }}, {{ firstName() }}</h2>
          <p class="mt-2 text-slate-500">Aqui está o que precisa da sua atenção hoje.</p>
        </div>
        <button type="button" class="btn-secondary" (click)="open('/app/calendar')">
          <lucide-icon name="CalendarDays" size="18" /> Abrir agenda
        </button>
      </header>

      <section class="card overflow-hidden">
        <div class="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
          <div>
            <p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Prioridades</p>
            <h3 class="mt-1 text-xl font-extrabold">Requer atenção</h3>
          </div>
          <lucide-icon name="CircleAlert" size="21" class="text-amber-600" />
        </div>
        @if (attention().loading) {
          <div class="grid gap-3 p-5 md:grid-cols-2 xl:grid-cols-3">
            @for (item of skeletons; track item) { <div class="h-28 animate-pulse rounded-xl bg-slate-100"></div> }
          </div>
        } @else if (attention().failed) {
          <ng-container [ngTemplateOutlet]="errorState" [ngTemplateOutletContext]="{ retry: reloadAttention }"></ng-container>
        } @else if (!(attention().data?.length)) {
          <ng-container [ngTemplateOutlet]="emptyState" [ngTemplateOutletContext]="{ icon: 'CircleCheck', message: 'Nenhuma pendência importante.' }"></ng-container>
        } @else {
          <div class="grid gap-3 p-5 md:grid-cols-2 xl:grid-cols-3">
            @for (item of attention().data!.slice(0, 6); track item.type + item.sourceId) {
              <button type="button" class="group min-w-0 rounded-xl border border-slate-200 p-4 text-left transition hover:border-arqly-300 hover:bg-arqly-50/40" (click)="openAction(item.actionUrl)">
                <div class="flex gap-3">
                  <span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl" [class]="attentionTone(item.severity)">
                    <lucide-icon [name]="attentionIcon(item.type, item.severity)" size="18" />
                  </span>
                  <div class="min-w-0 flex-1">
                    <div class="flex items-start justify-between gap-2"><p class="line-clamp-1 font-bold">{{ item.title }}</p><lucide-icon *ngIf="item.actionUrl" name="ArrowUpRight" size="16" class="shrink-0 text-slate-400 group-hover:text-arqly-700" /></div>
                    <p *ngIf="item.description" class="mt-1 line-clamp-2 text-sm leading-5 text-slate-500">{{ item.description }}</p>
                    <div class="mt-3 flex flex-wrap gap-x-3 gap-y-1 text-xs font-semibold text-slate-500">
                      <span *ngIf="item.projectName">{{ item.projectName }}</span>
                      <span *ngIf="item.dueDate" [class.text-red-600]="item.severity === 'CRITICAL' || item.severity === 'HIGH'">{{ dueLabel(item.dueDate) }}</span>
                    </div>
                  </div>
                </div>
              </button>
            }
          </div>
        }
      </section>

      <div class="grid gap-5 xl:grid-cols-[minmax(0,.85fr)_minmax(0,1.15fr)]">
        <section class="card overflow-hidden">
          <div class="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
            <div><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Agenda</p><h3 class="mt-1 text-xl font-extrabold">Hoje</h3></div>
            <button type="button" class="text-sm font-bold text-arqly-700 hover:text-arqly-800" (click)="open('/app/calendar')">Abrir agenda</button>
          </div>
          @if (today().loading) {
            <div class="space-y-3 p-5">@for (item of skeletons.slice(0, 3); track item) { <div class="h-16 animate-pulse rounded-xl bg-slate-100"></div> }</div>
          } @else if (today().failed) {
            <ng-container [ngTemplateOutlet]="errorState" [ngTemplateOutletContext]="{ retry: reloadToday }"></ng-container>
          } @else if (!(today().data?.length)) {
            <ng-container [ngTemplateOutlet]="emptyState" [ngTemplateOutletContext]="{ icon: 'CalendarCheck2', message: 'Nenhum compromisso para hoje.' }"></ng-container>
          } @else {
            <div class="divide-y divide-slate-100">
              @for (item of today().data!.slice(0, 6); track item.id) {
                <button type="button" class="flex w-full items-center gap-3 px-5 py-4 text-left transition hover:bg-slate-50" (click)="openAction(item.sourceUrl)">
                  <span class="w-14 shrink-0 text-sm font-extrabold text-arqly-700">{{ item.allDay ? 'Dia todo' : time(item.start) }}</span>
                  <span class="h-9 w-1 shrink-0 rounded-full" [style.background]="item.color || '#0f766e'"></span>
                  <span class="min-w-0 flex-1"><span class="block truncate font-bold">{{ item.title }}</span><span class="mt-1 block truncate text-xs text-slate-500">{{ item.projectName || sourceLabel(item.sourceType) }}<ng-container *ngIf="item.responsibleUserName"> · {{ item.responsibleUserName }}</ng-container></span></span>
                  <lucide-icon *ngIf="item.sourceUrl" name="ChevronRight" size="18" class="shrink-0 text-slate-400" />
                </button>
              }
            </div>
          }
        </section>

        <section class="card overflow-hidden">
          <div class="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4"><div><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Responsabilidades</p><h3 class="mt-1 text-xl font-extrabold">Meus Projetos</h3></div><button type="button" class="text-sm font-bold text-arqly-700 hover:text-arqly-800" (click)="open('/app/projects')">Ver projetos</button></div>
          @if (projects().loading) {
            <div class="grid gap-3 p-5 md:grid-cols-2">@for (item of skeletons.slice(0, 4); track item) { <div class="h-40 animate-pulse rounded-xl bg-slate-100"></div> }</div>
          } @else if (projects().failed) {
            <ng-container [ngTemplateOutlet]="errorState" [ngTemplateOutletContext]="{ retry: reloadProjects }"></ng-container>
          } @else if (!(projects().data?.length)) {
            <ng-container [ngTemplateOutlet]="emptyState" [ngTemplateOutletContext]="{ icon: 'FolderOpen', message: 'Nenhum projeto relacionado a você.' }"></ng-container>
          } @else {
            <div class="grid gap-3 p-5 md:grid-cols-2">
              @for (project of projects().data!.slice(0, 6); track project.id) {
                <button type="button" class="rounded-xl border border-slate-200 p-4 text-left transition hover:border-arqly-300 hover:bg-arqly-50/40" (click)="open('/app/projects/' + project.id)">
                  <div class="flex items-start justify-between gap-3"><div class="min-w-0"><p class="text-xs font-bold text-slate-400">{{ project.code }}</p><p class="mt-1 truncate font-extrabold">{{ project.name }}</p><p class="mt-1 truncate text-sm text-slate-500">{{ project.clientName }}</p></div><span class="shrink-0 rounded-full px-2.5 py-1 text-xs font-bold" [class]="projectStatusTone(project.status)">{{ projectStatusLabel(project.status) }}</span></div>
                  <div class="mt-4"><div class="mb-1 flex justify-between text-xs font-semibold text-slate-500"><span>Progresso</span><span>{{ project.completionPercentage == null ? '—' : project.completionPercentage + '%' }}</span></div><div class="h-2 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600" [style.width.%]="project.completionPercentage || 0"></div></div></div>
                  <p class="mt-3 text-xs text-slate-500">Atualizado {{ relativeDate(project.updatedAt) }}</p>
                </button>
              }
            </div>
          }
        </section>
      </div>

      <section>
        <div class="mb-3"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Acompanhamento</p><h3 class="mt-1 text-xl font-extrabold">Indicadores</h3></div>
        @if (indicators().loading) {
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">@for (item of skeletons.slice(0, 4); track item) { <div class="h-28 animate-pulse rounded-2xl bg-slate-100"></div> }</div>
        } @else if (indicators().failed) {
          <section class="card"><ng-container [ngTemplateOutlet]="errorState" [ngTemplateOutletContext]="{ retry: reloadIndicators }"></ng-container></section>
        } @else if (indicators().data; as summary) {
          <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            @for (item of indicatorCards(summary); track item.label) {
              <button type="button" class="card p-5 text-left transition hover:border-arqly-300 hover:bg-arqly-50/30" (click)="open(item.url)">
                <div class="flex items-center justify-between"><span class="grid h-10 w-10 place-items-center rounded-xl" [class]="item.tone"><lucide-icon [name]="item.icon" size="19" /></span><lucide-icon name="ArrowUpRight" size="17" class="text-slate-400" /></div><p class="mt-4 text-3xl font-extrabold">{{ item.value }}</p><p class="mt-1 text-sm font-semibold text-slate-500">{{ item.label }}</p>
              </button>
            }
          </div>
        }
      </section>

      <div class="grid gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(0,.9fr)]">
        @if (financial().loading) {
          <section class="card p-5"><div class="h-5 w-40 animate-pulse rounded bg-slate-100"></div><div class="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">@for (item of skeletons.slice(0, 5); track item) { <div class="h-24 animate-pulse rounded-xl bg-slate-100"></div> }</div><div class="mt-4 h-48 animate-pulse rounded-xl bg-slate-100"></div></section>
        } @else if (financial().data; as summary) {
          <section class="card overflow-hidden">
            <div class="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4"><div><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Financeiro</p><h3 class="mt-1 text-xl font-extrabold">Resumo Financeiro</h3></div><button type="button" class="text-sm font-bold text-arqly-700 hover:text-arqly-800" (click)="open('/app/financial')">Abrir financeiro</button></div>
            <div class="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-3"><div class="rounded-xl bg-arqly-50 p-4"><p class="text-xs font-bold text-slate-500">A receber</p><p class="mt-2 text-xl font-extrabold text-arqly-800">{{ money(summary.expectedIncome) }}</p></div><div class="rounded-xl bg-slate-50 p-4"><p class="text-xs font-bold text-slate-500">Recebido</p><p class="mt-2 text-xl font-extrabold">{{ money(summary.actualIncome) }}</p></div><div class="rounded-xl bg-amber-50 p-4"><p class="text-xs font-bold text-slate-500">A pagar</p><p class="mt-2 text-xl font-extrabold">{{ money(summary.expectedExpense) }}</p></div><div class="rounded-xl bg-slate-50 p-4"><p class="text-xs font-bold text-slate-500">Pago</p><p class="mt-2 text-xl font-extrabold">{{ money(summary.actualExpense) }}</p></div><div class="rounded-xl bg-arqly-50 p-4 sm:col-span-2"><p class="text-xs font-bold text-slate-500">Saldo previsto</p><p class="mt-2 text-xl font-extrabold text-arqly-800">{{ money(summary.expectedBalance) }}</p><p class="mt-1 text-xs text-slate-500">Saldo realizado: {{ money(summary.actualBalance) }}</p></div></div>
            <div class="border-t border-slate-100 px-3 pt-3"><apx-chart [series]="financialSeries(summary)" [chart]="chartOptions.chart" [colors]="chartOptions.colors" [plotOptions]="chartOptions.plotOptions" [dataLabels]="chartOptions.dataLabels" [xaxis]="chartOptions.xaxis" [yaxis]="chartOptions.yaxis" [grid]="chartOptions.grid" [tooltip]="chartOptions.tooltip"></apx-chart></div>
          </section>
        }

        <section class="card overflow-hidden" [class.xl:col-span-2]="financial().failed">
          <div class="border-b border-slate-100 px-5 py-4"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Histórico</p><h3 class="mt-1 text-xl font-extrabold">Atividades Recentes</h3></div>
          @if (activities().loading) { <div class="space-y-3 p-5">@for (item of skeletons.slice(0, 4); track item) { <div class="h-14 animate-pulse rounded-xl bg-slate-100"></div> }</div>
          } @else if (activities().failed) { <ng-container [ngTemplateOutlet]="errorState" [ngTemplateOutletContext]="{ retry: reloadActivities }"></ng-container>
          } @else if (!(activities().data?.length)) { <ng-container [ngTemplateOutlet]="emptyState" [ngTemplateOutletContext]="{ icon: 'Activity', message: 'Nenhuma atividade recente.' }"></ng-container>
          } @else { <div class="divide-y divide-slate-100">@for (item of activities().data!.slice(0, 5); track item.id) { <button type="button" class="flex w-full gap-3 px-5 py-4 text-left transition hover:bg-slate-50" [disabled]="!item.projectId" (click)="openProject(item.projectId)"><span class="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-arqly-100 text-xs font-extrabold text-arqly-700">{{ initials(item.authorName) }}</span><span class="min-w-0 flex-1"><span class="block truncate text-sm"><strong>{{ item.authorName || 'Sistema' }}</strong> {{ item.title }}</span><span *ngIf="item.description" class="mt-1 block truncate text-xs text-slate-500">{{ item.description }}</span><span class="mt-1 block text-xs text-slate-400">{{ item.projectName || 'Escritório' }} · {{ relativeDate(item.createdAt) }}</span></span><lucide-icon *ngIf="item.projectId" name="ChevronRight" size="18" class="shrink-0 self-center text-slate-400" /></button> }</div> }
        </section>
      </div>
    </section>

    <ng-template #emptyState let-icon="icon" let-message="message"><div class="flex min-h-40 flex-col items-center justify-center gap-3 px-5 py-8 text-center"><span class="grid h-10 w-10 place-items-center rounded-xl bg-arqly-50 text-arqly-700"><lucide-icon [name]="icon" size="20" /></span><p class="text-sm font-medium text-slate-500">{{ message }}</p></div></ng-template>
    <ng-template #errorState let-retry="retry"><div class="flex min-h-40 flex-col items-center justify-center gap-3 px-5 py-8 text-center"><span class="grid h-10 w-10 place-items-center rounded-xl bg-red-50 text-red-600"><lucide-icon name="CircleAlert" size="20" /></span><p class="text-sm font-medium text-slate-500">Não foi possível carregar esta seção.</p><button type="button" class="btn-secondary text-sm" (click)="retry()">Tentar novamente</button></div></ng-template>
  `
})
export class HomePageComponent implements OnInit {
  private readonly api = inject(HomeApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);
  readonly skeletons = [1, 2, 3, 4, 5, 6];
  readonly attention = signal<SectionState<HomeAttentionItem[]>>(loading());
  readonly today = signal<SectionState<HomeTodayItem[]>>(loading());
  readonly projects = signal<SectionState<HomeProject[]>>(loading());
  readonly indicators = signal<SectionState<HomeIndicators>>(loading());
  readonly financial = signal<SectionState<HomeFinancialSummary>>(loading());
  readonly activities = signal<SectionState<HomeActivity[]>>(loading());

  readonly chartOptions = {
    chart: { type: 'bar' as const, height: 220, toolbar: { show: false } },
    colors: ['#0f766e', '#d97706'],
    plotOptions: { bar: { borderRadius: 6, columnWidth: '42%' } },
    dataLabels: { enabled: false },
    xaxis: { categories: ['Previsto', 'Realizado'], labels: { style: { colors: '#64748b' } } },
    yaxis: { labels: { formatter: (value: number) => this.money(value) } },
    grid: { borderColor: '#e2e8f0', strokeDashArray: 4 },
    tooltip: { y: { formatter: (value: number) => this.money(value) } }
  };

  ngOnInit(): void {
    this.reloadAttention();
    this.reloadToday();
    this.reloadProjects();
    this.reloadIndicators();
    this.reloadFinancial();
    this.reloadActivities();
  }

  readonly reloadAttention = () => this.load(this.attention, this.api.attention());
  readonly reloadToday = () => this.load(this.today, this.api.today());
  readonly reloadProjects = () => this.load(this.projects, this.api.projects());
  readonly reloadIndicators = () => this.load(this.indicators, this.api.indicators());
  readonly reloadActivities = () => this.load(this.activities, this.api.activities());
  readonly reloadFinancial = () => this.load(this.financial, this.api.financialSummary(), true);

  greeting(): string {
    const hour = new Date().getHours();
    return hour < 12 ? 'Bom dia' : hour < 18 ? 'Boa tarde' : 'Boa noite';
  }

  firstName(): string { return this.auth.currentUser()?.name?.split(' ')[0] || 'usuário'; }
  open(url: string): void { void this.router.navigateByUrl(url); }
  openAction(url?: string | null): void { if (url) this.open(url); }
  openProject(projectId?: string | null): void { if (projectId) this.open(`/app/projects/${projectId}`); }

  money(value?: number | null): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value || 0));
  }

  financialSeries(summary: HomeFinancialSummary) {
    return [
      { name: 'Entradas', data: [Number(summary.expectedIncome || 0), Number(summary.actualIncome || 0)] },
      { name: 'Saídas', data: [Number(summary.expectedExpense || 0), Number(summary.actualExpense || 0)] }
    ];
  }

  time(value: string): string { return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
  dueLabel(value: string): string { return `Vence ${new Intl.DateTimeFormat('pt-BR').format(new Date(`${value}T12:00:00`))}`; }
  relativeDate(value?: string | null): string {
    if (!value) return 'recentemente';
    const difference = Math.max(0, Date.now() - new Date(value).getTime());
    const minutes = Math.floor(difference / 60000);
    if (minutes < 1) return 'agora';
    if (minutes < 60) return `há ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `há ${hours} h`;
    const days = Math.floor(hours / 24);
    return days === 1 ? 'ontem' : `há ${days} dias`;
  }

  initials(name?: string | null): string { return (name || 'AR').split(' ').slice(0, 2).map(part => part[0]).join('').toUpperCase(); }
  sourceLabel(source?: string | null): string { return ({ PROJECT_STAGE: 'Etapa', APPROVAL: 'Aprovação', DIARY_VISIT: 'Diário de obra', MANUAL_EVENT: 'Compromisso' } as Record<string, string>)[source || ''] || 'Agenda'; }
  projectStatusLabel(status: string): string { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' } as Record<string, string>)[status] || status; }
  projectStatusTone(status: string): string { return ({ PLANNING: 'bg-slate-100 text-slate-600', IN_PROGRESS: 'bg-arqly-50 text-arqly-700', ON_HOLD: 'bg-amber-50 text-amber-700', COMPLETED: 'bg-emerald-50 text-emerald-700', CANCELLED: 'bg-red-50 text-red-700' } as Record<string, string>)[status] || 'bg-slate-100 text-slate-600'; }
  attentionTone(severity: string): string { return ({ CRITICAL: 'bg-red-50 text-red-600', HIGH: 'bg-amber-50 text-amber-700', NORMAL: 'bg-arqly-50 text-arqly-700', LOW: 'bg-slate-100 text-slate-600' } as Record<string, string>)[severity] || 'bg-slate-100 text-slate-600'; }
  attentionIcon(type: string, severity: string): string {
    if (severity === 'CRITICAL') return 'OctagonAlert';
    return ({ OVERDUE_STAGE: 'CalendarX2', PENDING_APPROVAL: 'CircleCheckBig', EXPIRING_APPROVAL: 'Clock3', CRITICAL_DIARY_OCCURRENCE: 'TriangleAlert', OVERDUE_RECEIVABLE: 'WalletCards', OVERDUE_PAYABLE: 'ReceiptText', STALE_PROJECT: 'FolderClock' } as Record<string, string>)[type] || 'CircleAlert';
  }

  indicatorCards(summary: HomeIndicators) {
    return [
      { label: 'Projetos ativos', value: summary.activeProjects, icon: 'FolderKanban', tone: 'bg-arqly-50 text-arqly-700', url: '/app/projects' },
      { label: 'Etapas em andamento', value: summary.inProgressStages, icon: 'ListTodo', tone: 'bg-sky-50 text-sky-700', url: '/app/projects' },
      { label: 'Etapas atrasadas', value: summary.overdueStages, icon: 'CalendarX2', tone: 'bg-red-50 text-red-700', url: '/app/projects' },
      { label: 'Aprovações pendentes', value: summary.pendingApprovals, icon: 'CircleCheckBig', tone: 'bg-amber-50 text-amber-700', url: '/app/projects' }
    ];
  }

  private load<T>(target: ReturnType<typeof signal<SectionState<T>>>, request: Observable<ApiResponse<T>>, hideOnFailure = false): void {
    target.set(loading<T>());
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => target.set({ loading: false, data: response.data, failed: false }),
      error: () => target.set({ loading: false, data: null, failed: !hideOnFailure })
    });
  }
}
