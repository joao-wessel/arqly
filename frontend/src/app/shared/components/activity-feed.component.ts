import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from './toast/toast.service';
import { MarkdownPreviewComponent } from './markdown-preview.component';
import { MarkdownEditorComponent } from './markdown-editor.component';

type ActivityType = 'COMMENT' | 'STATUS_CHANGED' | 'CHECKLIST_UPDATED' | 'PROJECT_CREATED' | 'PROJECT_UPDATED' | 'PROJECT_COMPLETED' | 'PHASE_CREATED' | 'PHASE_UPDATED' | 'STAGE_CREATED' | 'STAGE_UPDATED' | 'STAGE_COMPLETED' | 'ASSIGNED' | 'UNASSIGNED' | 'FILE_UPLOADED' | 'FILE_VERSIONED' | 'FILE_DOWNLOADED' | 'FILE_ARCHIVED' | 'FILE_RESTORED' | 'FILE_MOVED' | 'FILE_RENAMED' | 'DOCUMENT_GENERATED' | 'CLIENT_APPROVAL' | 'VISIT_REGISTERED' | 'SYSTEM';
type ActivityFilter = 'ALL' | 'COMMENTS' | 'SYSTEM' | 'CHECKLIST' | 'STATUS' | 'FILES' | 'APPROVALS';

interface Page<T> { content: T[]; number: number; totalPages: number; totalElements: number; }
interface ActivityItem {
  id: string; projectId: string; phaseId?: string | null; stageId?: string | null; authorId?: string | null;
  authorName: string; authorRole: string; type: ActivityType; title: string; description: string; metadata?: string | null;
  edited: boolean; deleted: boolean; editedAt?: string | null; createdAt: string;
}

