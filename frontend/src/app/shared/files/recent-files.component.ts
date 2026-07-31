import { DatePipe } from '@angular/common';
import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FileApiService } from './file-api.service';
import { FileItem, FileOwnerType } from './file.models';

@Component({
  selector: 'app-recent-files',
  standalone: true,
  imports: [DatePipe, LucideAngularModule],
  template: `
    <section class="card h-full p-5">
      <div class="flex items-center justify-between"><div><p class="text-xs font-extrabold uppercase tracking-[0.16em] text-arqly-700">Arquivos recentes</p><h4 class="mt-1 font-extrabold">Últimas atualizações</h4></div><lucide-icon class="text-arqly-700" name="Archive" size="20" /></div>
      <div class="mt-4 space-y-2">
        @for (file of files(); track file.id) {
          <div class="flex items-center gap-3 rounded-2xl bg-slate-50/70 p-3"><span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white text-arqly-700"><lucide-icon name="FileText" size="16" /></span><div class="min-w-0 flex-1"><p class="truncate text-sm font-bold">{{ file.name }}</p><p class="mt-0.5 text-xs text-slate-400">v{{ file.version }} · {{ file.updatedAt | date:'dd/MM/yyyy' }}</p></div></div>
        } @empty {<p class="rounded-2xl bg-slate-50/70 py-8 text-center text-sm text-slate-500">Nenhum arquivo enviado.</p>}
      </div>
    </section>
  `
})
export class RecentFilesComponent implements OnChanges {
  @Input({ required: true }) ownerType!: FileOwnerType;
  @Input({ required: true }) ownerId = '';
  private readonly api = inject(FileApiService);
  readonly files = signal<FileItem[]>([]);
  ngOnChanges() {
    if (this.ownerId) this.api.list({ ownerType: this.ownerType, ownerId: this.ownerId, status: 'ACTIVE', page: 0, size: 5 })
      .subscribe(response => this.files.set(response.data.content));
  }
}
