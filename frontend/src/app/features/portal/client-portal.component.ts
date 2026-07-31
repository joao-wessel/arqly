import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { LogoComponent } from '../../shared/components/logo.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type PortalView = 'dashboard' | 'projects' | 'documents' | 'files' | 'approvals' | 'profile';
type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'WAITING_CLIENT' | 'WAITING_APPROVAL' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';

interface PortalClient { id: string; displayName: string; email: string; phone: string; city: string; state: string; }
interface PortalActivity { id: string; projectId?: string; stageId?: string; authorName: string; authorRole: string; type: string; title: string; description: string; createdAt: string; }
interface PortalDashboard { client: PortalClient; projectCount: number; activeProjects: number; completedProjects: number; pendingProposals: number; documentCount: number; pendingApprovals: number; lastUpdate: string | null; latestActivities: PortalActivity[]; }
interface PortalProjectSummary { id: string; code: string; name: string; status: ProjectStatus; progressPercentage: number; responsibleName: string; updatedAt: string; }
interface PortalStage { id: string; name: string; status: StageStatus; completionPercentage: number; responsibleName: string; plannedEnd: string | null; }
interface PortalPhase { id: string; name: string; color?: string; icon?: string; status: StageStatus; completionPercentage: number; stages: PortalStage[]; }
interface PortalDocument { id: string; title: string; category: string; version: number; status: string; generatedAt: string; }
interface PortalFile { id: string; name: string; extension: string; mimeType: string; size: number; version: number; previewAvailable: boolean; updatedAt: string; }
interface PortalApproval { id: string; projectId: string; projectName: string; stageName?: string; documentId?: string; documentTitle?: string; status: ApprovalStatus; description: string; deadline?: string; approvedAt?: string; clientComment?: string; createdAt: string; }
interface PortalProposal { id: string; number: string; title: string; status: string; total: number; validUntil: string | null; portalUrl: string; createdAt: string; }
interface PortalProjectDetail extends PortalProjectSummary { description: string; clientName: string; proposalNumber?: string; startDate?: string; expectedEndDate?: string; phases: PortalPhase[]; documents: PortalDocument[]; files: PortalFile[]; approvals: PortalApproval[]; activities: PortalActivity[]; }

