import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FileFolder } from './file.models';

@Component({
  selector: 'app-folder-tree',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <nav class="space-y-1" aria-label="Pastas">
      <button class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-bold transition"
        type="button" [class.bg-arqly-50]="selectedId === null" [class.text-arqly-700]="selectedId === null"
        [class.text-slate-500]="selectedId !== null" (click)="selected.emit(null)">
        <lucide-icon name="Folder" size="17" />Todos os arquivos
      </button>
      @for (folder of orderedFolders(); track folder.id) {
        <button class="flex w-full items-center gap-3 rounded-xl py-2.5 pr-3 text-left text-sm font-bold transition"
          type="button" [style.padding-left.px]="12 + depth(folder) * 18"
          [class.bg-arqly-50]="selectedId === folder.id" [class.text-arqly-700]="selectedId === folder.id"
          [class.text-slate-500]="selectedId !== folder.id" (click)="selected.emit(folder.id)">
          <lucide-icon name="Folder" size="17" /><span class="truncate">{{ folder.name }}</span>
        </button>
      }
    </nav>
  `
})
export class FolderTreeComponent {
  @Input() folders: FileFolder[] = [];
  @Input() selectedId: string | null = null;
  @Output() selected = new EventEmitter<string | null>();

  orderedFolders() {
    const result: FileFolder[] = [];
    const append = (parentId: string | null) => {
      this.folders.filter(item => (item.parentId || null) === parentId)
        .sort((a, b) => a.name.localeCompare(b.name))
        .forEach(item => { result.push(item); append(item.id); });
    };
    append(null);
    return result;
  }

  depth(folder: FileFolder) {
    let depth = 0;
    let parentId = folder.parentId;
    while (parentId && depth < 12) {
      depth++;
      parentId = this.folders.find(item => item.id === parentId)?.parentId;
    }
    return depth;
  }
}
