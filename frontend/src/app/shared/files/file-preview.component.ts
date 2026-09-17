import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { LucideAngularModule } from 'lucide-angular';
export interface FilePreviewFile {
  name: string;
  mimeType: string;
  extension: string;
}

@Component({
  selector: 'app-file-preview',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="modal-overlay fixed inset-0 z-[75] grid place-items-center p-4">
      <section class="modal-panel card flex h-[90vh] w-full max-w-6xl flex-col overflow-hidden">
        <header class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
          <div class="min-w-0"><p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">Pré-visualização</p><h3 class="mt-1 truncate text-xl font-extrabold">{{ file.name }}</h3></div>
          <button class="btn-secondary px-3 py-2" type="button" (click)="closed.emit()"><lucide-icon name="X" size="18" /></button>
        </header>
        <div class="min-h-0 flex-1 bg-slate-100 p-4">
          @if (image()) {
            <div class="grid h-full place-items-center overflow-auto"><img class="max-h-full max-w-full rounded-xl object-contain shadow-sm" [src]="url" [alt]="file.name"></div>
          } @else {
            <iframe class="h-full w-full rounded-xl border-0 bg-white" [src]="safeUrl()" [title]="file.name"></iframe>
          }
        </div>
      </section>
    </div>
  `
})
export class FilePreviewComponent {
  private readonly sanitizer = inject(DomSanitizer);
  @Input({ required: true }) file!: FilePreviewFile;
  @Input({ required: true }) url = '';
  @Output() closed = new EventEmitter<void>();
  image() { return this.file.mimeType.startsWith('image/') && this.file.extension !== 'svg'; }
  safeUrl() { return this.sanitizer.bypassSecurityTrustResourceUrl(this.url); }
}
