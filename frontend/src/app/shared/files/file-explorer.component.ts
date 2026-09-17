import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ArqlySelectComponent } from '../components/arqly-select.component';
import { ToastService } from '../components/toast/toast.service';
import { FileApiService } from './file-api.service';
import { FileCardComponent } from './file-card.component';
import { FileFolder, FileItem, FileOwnerType, FileStatus, FileVersion } from './file.models';
import { FilePreviewComponent } from './file-preview.component';
import { FileUploadComponent } from './file-upload.component';
import { FolderTreeComponent } from './folder-tree.component';
import { VersionHistoryComponent } from './version-history.component';

@Component({
  selector: 'app-file-explorer',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, LucideAngularModule, ArqlySelectComponent, FolderTreeComponent,
    FileUploadComponent, FileCardComponent, FilePreviewComponent, VersionHistoryComponent],
  template: `
    <section class="card overflow-hidden">
      <header class="flex flex-col gap-4 border-b border-slate-200 p-5 md:flex-row md:items-center md:justify-between">
        <div><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">{{ eyebrow }}</p><h3 class="mt-1 text-xl font-extrabold">{{ title }}</h3><p class="mt-1 text-sm text-slate-500">Pastas, versões e arquivos relacionados a este contexto.</p></div>
        <div class="flex flex-wrap gap-2">
          <button class="btn-secondary" type="button" (click)="openFolderModal()"><lucide-icon name="FolderPlus" size="17" />Nova pasta</button>
          <button class="btn-primary" type="button" (click)="uploadVisible.set(!uploadVisible())"><lucide-icon name="Upload" size="17" />Enviar arquivos</button>
        </div>
      </header>

      @if (uploadVisible()) {
        <div class="border-b border-slate-200 bg-slate-50/70 p-5">
          <app-file-upload [ownerType]="ownerType" [ownerId]="ownerId" [folderId]="selectedFolderId()"
            [existingFiles]="currentFolderFiles()" (completed)="uploadCompleted()" />
        </div>
      }

      <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-[minmax(260px,1.4fr)_130px_160px_170px_160px_auto]"
        [formGroup]="filterForm" (ngSubmit)="reload()">
        <input class="field h-12 py-0 sm:col-span-2 lg:col-span-3 xl:col-span-1" formControlName="search" placeholder="Pesquisar por nome">
        <input class="field h-12 py-0" formControlName="extension" placeholder="Extensão">
        <input class="field h-12 py-0" formControlName="author" placeholder="Autor">
        <app-arqly-select class="file-filter-select" formControlName="ownerType" placeholder="Contexto" [options]="ownerTypeOptions" panelMode="fixed" />
        <app-arqly-select class="file-filter-select" formControlName="status" placeholder="Status" [options]="statusOptions" panelMode="fixed" />
        <button class="btn-secondary h-12 justify-center whitespace-nowrap px-4 py-0" type="submit"><lucide-icon name="Search" size="17" />Pesquisar</button>
      </form>

      <div class="grid min-h-[440px] lg:grid-cols-[240px_minmax(0,1fr)]">
        <aside class="border-b border-slate-200 bg-slate-50/40 p-4 lg:border-b-0 lg:border-r">
          <div class="mb-3 flex items-center justify-between"><p class="text-xs font-extrabold uppercase tracking-[0.16em] text-slate-400">Pastas</p>@if (selectedFolderId()) {<button class="text-xs font-bold text-red-500" type="button" (click)="confirmFolderDelete()">Excluir</button>}</div>
          <app-folder-tree [folders]="folders()" [selectedId]="selectedFolderId()" (selected)="selectFolder($event)" />
        </aside>

        <div class="min-w-0">
          <div class="hidden min-w-0 md:block">
            <table class="w-full table-fixed text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500"><tr><th class="px-4 py-4">Arquivo</th>@if (browseAll) {<th class="hidden px-4 py-4 xl:table-cell">Contexto</th>}<th class="hidden px-4 py-4 xl:table-cell">Pasta</th><th class="w-16 px-3 py-4">Versão</th><th class="w-20 px-3 py-4">Tamanho</th><th class="hidden w-28 px-3 py-4 xl:table-cell">Autor</th><th class="hidden w-36 px-3 py-4 xl:table-cell">Atualizado</th><th class="w-32 px-3 py-4 text-right">Ações</th></tr></thead>
              <tbody>
                @for (file of files(); track file.id) {
                  <tr class="border-t border-slate-100">
                    <td class="px-4 py-4"><div class="flex min-w-0 items-center gap-3"><span class="grid h-10 w-10 shrink-0 place-items-center rounded-2xl bg-arqly-50 text-arqly-700"><lucide-icon [name]="fileIcon(file)" size="18" /></span><div class="min-w-0"><p class="truncate font-bold">{{ file.name }}</p><p class="mt-1 text-xs uppercase text-slate-400">{{ file.extension || 'arquivo' }} · {{ visibilityLabel(file.visibility) }}</p></div></div></td>
                    @if (browseAll) {<td class="hidden px-4 py-4 xl:table-cell"><p class="truncate font-bold">{{ file.ownerLabel }}</p><p class="mt-1 text-xs text-slate-400">{{ ownerTypeLabel(file.ownerType) }}</p></td>}
                    <td class="hidden px-4 py-4 text-slate-500 xl:table-cell">{{ file.folderName || 'Raiz' }}</td>
                    <td class="px-3 py-4 font-bold">v{{ file.version }}</td>
                    <td class="px-3 py-4 text-slate-500">{{ sizeLabel(file.size) }}</td>
                    <td class="hidden px-3 py-4 xl:table-cell">{{ file.uploadedByName }}</td>
                    <td class="hidden px-3 py-4 text-slate-500 xl:table-cell">{{ file.updatedAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td class="px-3 py-4"><div class="flex justify-end gap-1">@if (file.previewAvailable) {<button class="btn-secondary px-2.5 py-2" title="Visualizar" type="button" (click)="preview(file)"><lucide-icon name="Eye" size="15" /></button>}<button class="btn-secondary px-2.5 py-2" title="Baixar" type="button" (click)="download(file)"><lucide-icon name="Download" size="15" /></button><button class="btn-secondary px-2.5 py-2" title="Detalhes" type="button" (click)="openDetails(file)"><lucide-icon name="Search" size="15" /></button></div></td>
                </tr>
                } @empty {<tr><td [attr.colspan]="browseAll ? 8 : 7" class="px-4 py-14 text-center text-slate-500">Nenhum arquivo encontrado.</td></tr>}
              </tbody>
            </table>
          </div>
          <div class="grid gap-3 p-4 md:hidden">@for (file of files(); track file.id) {<app-file-card [file]="file" (open)="openDetails($event)" />} @empty {<p class="py-12 text-center text-sm text-slate-500">Nenhum arquivo encontrado.</p>}</div>
          @if (hasMore()) {<div #sentinel class="border-t border-slate-100 py-5 text-center text-xs font-bold text-slate-400">{{ loading() ? 'Carregando arquivos...' : 'Role para carregar mais' }}</div>}
        </div>
      </div>

      <footer class="flex flex-col gap-2 border-t border-slate-200 p-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <span>{{ totalElements() }} arquivo(s) encontrado(s)</span><span>{{ totalSizeLabel() }} nesta visualização</span>
      </footer>
    </section>

    @if (folderModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-[70] grid place-items-center p-4"><form class="modal-panel card w-full max-w-md p-6" [formGroup]="folderForm" (ngSubmit)="saveFolder()">
        <div class="flex items-center justify-between"><div><p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">Organização</p><h3 class="mt-1 text-xl font-extrabold">Nova pasta</h3></div><button class="btn-secondary px-3 py-2" type="button" (click)="folderModalOpen.set(false)"><lucide-icon name="X" size="18" /></button></div>
        <label class="mt-5 block space-y-1"><span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span><input class="field" formControlName="name" placeholder="Ex.: Arquitetura"></label>
        <p class="mt-3 text-xs text-slate-400">A pasta será criada em {{ selectedFolderName() }}.</p>
        <div class="mt-6 flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="folderModalOpen.set(false)">Cancelar</button><button class="btn-primary" type="submit"><lucide-icon name="Save" size="16" />Criar pasta</button></div>
      </form></div>
    }

    @if (details()) {
      <div class="modal-overlay fixed inset-0 z-[70] grid place-items-center p-4"><section class="modal-panel card flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden">
        <header class="flex items-center justify-between border-b border-slate-200 px-6 py-5"><div class="min-w-0"><p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">Informações do arquivo</p><h3 class="mt-1 truncate text-xl font-extrabold">{{ details()!.name }}</h3></div><button class="btn-secondary px-3 py-2" type="button" (click)="closeDetails()"><lucide-icon name="X" size="18" /></button></header>
        <div class="min-h-0 flex-1 space-y-5 overflow-y-auto p-6">
          <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-2xl bg-slate-50 p-4"><p class="text-xs font-bold text-slate-400">Tipo</p><strong class="mt-1 block uppercase">{{ details()!.extension || 'Arquivo' }}</strong></div>
            <div class="rounded-2xl bg-slate-50 p-4"><p class="text-xs font-bold text-slate-400">Tamanho</p><strong class="mt-1 block">{{ sizeLabel(details()!.size) }}</strong></div>
            <div class="rounded-2xl bg-slate-50 p-4"><p class="text-xs font-bold text-slate-400">Versão atual</p><strong class="mt-1 block">v{{ details()!.version }}</strong></div>
          </div>
          <form class="grid gap-4 md:grid-cols-2" [formGroup]="detailsForm" (ngSubmit)="saveDetails()">
            <label class="block space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Nome</span><input class="field" formControlName="name"></label>
            <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Pasta</span><app-arqly-select formControlName="folderId" placeholder="Raiz" [options]="folderOptions()" panelMode="fixed" /></label>
            <label class="block space-y-1"><span class="text-xs font-bold text-slate-500">Visibilidade</span><app-arqly-select formControlName="visibility" placeholder="Selecione" [options]="visibilityOptions" panelMode="fixed" /></label>
            <label class="block space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Tags</span><input class="field" formControlName="tags" placeholder="Aprovado, Cliente, Executivo"></label>
            <div class="flex justify-end md:col-span-2"><button class="btn-primary" type="submit"><lucide-icon name="Save" size="16" />Salvar alterações</button></div>
          </form>
          <div class="rounded-2xl border border-slate-200 p-4"><div class="grid gap-2 text-xs sm:grid-cols-2"><p><strong>Checksum:</strong> <span class="break-all text-slate-500">{{ details()!.checksum }}</span></p><p><strong>Autor:</strong> {{ details()!.uploadedByName }}</p><p><strong>Pasta:</strong> {{ details()!.folderName || 'Raiz' }}</p><p><strong>Status:</strong> {{ statusLabel(details()!.status) }}</p></div></div>
          <div><div class="mb-3 flex items-center justify-between"><h4 class="font-extrabold">Histórico de versões</h4><button class="btn-secondary px-3 py-2" type="button" (click)="versionInput.click()"><lucide-icon name="Upload" size="15" />Nova versão</button><input #versionInput class="hidden" type="file" (change)="uploadVersion($any($event.target).files?.[0]); versionInput.value = ''"></div><app-version-history [versions]="versions()" (download)="downloadVersion($event)" /></div>
        </div>
        <footer class="flex flex-wrap justify-between gap-3 border-t border-slate-200 px-6 py-4"><div class="flex gap-2">@if (details()!.status === 'ACTIVE') {<button class="btn-secondary" type="button" (click)="askAction('archive')"><lucide-icon name="Archive" size="16" />Arquivar</button><button class="btn-secondary text-red-600" type="button" (click)="askAction('delete')"><lucide-icon name="Trash2" size="16" />Excluir</button>} @else {<button class="btn-secondary" type="button" (click)="askAction('restore')"><lucide-icon name="ArchiveRestore" size="16" />Restaurar</button>}</div><div class="flex gap-2">@if (details()!.previewAvailable) {<button class="btn-secondary" type="button" (click)="preview(details()!)"><lucide-icon name="Eye" size="16" />Visualizar</button>}<button class="btn-primary" type="button" (click)="download(details()!)"><lucide-icon name="Download" size="16" />Baixar</button></div></footer>
      </section></div>
    }

    @if (previewFile() && previewUrl()) {<app-file-preview [file]="previewFile()!" [url]="previewUrl()!" (closed)="closePreview()" />}

    @if (confirmAction()) {
      <div class="modal-overlay fixed inset-0 z-[90] grid place-items-center p-4"><section class="modal-panel card w-full max-w-md p-6">
        <span class="grid h-12 w-12 place-items-center rounded-2xl bg-amber-50 text-amber-700"><lucide-icon name="CircleAlert" size="22" /></span><h3 class="mt-4 text-xl font-extrabold">Confirmar ação</h3><p class="mt-2 text-sm leading-6 text-slate-500">{{ confirmMessage() }}</p>
        <div class="mt-6 flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="confirmAction.set(null)">Cancelar</button><button class="btn-primary" type="button" (click)="executeAction()">Confirmar</button></div>
      </section></div>
    }
  `
})
export class FileExplorerComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) ownerType!: FileOwnerType;
  @Input({ required: true }) ownerId = '';
  @Input() title = 'Arquivos';
  @Input() eyebrow = 'Gestão de arquivos';
  @Input() browseAll = false;
  @Input() relatedProjectId?: string;

  private readonly api = inject(FileApiService);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private observer?: IntersectionObserver;
  readonly files = signal<FileItem[]>([]);
  readonly folders = signal<FileFolder[]>([]);
  readonly selectedFolderId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly uploadVisible = signal(false);
  readonly folderModalOpen = signal(false);
  readonly details = signal<FileItem | null>(null);
  readonly versions = signal<FileVersion[]>([]);
  readonly previewFile = signal<FileItem | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly confirmAction = signal<'archive' | 'restore' | 'delete' | 'delete-folder' | null>(null);
  readonly hasMore = computed(() => this.page() + 1 < this.totalPages());
  readonly totalSizeLabel = computed(() => this.sizeLabel(this.files().reduce((sum, item) => sum + item.size, 0)));
  readonly currentFolderFiles = computed(() => this.files().filter(
    item => (item.folderId || null) === this.selectedFolderId()
      && item.ownerType === this.ownerType
      && item.ownerId === this.ownerId
  ));
  readonly filterForm = this.fb.nonNullable.group({ search: [''], extension: [''], author: [''], ownerType: [''], status: ['ACTIVE'] });
  readonly folderForm = this.fb.nonNullable.group({ name: ['', Validators.required] });
  readonly detailsForm = this.fb.nonNullable.group({ name: ['', Validators.required], folderId: [''], visibility: ['INTERNAL'], tags: [''] });
  readonly statusOptions: { label: string; value: FileStatus }[] = [{ label: 'Ativos', value: 'ACTIVE' }, { label: 'Arquivados', value: 'ARCHIVED' }, { label: 'Excluídos', value: 'DELETED' }];
  readonly ownerTypeOptions = [
    { label: 'Todos os contextos', value: '' },
    { label: 'Briefings', value: 'BRIEFING' },
    { label: 'Propostas', value: 'PROPOSAL' },
    { label: 'Projetos', value: 'PROJECT' },
    { label: 'Etapas', value: 'PROJECT_STAGE' },
    { label: 'Diário de Obra', value: 'CONSTRUCTION_DIARY_ENTRY' },
    { label: 'Documentos', value: 'DOCUMENT' },
    { label: 'Escritório', value: 'TENANT' }
  ];
  readonly visibilityOptions = [{ label: 'Somente equipe', value: 'INTERNAL' }, { label: 'Visível ao cliente', value: 'CLIENT_VISIBLE' }];

  @ViewChild('sentinel')
  set sentinel(element: ElementRef<HTMLElement> | undefined) {
    this.observer?.disconnect();
    if (!element) return;
    this.observer = new IntersectionObserver(entries => {
      if (entries[0]?.isIntersecting) this.loadMore();
    }, { rootMargin: '180px' });
    this.observer.observe(element.nativeElement);
  }

  ngOnChanges(changes: SimpleChanges) {
    if ((changes['ownerId'] || changes['ownerType']) && this.ownerId) {
      this.selectedFolderId.set(null);
      this.loadFolders();
      this.reload();
    }
  }

  ngOnDestroy() { this.observer?.disconnect(); this.closePreview(); }

  reload() { this.page.set(0); this.load(false); }
  loadMore() { if (!this.loading() && this.hasMore()) { this.page.update(value => value + 1); this.load(true); } }
  selectFolder(id: string | null) { this.selectedFolderId.set(id); this.reload(); }
  uploadCompleted() { this.loadFolders(); this.reload(); }
  openFolderModal() { this.folderForm.reset(); this.folderModalOpen.set(true); }
  selectedFolderName() { return this.folders().find(item => item.id === this.selectedFolderId())?.name || 'Todos os arquivos'; }
  folderOptions() { return [{ label: 'Raiz', value: '' }, ...this.folders().map(item => ({ label: item.name, value: item.id }))]; }

  saveFolder() {
    if (this.folderForm.invalid) { this.toast.validation('Informe o nome da pasta.'); return; }
    this.api.createFolder(this.ownerType, this.ownerId, this.folderForm.controls.name.value, this.selectedFolderId()).subscribe({
      next: response => { this.folderModalOpen.set(false); this.loadFolders(); this.selectedFolderId.set(response.data.id); this.reload(); this.toast.success('Pasta criada.'); },
      error: () => this.toast.error('Não foi possível criar a pasta.')
    });
  }

  confirmFolderDelete() { this.confirmAction.set('delete-folder'); }

  openDetails(file: FileItem) {
    this.details.set(file);
    this.detailsForm.reset({ name: file.name, folderId: file.folderId || '', visibility: file.visibility, tags: file.tags.join(', ') });
    this.api.versions(file.id).subscribe(response => this.versions.set(response.data));
  }

  closeDetails() { this.details.set(null); this.versions.set([]); }

  saveDetails() {
    const file = this.details();
    if (!file || this.detailsForm.invalid) { this.toast.validation('Informe o nome do arquivo.'); return; }
    const raw = this.detailsForm.getRawValue();
    this.api.update(file.id, { name: raw.name, folderId: raw.folderId || null, visibility: raw.visibility,
      tags: raw.tags.split(',').map(item => item.trim()).filter(Boolean) }).subscribe({
      next: response => { this.details.set(response.data); this.reload(); this.loadFolders(); this.toast.success('Arquivo atualizado.'); },
      error: () => this.toast.error('Não foi possível atualizar o arquivo.')
    });
  }

  uploadVersion(file?: File) {
    const current = this.details();
    if (!file || !current) return;
    const form = new FormData();
    form.append('file', file);
    this.http.post<{ data: FileItem }>(`${this.api.baseUrl}/${current.id}/versions`, form).subscribe({
      next: response => { this.details.set(response.data); this.openDetails(response.data); this.reload(); this.toast.success('Nova versão enviada.'); },
      error: () => this.toast.error('Não foi possível enviar a nova versão.')
    });
  }

  askAction(action: 'archive' | 'restore' | 'delete') { this.confirmAction.set(action); }
  confirmMessage() {
    return ({ archive: 'O arquivo ficará disponível na área de arquivados.', restore: 'O arquivo voltará para os arquivos ativos.',
      delete: 'O arquivo será movido para a área de excluídos e poderá ser restaurado.',
      'delete-folder': 'A pasta será excluída. Ela precisa estar vazia.' } as Record<string, string>)[this.confirmAction() || ''] || '';
  }

  executeAction() {
    const action = this.confirmAction();
    if (action === 'delete-folder') {
      const folderId = this.selectedFolderId();
      if (!folderId) return;
      this.api.deleteFolder(folderId).subscribe({ next: () => { this.confirmAction.set(null); this.selectedFolderId.set(null); this.loadFolders(); this.reload(); this.toast.success('Pasta removida.'); }, error: () => this.toast.error('Não foi possível remover a pasta.') });
      return;
    }
    const file = this.details();
    if (!file || !action) return;
    const request = action === 'archive' ? this.api.archive(file.id) : action === 'restore' ? this.api.restore(file.id) : this.api.delete(file.id);
    request.subscribe({ next: () => { this.confirmAction.set(null); this.closeDetails(); this.setStatus(action === 'archive' ? 'ARCHIVED' : action === 'restore' ? 'ACTIVE' : 'DELETED'); this.toast.success('Ação concluída.'); }, error: () => this.toast.error('Não foi possível concluir a ação.') });
  }

  preview(file: FileItem) {
    this.api.preview(file.id).subscribe({
      next: blob => { this.closePreview(); this.previewFile.set(file); this.previewUrl.set(URL.createObjectURL(blob)); },
      error: () => this.toast.error('Pré-visualização indisponível para este arquivo.')
    });
  }

  closePreview() {
    if (this.previewUrl()) URL.revokeObjectURL(this.previewUrl()!);
    this.previewUrl.set(null);
    this.previewFile.set(null);
  }

  download(file: FileItem) { this.downloadBlob(file, undefined); }
  downloadVersion(version: FileVersion) { const file = this.details(); if (file) this.downloadBlob(file, version.id, `v${version.versionNumber}`); }
  private downloadBlob(file: FileItem, versionId?: string, suffix?: string) {
    this.api.download(file.id, versionId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob); const anchor = document.createElement('a');
        anchor.href = url; anchor.download = suffix ? `${this.baseName(file.name)}-${suffix}.${file.extension}` : file.name;
        anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => this.toast.error('Não foi possível baixar o arquivo.')
    });
  }

  fileIcon(file: FileItem) { return ['png', 'jpg', 'jpeg', 'webp', 'svg'].includes(file.extension) ? 'Image' : file.extension === 'pdf' ? 'FileText' : 'FileType'; }
  visibilityLabel(value: string) { return value === 'CLIENT_VISIBLE' ? 'Cliente' : 'Interno'; }
  ownerTypeLabel(value: FileOwnerType) {
    return ({ BRIEFING: 'Briefing', PROPOSAL: 'Proposta', PROJECT: 'Projeto', PROJECT_STAGE: 'Etapa', CONSTRUCTION_DIARY_ENTRY: 'Diário de Obra',
      DOCUMENT: 'Documento', TENANT: 'Escritório' })[value];
  }
  statusLabel(value: FileStatus) { return ({ ACTIVE: 'Ativo', ARCHIVED: 'Arquivado', DELETED: 'Excluído' })[value]; }
  setStatus(status: FileStatus) { this.filterForm.controls.status.setValue(status); this.reload(); }
  sizeLabel(size: number) { if (size < 1024) return `${size} B`; if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`; if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`; return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`; }
  private baseName(name: string) { const index = name.lastIndexOf('.'); return index > 0 ? name.substring(0, index) : name; }

  private loadFolders() {
    this.api.folders(this.ownerType, this.ownerId).subscribe({ next: response => this.folders.set(response.data), error: () => this.toast.error('Não foi possível carregar as pastas.') });
  }

  private load(append: boolean) {
    if (!this.ownerId || this.loading()) return;
    this.loading.set(true);
    const raw = this.filterForm.getRawValue();
    this.api.list({ ownerType: this.relatedProjectId ? undefined : this.browseAll ? (raw.ownerType as FileOwnerType || undefined) : this.ownerType,
      ownerId: this.relatedProjectId ? undefined : this.browseAll ? undefined : this.ownerId, projectId: this.relatedProjectId, folderId: this.selectedFolderId(),
      search: raw.search, extension: raw.extension, author: raw.author, status: raw.status as FileStatus,
      page: this.page(), size: 20 }).subscribe({
      next: response => { this.files.set(append ? [...this.files(), ...response.data.content] : response.data.content); this.totalPages.set(response.data.totalPages); this.totalElements.set(response.data.totalElements); this.loading.set(false); },
      error: () => { if (append) this.page.update(value => Math.max(0, value - 1)); this.loading.set(false); this.toast.error('Não foi possível carregar os arquivos.'); }
    });
  }
}