@Component({
  selector: 'app-activity-feed',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, LucideAngularModule, MarkdownPreviewComponent, MarkdownEditorComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">Central de atividades</p>
          <h3 class="mt-1 text-xl font-extrabold">Histórico unificado</h3>
          <p class="mt-1 text-sm text-slate-500">Comentários, checklist, status e eventos do sistema no mesmo feed.</p>
        </div>
        <form class="flex w-full flex-col gap-2 sm:flex-row lg:w-auto" [formGroup]="searchForm" (ngSubmit)="search()">
          <input class="field h-11 sm:w-72" formControlName="search" placeholder="Pesquisar comentários e eventos">
          <button class="btn-secondary h-11 justify-center px-4" type="submit"><lucide-icon name="Search" size="16"></lucide-icon>Pesquisar</button>
        </form>
      </div>

      <div class="flex gap-2 overflow-x-auto rounded-2xl bg-slate-50/70 p-2">
        @for (option of filterOptions; track option.value) {
          <button class="whitespace-nowrap rounded-xl px-4 py-2 text-sm font-bold transition" type="button"
            [class.bg-white]="filter() === option.value"
            [class.text-arqly-700]="filter() === option.value"
            [class.shadow-sm]="filter() === option.value"
            [class.text-slate-500]="filter() !== option.value"
            (click)="setFilter(option.value)">{{ option.label }}</button>
        }
      </div>

      <div class="relative space-y-0 pl-8 before:absolute before:bottom-6 before:left-[1.15rem] before:top-6 before:w-px before:bg-slate-200">
        @for (activity of activities(); track activity.id) {
          <article class="relative pb-6">
            <span class="absolute -left-8 top-1 z-10 grid h-9 w-9 place-items-center rounded-full border-4 border-white bg-arqly-50 text-arqly-700 shadow-sm">
              <lucide-icon [name]="activityIcon(activity.type)" size="16"></lucide-icon>
            </span>
            <div class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div class="flex gap-3">
                  <span class="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-slate-100 text-sm font-extrabold text-slate-600">{{ initials(activity.authorName) }}</span>
                  <div>
                    <p class="font-extrabold">{{ activity.authorName || 'Sistema' }}</p>
                    <p class="text-xs font-semibold text-slate-500">{{ activity.authorRole }}</p>
                    <p class="mt-0.5 text-xs font-semibold text-slate-400">{{ activity.createdAt | date:'dd/MM/yyyy HH:mm' }} @if (activity.edited) { · editado }</p>
                  </div>
                </div>
                <span class="w-fit rounded-full px-3 py-1 text-xs font-bold" [class]="typeClass(activity.type)">{{ typeLabel(activity.type) }}</span>
              </div>

              @if (editingId() === activity.id) {
                <form class="mt-4 space-y-3" [formGroup]="editCommentForm" (ngSubmit)="saveComment(activity)">
                  <app-markdown-editor formControlName="comment" [minHeight]="120" placeholder="Atualize o comentário" />
                  <div class="flex justify-end gap-2">
                    <button class="btn-secondary py-2" type="button" (click)="cancelEditing()">Cancelar</button>
                    <button class="btn-primary py-2" type="submit"><lucide-icon name="Save" size="15"></lucide-icon>Salvar</button>
                  </div>
                </form>
              } @else {
                <div class="mt-4 rounded-2xl bg-slate-50/70 p-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.16em] text-slate-400">{{ activity.title }}</p>
                  @if (activity.type === 'COMMENT' && !activity.deleted) {
                    <div class="mt-2 text-sm"><app-markdown-preview [content]="activity.description" /></div>
                  } @else {
                    <p class="mt-2 whitespace-pre-line text-sm leading-6" [class.italic]="activity.deleted" [class.text-slate-400]="activity.deleted" [class.text-slate-700]="!activity.deleted">{{ activityDescription(activity.description) }}</p>
                  }
                </div>
              }

              @if (canEdit(activity) && editingId() !== activity.id) {
                <div class="mt-3 flex justify-end gap-2">
                  <button class="btn-secondary px-3 py-2" type="button" (click)="beginEditing(activity)"><lucide-icon name="Pencil" size="15"></lucide-icon>Editar</button>
                  <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="deleteComment(activity)"><lucide-icon name="Trash2" size="15"></lucide-icon>Excluir</button>
                </div>
              }
            </div>
          </article>
        } @empty {
          <p class="rounded-2xl bg-slate-50/70 py-12 text-center text-sm text-slate-500">Nenhuma atividade encontrada.</p>
        }
      </div>

      @if (hasMore()) {
        <div #loadMoreSentinel class="py-3 text-center text-xs font-bold text-slate-400">
          {{ loading() ? 'Carregando atividades...' : 'Role para carregar mais' }}
        </div>
      }

      @if (allowComments) {
        <form class="card p-5" [formGroup]="commentForm" (ngSubmit)="addComment()">
          <div class="mb-3">
            <p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">Novo comentário</p>
            <p class="mt-1 text-sm text-slate-500">Registre decisões, dúvidas e detalhes técnicos da etapa.</p>
          </div>
          <div>
            <app-markdown-editor formControlName="comment" [minHeight]="150" placeholder="Registre uma decisão, dúvida ou detalhe técnico" />
          </div>
          <div class="mt-4 flex justify-end">
            <button class="btn-primary" type="submit"><lucide-icon name="Send" size="16"></lucide-icon>Comentar</button>
          </div>
        </form>
      }
    </section>
  `
})
export class ActivityFeedComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) projectId = '';
  @Input() phaseId = '';
  @Input() stageId = '';
  @Input() allowComments = true;

  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = '/api/tenant/activities';

  readonly activities = signal<ActivityItem[]>([]);
  readonly filter = signal<ActivityFilter>('ALL');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly editingId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly hasMore = computed(() => this.page() + 1 < this.totalPages());
  readonly searchForm = this.fb.nonNullable.group({ search: [''] });
  readonly commentForm = this.fb.nonNullable.group({ comment: ['', Validators.required] });
  readonly editCommentForm = this.fb.nonNullable.group({ comment: ['', Validators.required] });
  readonly filterOptions: { label: string; value: ActivityFilter }[] = [
    { label: 'Todos', value: 'ALL' },
    { label: 'Comentários', value: 'COMMENTS' },
    { label: 'Sistema', value: 'SYSTEM' },
    { label: 'Checklist', value: 'CHECKLIST' },
    { label: 'Status', value: 'STATUS' },
    { label: 'Arquivos', value: 'FILES' },
    { label: 'Aprovações', value: 'APPROVALS' }
  ];
  private loadMoreObserver?: IntersectionObserver;
  @ViewChild('loadMoreSentinel')
  set loadMoreSentinel(element: ElementRef<HTMLElement> | undefined) {
    this.loadMoreObserver?.disconnect();
    if (!element) return;
    this.loadMoreObserver = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting) this.loadMore();
    }, { rootMargin: '180px' });
    this.loadMoreObserver.observe(element.nativeElement);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['projectId'] || changes['stageId']) this.reload();
  }

  ngOnDestroy() {
    this.loadMoreObserver?.disconnect();
  }

  reload() {
    this.page.set(0);
    this.load(false);
  }

  search() { this.reload(); }

  setFilter(filter: ActivityFilter) {
    this.filter.set(filter);
    this.reload();
  }

  loadMore() {
    if (this.loading() || !this.hasMore()) return;
    this.page.update((page) => page + 1);
    this.load(true);
  }

  addComment() {
    if (this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      this.toast.validation('Escreva um comentário.');
      return;
    }
    const params = new URLSearchParams({ projectId: this.projectId });
    if (this.phaseId) params.set('phaseId', this.phaseId);
    if (this.stageId) params.set('stageId', this.stageId);
    this.http.post<ApiResponse<ActivityItem>>(`${this.baseUrl}/comments?${params}`, this.commentForm.getRawValue()).subscribe({
      next: () => { this.commentForm.reset(); this.reload(); this.toast.success('Comentário registrado.'); },
      error: () => this.toast.error('Não foi possível comentar.')
    });
  }

  beginEditing(activity: ActivityItem) {
    this.editCommentForm.reset({ comment: activity.description });
    this.editingId.set(activity.id);
  }

  cancelEditing() {
    this.editingId.set(null);
    this.editCommentForm.reset();
  }

  saveComment(activity: ActivityItem) {
    const comment = this.editCommentForm.controls.comment.value;
    if (!comment.trim()) {
      this.editCommentForm.markAllAsTouched();
      this.toast.validation('Escreva um comentário.');
      return;
    }
    this.http.put<ApiResponse<ActivityItem>>(`${this.baseUrl}/${activity.id}/comments`, { comment }).subscribe({
      next: () => { this.cancelEditing(); this.reload(); this.toast.success('Comentário atualizado.'); },
      error: () => this.toast.error('Não foi possível atualizar o comentário.')
    });
  }

  deleteComment(activity: ActivityItem) {
    this.http.patch<ApiResponse<void>>(`${this.baseUrl}/${activity.id}/comments/delete`, {}).subscribe({
      next: () => { this.reload(); this.toast.success('Comentário removido.'); },
      error: () => this.toast.error('Não foi possível remover o comentário.')
    });
  }

  canEdit(activity: ActivityItem) {
    return activity.type === 'COMMENT' && !activity.deleted && activity.authorId === this.auth.currentUser()?.userId;
  }

  activityDescription(description: string) {
    const statuses: Record<string, string> = {
      NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em progresso', WAITING_CLIENT: 'Aguardando cliente',
      WAITING_APPROVAL: 'Aguardando aprovação', ON_HOLD: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada'
    };
    return Object.entries(statuses).reduce((text, [status, label]) => text.replaceAll(status, label), description)
      .replace(/(\d+)\.00%/g, '$1%')
      .replace(/(\d+),(00)%/g, '$1%');
  }

  private load(append: boolean) {
    if (!this.projectId || this.loading()) return;
    this.loading.set(true);
    const params = new URLSearchParams({
      projectId: this.projectId,
      filter: this.filter(),
      page: String(this.page()),
      size: '20',
      sort: 'createdAt,desc'
    });
    if (this.stageId) params.set('stageId', this.stageId);
    const search = this.searchForm.controls.search.value;
    if (search) params.set('search', search);
    this.http.get<ApiResponse<Page<ActivityItem>>>(`${this.baseUrl}?${params}`).subscribe({
      next: (response) => {
        this.totalPages.set(response.data.totalPages || 0);
        this.activities.set(append ? [...this.activities(), ...response.data.content] : response.data.content);
        this.loading.set(false);
      },
      error: () => {
        if (append) this.page.update((page) => Math.max(0, page - 1));
        this.loading.set(false);
        this.toast.error('Não foi possível carregar as atividades.');
      }
    });
  }

  initials(name: string) {
    return (name || 'Sistema').split(' ').slice(0, 2).map((part) => part.charAt(0)).join('').toUpperCase();
  }

  activityIcon(type: ActivityType) {
    const icons: Record<ActivityType, string> = {
      COMMENT: 'MessageSquare',
      STATUS_CHANGED: 'Flag',
      CHECKLIST_UPDATED: 'ListChecks',
      PROJECT_CREATED: 'FolderPlus',
      PROJECT_UPDATED: 'FolderPen',
      PROJECT_COMPLETED: 'BadgeCheck',
      PHASE_CREATED: 'Layers3',
      PHASE_UPDATED: 'Layers3',
      STAGE_CREATED: 'Workflow',
      STAGE_UPDATED: 'Workflow',
      STAGE_COMPLETED: 'CheckCircle2',
      ASSIGNED: 'UserPlus',
      UNASSIGNED: 'UserMinus',
      FILE_UPLOADED: 'FileText',
      FILE_VERSIONED: 'History',
      FILE_DOWNLOADED: 'Download',
      FILE_ARCHIVED: 'Archive',
      FILE_RESTORED: 'ArchiveRestore',
      FILE_MOVED: 'FolderInput',
      FILE_RENAMED: 'FilePenLine',
      DOCUMENT_GENERATED: 'FileCheck2',
      CLIENT_APPROVAL: 'Handshake',
      VISIT_REGISTERED: 'ClipboardList',
      SYSTEM: 'Info'
    };
    return icons[type] || 'Activity';
  }

  typeLabel(type: ActivityType) {
    const labels: Record<string, string> = {
      COMMENT: 'Comentário',
      STATUS_CHANGED: 'Status',
      CHECKLIST_UPDATED: 'Checklist',
      PROJECT_CREATED: 'Projeto',
      PROJECT_UPDATED: 'Projeto',
      PROJECT_COMPLETED: 'Projeto',
      PHASE_CREATED: 'Fase',
      PHASE_UPDATED: 'Fase',
      STAGE_CREATED: 'Etapa',
      STAGE_UPDATED: 'Etapa',
      STAGE_COMPLETED: 'Etapa',
      ASSIGNED: 'Responsável',
      UNASSIGNED: 'Responsável',
      FILE_UPLOADED: 'Arquivo',
      FILE_VERSIONED: 'Nova versão',
      FILE_DOWNLOADED: 'Download',
      FILE_ARCHIVED: 'Arquivo',
      FILE_RESTORED: 'Arquivo',
      FILE_MOVED: 'Arquivo',
      FILE_RENAMED: 'Arquivo',
      DOCUMENT_GENERATED: 'Documento',
      CLIENT_APPROVAL: 'Aprovação',
      VISIT_REGISTERED: 'Visita',
      SYSTEM: 'Sistema'
    };
    return labels[type] || 'Atividade';
  }

  typeClass(type: ActivityType) {
    if (type === 'COMMENT') return 'bg-blue-50 text-blue-700';
    if (type === 'CHECKLIST_UPDATED' || type === 'STAGE_COMPLETED' || type === 'PROJECT_COMPLETED') return 'bg-arqly-50 text-arqly-700';
    if (type === 'STATUS_CHANGED' || type === 'ASSIGNED' || type === 'UNASSIGNED') return 'bg-amber-50 text-amber-700';
    if (type.startsWith('FILE_')) return 'bg-blue-50 text-blue-700';
    return 'bg-slate-100 text-slate-600';
  }
}