@Component({
  selector: 'app-client-portal',
  standalone: true,
  imports: [DatePipe, FormsModule, LucideAngularModule, LogoComponent],
  template: `
    <main class="min-h-screen bg-[var(--surface-muted)] text-slate-900">
      @if (dashboard(); as data) {
        <div class="grid min-h-screen lg:grid-cols-[17rem_1fr]">
          <aside class="border-r border-slate-200 bg-white p-5">
            <app-logo />
            <div class="mt-8 rounded-2xl bg-arqly-50 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Portal do cliente</p>
              <h2 class="mt-2 font-extrabold">{{ data.client.displayName }}</h2>
              <p class="mt-1 text-sm text-slate-500">{{ data.client.email || 'E-mail não informado' }}</p>
            </div>
            <nav class="mt-6 space-y-2">
              @for (item of menu; track item.value) {
                <button type="button" class="flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-bold transition"
                        [class.bg-arqly-700]="view() === item.value"
                        [class.text-white]="view() === item.value"
                        [class.text-slate-600]="view() !== item.value"
                        [class.hover:bg-slate-50]="view() !== item.value"
                        (click)="setView(item.value)">
                  <lucide-icon [name]="item.icon" size="18"></lucide-icon>
                  {{ item.label }}
                </button>
              }
            </nav>
          </aside>

          <section class="min-w-0">
            <header class="sticky top-0 z-20 border-b border-slate-200 bg-white/90 px-5 py-4 backdrop-blur md:px-8">
              <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div>
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Arqly Portal</p>
                  <h1 class="mt-1 text-2xl font-extrabold">Olá, {{ firstName(data.client.displayName) }}.</h1>
                </div>
                <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-500">
                  Última atualização:
                  <strong class="text-slate-900">{{ data.lastUpdate ? (data.lastUpdate | date:'dd/MM/yyyy HH:mm') : 'Ainda sem atividades' }}</strong>
                </div>
              </div>
            </header>

            <div class="space-y-5 p-5 md:p-8">
              @if (view() === 'dashboard') {
                <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <article class="card p-5"><p class="text-sm font-bold text-slate-500">Projetos ativos</p><strong class="mt-3 block text-3xl text-arqly-700">{{ data.activeProjects }}</strong></article>
                  <article class="card p-5"><p class="text-sm font-bold text-slate-500">Projetos concluídos</p><strong class="mt-3 block text-3xl">{{ data.completedProjects }}</strong></article>
                  <article class="card p-5"><p class="text-sm font-bold text-slate-500">Propostas pendentes</p><strong class="mt-3 block text-3xl">{{ data.pendingProposals }}</strong></article>
                  <article class="card p-5"><p class="text-sm font-bold text-slate-500">Documentos</p><strong class="mt-3 block text-3xl">{{ data.documentCount }}</strong></article>
                </section>

                <section class="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
                  <div class="card p-5">
                    <div class="flex items-center justify-between">
                      <h2 class="text-xl font-extrabold">Meus projetos</h2>
                      <button class="btn-secondary px-3 py-2" type="button" (click)="setView('projects')">Ver todos</button>
                    </div>
                    <div class="mt-4 grid gap-3">
                      @for (project of projects().slice(0, 3); track project.id) {
                        <button class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 text-left transition hover:border-arqly-200 hover:bg-arqly-50"
                                type="button" (click)="openProject(project.id)">
                          <div class="flex items-start justify-between gap-4">
                            <div>
                              <p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">{{ project.code }}</p>
                              <h3 class="mt-1 font-extrabold">{{ project.name }}</h3>
                              <p class="mt-1 text-sm text-slate-500">Responsável: {{ project.responsibleName || 'Equipe' }}</p>
                            </div>
                            <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(project.status)">{{ statusLabel(project.status) }}</span>
                          </div>
                          <div class="mt-4 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-arqly-700" [style.width.%]="project.progressPercentage || 0"></div></div>
                        </button>
                      } @empty {
                        <p class="rounded-2xl border border-slate-200 bg-slate-50 p-5 text-sm text-slate-500">Nenhum projeto disponível no portal.</p>
                      }
                    </div>
                  </div>

                  <div class="card p-5">
                    <h2 class="text-xl font-extrabold">Últimas atividades</h2>
                    <div class="mt-5 space-y-4">
                      @for (activity of data.latestActivities; track activity.id) {
                        <div class="flex gap-3">
                          <span class="mt-1 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-arqly-50 text-arqly-700"><lucide-icon [name]="activityIcon(activity.type)" size="17"></lucide-icon></span>
                          <div>
                            <p class="text-sm font-bold">{{ activity.title }}</p>
                            <p class="mt-1 text-sm text-slate-500">{{ activity.description }}</p>
                            <p class="mt-1 text-xs font-bold text-slate-400">{{ activity.createdAt | date:'dd/MM/yyyy HH:mm' }}</p>
                          </div>
                        </div>
                      } @empty {
                        <p class="text-sm text-slate-500">Nenhuma atividade visível ao cliente ainda.</p>
                      }
                    </div>
                  </div>
                </section>
              }

              @if (view() === 'projects') {
                @if (selectedProject(); as project) {
                  <section class="space-y-5">
                    <button class="btn-secondary px-3 py-2" type="button" (click)="selectedProject.set(null)"><lucide-icon name="ArrowLeft" size="17"></lucide-icon>Voltar aos projetos</button>
                    <article class="card overflow-hidden">
                      <div class="border-b border-slate-200 bg-slate-50/70 p-6">
                        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                          <div>
                            <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">{{ project.code }}</p>
                            <h2 class="mt-2 text-3xl font-extrabold">{{ project.name }}</h2>
                            <p class="mt-2 max-w-3xl text-slate-500">{{ project.description || 'Projeto em acompanhamento pelo escritório.' }}</p>
                          </div>
                          <span class="rounded-full px-4 py-2 text-sm font-bold" [class]="statusClass(project.status)">{{ statusLabel(project.status) }}</span>
                        </div>
                      </div>
                      <div class="grid gap-5 p-6 xl:grid-cols-[1fr_22rem]">
                        <div class="space-y-5">
                          <div class="grid gap-4 md:grid-cols-3">
                            <div class="rounded-2xl border border-slate-200 p-4"><p class="text-xs font-bold text-slate-500">Responsável</p><strong class="mt-2 block">{{ project.responsibleName || 'Equipe' }}</strong></div>
                            <div class="rounded-2xl border border-slate-200 p-4"><p class="text-xs font-bold text-slate-500">Proposta</p><strong class="mt-2 block">{{ project.proposalNumber || 'Sem proposta vinculada' }}</strong></div>
                            <div class="rounded-2xl border border-slate-200 p-4"><p class="text-xs font-bold text-slate-500">Previsão</p><strong class="mt-2 block">{{ project.expectedEndDate ? (project.expectedEndDate | date:'dd/MM/yyyy') : '-' }}</strong></div>
                          </div>
                          <section>
                            <h3 class="text-lg font-extrabold">Etapas</h3>
                            <div class="mt-4 space-y-4">
                              @for (phase of project.phases; track phase.id) {
                                <div class="rounded-2xl border border-slate-200 bg-white p-4">
                                  <div class="flex items-center justify-between gap-3">
                                    <div class="flex items-center gap-3">
                                      <span class="h-3 w-3 rounded-full" [style.background]="phase.color || 'var(--arqly-600)'"></span>
                                      <strong>{{ phase.name }}</strong>
                                    </div>
                                    <span class="text-sm font-bold text-slate-500">{{ phase.completionPercentage || 0 }}%</span>
                                  </div>
                                  <div class="mt-4 space-y-3">
                                    @for (stage of phase.stages; track stage.id) {
                                      <div class="flex items-center gap-4 rounded-xl bg-slate-50 p-3">
                                        <span class="inline-flex h-8 w-8 items-center justify-center rounded-full" [class]="stage.status === 'COMPLETED' ? 'bg-arqly-700 text-white' : 'bg-white text-slate-400'">
                                          <lucide-icon [name]="stage.status === 'COMPLETED' ? 'Check' : 'Clock'" size="16"></lucide-icon>
                                        </span>
                                        <div class="min-w-0 flex-1">
                                          <p class="font-bold">{{ stage.name }}</p>
                                          <p class="text-xs text-slate-500">{{ stageStatusLabel(stage.status) }} · {{ stage.responsibleName || 'Equipe' }}</p>
                                        </div>
                                        <strong class="text-sm">{{ stage.completionPercentage || 0 }}%</strong>
                                      </div>
                                    }
                                  </div>
                                </div>
                              }
                            </div>
                          </section>
                        </div>
                        <aside class="space-y-4">
                          <div class="rounded-2xl border border-slate-200 p-5">
                            <div class="flex items-center justify-between"><h3 class="font-extrabold">Progresso</h3><strong class="text-arqly-700">{{ project.progressPercentage || 0 }}%</strong></div>
                            <div class="mt-4 h-3 rounded-full bg-slate-100"><div class="h-3 rounded-full bg-arqly-700" [style.width.%]="project.progressPercentage || 0"></div></div>
                          </div>
                          <div class="rounded-2xl border border-slate-200 p-5">
                            <h3 class="font-extrabold">Aprovações</h3>
                            <p class="mt-2 text-sm text-slate-500">{{ project.approvals.length }} solicitação(ões) neste projeto.</p>
                          </div>
                          <div class="rounded-2xl border border-slate-200 p-5">
                            <h3 class="font-extrabold">Arquivos e documentos</h3>
                            <p class="mt-2 text-sm text-slate-500">{{ project.files.length }} arquivos · {{ project.documents.length }} documentos publicados.</p>
                          </div>
                        </aside>
                      </div>
                    </article>
                  </section>
                } @else {
                  <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    @for (project of projects(); track project.id) {
                      <button class="card p-5 text-left transition hover:-translate-y-0.5 hover:border-arqly-200" type="button" (click)="openProject(project.id)">
                        <div class="flex items-start justify-between gap-3">
                          <span class="rounded-2xl bg-arqly-50 p-3 text-arqly-700"><lucide-icon name="Folder" size="21"></lucide-icon></span>
                          <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(project.status)">{{ statusLabel(project.status) }}</span>
                        </div>
                        <p class="mt-5 text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">{{ project.code }}</p>
                        <h2 class="mt-2 text-xl font-extrabold">{{ project.name }}</h2>
                        <p class="mt-2 text-sm text-slate-500">Responsável: {{ project.responsibleName || 'Equipe' }}</p>
                        <div class="mt-5 h-2 rounded-full bg-slate-100"><div class="h-2 rounded-full bg-arqly-700" [style.width.%]="project.progressPercentage || 0"></div></div>
                      </button>
                    }
                  </section>
                }
              }

              @if (view() === 'documents') {
                <section class="card overflow-hidden">
                  <div class="border-b border-slate-200 p-5"><h2 class="text-xl font-extrabold">Documentos publicados</h2></div>
                  <div class="divide-y divide-slate-100">
                    @for (document of documents(); track document.id) {
                      <div class="flex flex-col gap-3 p-5 md:flex-row md:items-center md:justify-between">
                        <div><p class="font-extrabold">{{ document.title }}</p><p class="text-sm text-slate-500">{{ categoryLabel(document.category) }} · v{{ document.version }} · {{ document.generatedAt | date:'dd/MM/yyyy' }}</p></div>
                        <a class="btn-secondary px-3 py-2" [href]="documentPdfUrl(document.id)"><lucide-icon name="Download" size="17"></lucide-icon>PDF</a>
                      </div>
                    } @empty {
                      <p class="p-5 text-sm text-slate-500">Nenhum documento publicado ainda.</p>
                    }
                  </div>
                </section>
              }

              @if (view() === 'files') {
                <section class="card overflow-hidden">
                  <div class="border-b border-slate-200 p-5"><h2 class="text-xl font-extrabold">Arquivos compartilhados</h2></div>
                  <div class="grid gap-4 p-5 md:grid-cols-2 xl:grid-cols-3">
                    @for (file of files(); track file.id) {
                      <article class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                        <div class="flex items-start gap-3">
                          <span class="rounded-xl bg-white p-3 text-arqly-700"><lucide-icon name="FileText" size="20"></lucide-icon></span>
                          <div class="min-w-0 flex-1">
                            <p class="truncate font-extrabold">{{ file.name }}</p>
                            <p class="mt-1 text-xs font-bold uppercase text-slate-400">{{ file.extension || 'arquivo' }} · v{{ file.version }}</p>
                          </div>
                        </div>
                        <div class="mt-4 flex gap-2">
                          @if (file.previewAvailable) {
                            <a class="btn-secondary flex-1 justify-center px-3 py-2" target="_blank" [href]="filePreviewUrl(file.id)"><lucide-icon name="Eye" size="17"></lucide-icon>Ver</a>
                          }
                          <a class="btn-primary flex-1 justify-center px-3 py-2" [href]="fileDownloadUrl(file.id)"><lucide-icon name="Download" size="17"></lucide-icon>Baixar</a>
                        </div>
                      </article>
                    } @empty {
                      <p class="text-sm text-slate-500">Nenhum arquivo compartilhado ainda.</p>
                    }
                  </div>
                </section>
              }

              @if (view() === 'approvals') {
                <section class="space-y-4">
                  @for (approval of approvals(); track approval.id) {
                    <article class="card p-5">
                      <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                        <div>
                          <span class="rounded-full px-3 py-1 text-xs font-bold" [class]="approvalClass(approval.status)">{{ approvalLabel(approval.status) }}</span>
                          <h2 class="mt-3 text-xl font-extrabold">{{ approval.description }}</h2>
                          <p class="mt-2 text-sm text-slate-500">{{ approval.projectName }} @if (approval.stageName) { · {{ approval.stageName }} } @if (approval.documentTitle) { · {{ approval.documentTitle }} }</p>
                          @if (approval.deadline) { <p class="mt-2 text-sm font-bold text-slate-500">Prazo: {{ approval.deadline | date:'dd/MM/yyyy' }}</p> }
                        </div>
                        @if (approval.status === 'PENDING') {
                          <div class="w-full space-y-3 md:w-80">
                            <textarea class="field min-h-24" placeholder="Comentário para aprovação ou ajustes" [(ngModel)]="approvalComment"></textarea>
                            <div class="grid grid-cols-2 gap-2">
                              <button class="btn-primary justify-center" type="button" (click)="decideApproval(approval.id, true)">Aprovar</button>
                              <button class="btn-secondary justify-center text-red-600" type="button" (click)="decideApproval(approval.id, false)">Ajustes</button>
                            </div>
                          </div>
                        }
                      </div>
                    </article>
                  } @empty {
                    <p class="card p-5 text-sm text-slate-500">Nenhuma aprovação pendente.</p>
                  }
                </section>
              }

              @if (view() === 'profile') {
                <section class="card max-w-2xl p-6">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Perfil</p>
                  <h2 class="mt-2 text-2xl font-extrabold">{{ data.client.displayName }}</h2>
                  <div class="mt-5 grid gap-4">
                    <label class="space-y-1"><span class="text-xs font-bold text-slate-500">E-mail</span><input class="field" [value]="data.client.email" disabled></label>
                    <label class="space-y-1"><span class="text-xs font-bold text-slate-500">Telefone</span><input class="field" [(ngModel)]="profilePhone"></label>
                    <p class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">Alteração de senha e foto ficarão vinculadas à autenticação dedicada do cliente na próxima evolução do portal.</p>
                  </div>
                </section>
              }
            </div>
          </section>
        </div>
      } @else if (loaded()) {
        <section class="grid min-h-screen place-items-center p-6">
          <div class="card max-w-md p-8 text-center">
            <app-logo />
            <h1 class="mt-8 text-3xl font-extrabold">Portal indisponível</h1>
            <p class="mt-2 text-slate-500">O link pode ter expirado ou sido revogado.</p>
          </div>
        </section>
      }
    </main>
  `
})
export class ClientPortalComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  readonly token = signal('');
  readonly loaded = signal(false);
  readonly view = signal<PortalView>('dashboard');
  readonly dashboard = signal<PortalDashboard | null>(null);
  readonly projects = signal<PortalProjectSummary[]>([]);
  readonly selectedProject = signal<PortalProjectDetail | null>(null);
  readonly proposals = signal<PortalProposal[]>([]);
  readonly documents = signal<PortalDocument[]>([]);
  readonly files = signal<PortalFile[]>([]);
  readonly approvals = signal<PortalApproval[]>([]);
  approvalComment = '';
  profilePhone = '';

  readonly menu: { label: string; value: PortalView; icon: string }[] = [
    { label: 'Dashboard', value: 'dashboard', icon: 'LayoutDashboard' },
    { label: 'Projetos', value: 'projects', icon: 'Folder' },
    { label: 'Documentos', value: 'documents', icon: 'FileText' },
    { label: 'Arquivos', value: 'files', icon: 'Archive' },
    { label: 'Aprovações', value: 'approvals', icon: 'CheckCircle2' },
    { label: 'Perfil', value: 'profile', icon: 'UserCog' }
  ];

  ngOnInit() {
    this.token.set(this.route.snapshot.paramMap.get('token') || '');
    this.loadAll();
  }

  loadAll() {
    this.http.get<ApiResponse<PortalDashboard>>(`${this.baseUrl()}/dashboard`).subscribe({
      next: (response) => {
        this.dashboard.set(response.data);
        this.profilePhone = response.data.client.phone || '';
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true)
    });
    this.http.get<ApiResponse<PortalProjectSummary[]>>(`${this.baseUrl()}/projects`).subscribe((response) => this.projects.set(response.data));
    this.http.get<ApiResponse<PortalProposal[]>>(`${this.baseUrl()}/proposal-list`).subscribe((response) => this.proposals.set(response.data));
    this.http.get<ApiResponse<PortalDocument[]>>(`${this.baseUrl()}/documents`).subscribe((response) => this.documents.set(response.data));
    this.http.get<ApiResponse<PortalFile[]>>(`${this.baseUrl()}/files`).subscribe((response) => this.files.set(response.data));
    this.http.get<ApiResponse<PortalApproval[]>>(`${this.baseUrl()}/approvals`).subscribe((response) => this.approvals.set(response.data));
  }

  setView(view: PortalView) {
    this.view.set(view);
    if (view !== 'projects') this.selectedProject.set(null);
  }

  openProject(id: string) {
    this.view.set('projects');
    this.http.get<ApiResponse<PortalProjectDetail>>(`${this.baseUrl()}/projects/${id}`)
      .subscribe((response) => this.selectedProject.set(response.data));
  }

  decideApproval(id: string, approved: boolean) {
    if (!approved && !this.approvalComment.trim()) {
      this.toast.validation('Informe o comentário para solicitar ajustes.');
      return;
    }
    const action = approved ? 'approve' : 'reject';
    this.http.post<ApiResponse<PortalApproval>>(`${this.baseUrl()}/approvals/${id}/${action}`, { comment: this.approvalComment })
      .subscribe({
        next: () => {
          this.toast.success(approved ? 'Aprovação registrada' : 'Ajustes solicitados');
          this.approvalComment = '';
          this.loadAll();
        },
        error: () => this.toast.error('Não foi possível registrar a decisão')
      });
  }

  baseUrl() { return `http://localhost:8080/api/portal/${this.token()}`; }
  fileDownloadUrl(id: string) { return `${this.baseUrl()}/files/${id}/download`; }
  filePreviewUrl(id: string) { return `${this.baseUrl()}/files/${id}/preview`; }
  documentPdfUrl(id: string) { return `${this.baseUrl()}/documents/${id}/pdf`; }
  firstName(name: string) { return (name || 'Cliente').split(' ')[0]; }
  activityIcon(type: string) { if (type.includes('FILE')) return 'Archive'; if (type.includes('APPROVAL')) return 'CheckCircle2'; if (type.includes('DOCUMENT')) return 'FileText'; return 'Activity'; }
  categoryLabel(category: string) { return ({ CONTRACT: 'Contrato', PROPOSAL: 'Proposta', MEMORIAL: 'Memorial', DECLARATION: 'Declaração', RECEIPT: 'Recibo', REPORT: 'Relatório', CHECKLIST: 'Checklist', OTHER: 'Outro' } as Record<string, string>)[category] || category; }
  statusLabel(status: ProjectStatus) { return ({ PLANNING: 'Planejamento', IN_PROGRESS: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' } as Record<string, string>)[status] || status; }
  stageStatusLabel(status: StageStatus) { return ({ NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING_CLIENT: 'Aguardando cliente', WAITING_APPROVAL: 'Aguardando aprovação', ON_HOLD: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' } as Record<string, string>)[status] || status; }
  approvalLabel(status: ApprovalStatus) { return ({ PENDING: 'Pendente', APPROVED: 'Aprovada', REJECTED: 'Ajustes solicitados', EXPIRED: 'Expirada', CANCELLED: 'Cancelada' } as Record<string, string>)[status] || status; }
  statusClass(status: ProjectStatus) { if (status === 'COMPLETED') return 'bg-arqly-50 text-arqly-700'; if (status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-700'; if (status === 'ON_HOLD') return 'bg-amber-50 text-amber-700'; if (status === 'CANCELLED') return 'bg-red-50 text-red-700'; return 'bg-slate-100 text-slate-600'; }
  approvalClass(status: ApprovalStatus) { if (status === 'APPROVED') return 'bg-arqly-50 text-arqly-700'; if (status === 'REJECTED') return 'bg-red-50 text-red-700'; if (status === 'PENDING') return 'bg-amber-50 text-amber-700'; return 'bg-slate-100 text-slate-600'; }
}
