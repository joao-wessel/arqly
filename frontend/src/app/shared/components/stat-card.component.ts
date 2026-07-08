import { Component, Input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="card p-6 transition hover:-translate-y-0.5 hover:shadow-xl">
      <div class="flex items-center gap-4">
        <div class="grid h-14 w-14 place-items-center rounded-full bg-arqly-100 text-arqly-700">
          <lucide-icon [name]="icon" size="24"></lucide-icon>
        </div>
        <div>
          <p class="text-sm font-medium text-slate-500">{{ label }}</p>
          <strong class="mt-1 block text-3xl font-extrabold tracking-tight">{{ value }}</strong>
          <span class="mt-2 block text-sm text-arqly-600">{{ hint }}</span>
        </div>
      </div>
    </div>
  `
})
export class StatCardComponent {
  @Input({ required: true }) label = '';
  @Input({ required: true }) value: string | number = '';
  @Input() hint = '';
  @Input() icon = 'Activity';
}
