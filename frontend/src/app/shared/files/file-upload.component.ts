import { HttpClient, HttpEventType, HttpRequest } from '@angular/common/http';
import { Component, EventEmitter, Input, Output, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Subscription } from 'rxjs';
import { ApiResponse } from '../../core/auth/auth.models';
import { FileItem, FileOwnerType } from './file.models';

interface UploadEntry {
  id: string;
  file: File;
  progress: number;
  status: 'waiting' | 'conflict' | 'uploading' | 'completed' | 'error' | 'cancelled';
  subscription?: Subscription;
  existing?: FileItem;
}

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="rounded-2xl border-2 border-dashed p-5 text-center transition"
      [class.border-arqly-400]="dragging()" [class.bg-arqly-50]="dragging()"
      [class.border-slate-200]="!dragging()" (dragover)="dragOver($event)" (dragleave)="dragging.set(false)" (drop)="drop($event)">
      <input #fileInput class="hidden" type="file" multiple (change)="selectFiles($any($event.target).files)">
      <span class="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-arqly-50 text-arqly-700"><lucide-icon name="Upload" size="22" /></span>
      <p class="mt-3 font-extrabold">Arraste arquivos para cá</p>
      <p class="mt-1 text-xs text-slate-500">ou selecione vários arquivos no dispositivo</p>
      <button class="btn-secondary mx-auto mt-4" type="button" (click)="fileInput.click()"><lucide-icon name="Plus" size="16" />Selecionar arquivos</button>
    </div>

    @if (entries().length) {
      <div class="mt-4 max-h-56 space-y-2 overflow-y-auto">
        @for (entry of entries(); track entry.id) {
          <div class="rounded-2xl border border-slate-200 bg-white p-3">
            <div class="flex items-center justify-between gap-3">
              <div class="min-w-0"><p class="truncate text-sm font-bold">{{ entry.file.name }}</p><p class="text-xs text-slate-400">{{ statusLabel(entry) }}</p></div>
              @if (entry.status === 'uploading') {<button class="btn-secondary px-2 py-1.5" title="Cancelar upload" type="button" (click)="cancel(entry)"><lucide-icon name="X" size="14" /></button>}
            </div>
            <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100"><div class="h-full rounded-full bg-arqly-600 transition-all" [style.width.%]="entry.progress"></div></div>
          </div>
        }
      </div>
    }

    @if (conflict()) {
      <div class="modal-overlay fixed inset-0 z-[80] grid place-items-center p-4">
        <section class="modal-panel card w-full max-w-md p-6">
          <span class="grid h-12 w-12 place-items-center rounded-2xl bg-amber-50 text-amber-700"><lucide-icon name="CircleAlert" size="22" /></span>
          <h3 class="mt-4 text-xl font-extrabold">Arquivo já existente</h3>
          <p class="mt-2 text-sm leading-6 text-slate-500"><strong>{{ conflict()!.file.name }}</strong> já existe nesta pasta. Como deseja continuar?</p>
          <div class="mt-6 grid gap-3 sm:grid-cols-2">
            <button class="btn-secondary justify-center" type="button" (click)="resolveConflict('RENAME')"><lucide-icon name="Pencil" size="16" />Renomear cópia</button>
            <button class="btn-primary justify-center" type="button" (click)="resolveConflict('NEW_VERSION')"><lucide-icon name="History" size="16" />Nova versão</button>
          </div>
          <button class="mt-3 w-full text-center text-sm font-bold text-slate-400" type="button" (click)="cancelConflict()">Cancelar este arquivo</button>
        </section>
      </div>
    }
  `
})
export class FileUploadComponent {
  @Input({ required: true }) ownerType!: FileOwnerType;
  @Input({ required: true }) ownerId!: string;
  @Input() folderId: string | null = null;
  @Input() existingFiles: FileItem[] = [];
  @Output() completed = new EventEmitter<void>();
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/tenant/files';
  readonly dragging = signal(false);
  readonly entries = signal<UploadEntry[]>([]);
  readonly conflict = signal<UploadEntry | null>(null);

  dragOver(event: DragEvent) { event.preventDefault(); this.dragging.set(true); }
  drop(event: DragEvent) { event.preventDefault(); this.dragging.set(false); this.selectFiles(event.dataTransfer?.files); }

  selectFiles(files?: FileList | null) {
    if (!files?.length) return;
    Array.from(files).forEach(file => {
      const existing = this.existingFiles.find(item => item.name.toLowerCase() === file.name.toLowerCase() && item.status !== 'DELETED');
      const entry: UploadEntry = { id: crypto.randomUUID(), file, progress: 0, status: existing ? 'conflict' : 'waiting', existing };
      this.entries.update(items => [...items, entry]);
      if (existing) {
        if (!this.conflict()) this.conflict.set(entry);
      } else {
        this.upload(entry);
      }
    });
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }

  resolveConflict(strategy: 'NEW_VERSION' | 'RENAME') {
    const entry = this.conflict();
    if (!entry) return;
    this.conflict.set(null);
    this.upload(entry, strategy);
    this.showNextConflict();
  }

  cancelConflict() {
    const entry = this.conflict();
    if (!entry) return;
    this.patch(entry.id, { status: 'cancelled' });
    this.conflict.set(null);
    this.showNextConflict();
  }

  cancel(entry: UploadEntry) {
    entry.subscription?.unsubscribe();
    this.patch(entry.id, { status: 'cancelled', progress: 0 });
  }

  statusLabel(entry: UploadEntry) {
    return ({ waiting: 'Preparando...', conflict: 'Aguardando decisão', uploading: `${entry.progress}% enviado`,
      completed: 'Upload concluído', error: 'Não foi possível enviar', cancelled: 'Upload cancelado' })[entry.status];
  }

  private upload(entry: UploadEntry, strategy?: 'NEW_VERSION' | 'RENAME') {
    this.patch(entry.id, { status: 'uploading', progress: 0 });
    const form = new FormData();
    let url = `${this.baseUrl}/upload`;
    if (strategy === 'NEW_VERSION' && entry.existing) {
      url = `${this.baseUrl}/${entry.existing.id}/versions`;
      form.append('file', entry.file);
    } else {
      form.append('files', entry.file);
      form.append('metadata', new Blob([JSON.stringify({
        ownerType: this.ownerType, ownerId: this.ownerId, folderId: this.folderId,
        visibility: 'INTERNAL', conflictStrategy: strategy || null, tags: []
      })], { type: 'application/json' }));
    }
    const request = new HttpRequest('POST', url, form, { reportProgress: true });
    const subscription = this.http.request<ApiResponse<FileItem | FileItem[]>>(request).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress) {
          this.patch(entry.id, { progress: event.total ? Math.round(100 * event.loaded / event.total) : 0 });
        }
        if (event.type === HttpEventType.Response) {
          this.patch(entry.id, { status: 'completed', progress: 100 });
          this.completed.emit();
        }
      },
      error: () => this.patch(entry.id, { status: 'error', progress: 0 })
    });
    this.patch(entry.id, { subscription });
  }

  private patch(id: string, value: Partial<UploadEntry>) {
    this.entries.update(items => items.map(item => item.id === id ? { ...item, ...value } : item));
  }

  private showNextConflict() {
    this.conflict.set(this.entries().find(item => item.status === 'conflict') || null);
  }
}
