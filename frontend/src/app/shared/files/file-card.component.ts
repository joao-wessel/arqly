import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FileItem } from './file.models';

@Component({
  selector: 'app-file-card',
  standalone: true,
  imports: [DatePipe, LucideAngularModule],
  template: `
    <article class="rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-arqly-200 hover:shadow-sm">
      <div class="flex items-start gap-3">
        <span class="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-arqly-50 text-arqly-700">
          <lucide-icon [name]="icon()" size="20" />
        </span>
        <div class="min-w-0 flex-1"><p class="truncate font-extrabold">{{ file.name }}</p><p class="mt-1 text-xs text-slate-400">{{ sizeLabel(file.size) }} · v{{ file.version }}</p></div>
      </div>
      <div class="mt-4 flex flex-wrap gap-1">@for (tag of file.tags; track tag) {<span class="rounded-full bg-slate-100 px-2 py-1 text-[11px] font-bold text-slate-500">{{ tag }}</span>}</div>
      <div class="mt-4 flex items-center justify-between border-t border-slate-100 pt-3">
        <span class="text-xs text-slate-400">{{ file.updatedAt | date:'dd/MM/yyyy' }}</span>
        <button class="btn-secondary px-3 py-2" type="button" (click)="open.emit(file)"><lucide-icon name="Search" size="15" />Ver</button>
      </div>
    </article>
  `
})
export class FileCardComponent {
  @Input({ required: true }) file!: FileItem;
  @Output() open = new EventEmitter<FileItem>();
  icon() { return ['png', 'jpg', 'jpeg', 'webp', 'svg'].includes(this.file.extension) ? 'Image' : this.file.extension === 'pdf' ? 'FileText' : 'FileType'; }
  sizeLabel(size: number) {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }
}
