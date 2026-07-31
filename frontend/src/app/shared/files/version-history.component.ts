import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FileVersion } from './file.models';

@Component({
  selector: 'app-version-history',
  standalone: true,
  imports: [DatePipe, LucideAngularModule],
  template: `
    <div class="max-h-72 space-y-2 overflow-y-auto pr-1">
      @for (version of versions; track version.id) {
        <div class="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 p-3">
          <div><p class="text-sm font-extrabold">Versão {{ version.versionNumber }}</p><p class="mt-1 text-xs text-slate-400">{{ version.authorName }} · {{ version.createdAt | date:'dd/MM/yyyy HH:mm' }}</p><p class="mt-1 text-xs text-slate-500">{{ version.revisionComment || 'Sem comentário de revisão' }}</p></div>
          <button class="btn-secondary px-3 py-2" title="Baixar versão" type="button" (click)="download.emit(version)"><lucide-icon name="Download" size="15" /></button>
        </div>
      } @empty { <p class="py-8 text-center text-sm text-slate-500">Nenhuma versão registrada.</p> }
    </div>
  `
})
export class VersionHistoryComponent {
  @Input() versions: FileVersion[] = [];
  @Output() download = new EventEmitter<FileVersion>();
}
