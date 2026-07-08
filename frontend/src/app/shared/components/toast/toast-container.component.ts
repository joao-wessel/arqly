import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="fixed right-5 top-5 z-[80] w-[min(420px,calc(100vw-2.5rem))] space-y-3">
      @for (toast of toastService.toasts(); track toast.id) {
        <article class="rounded-2xl border bg-white p-4 shadow-soft"
                 [class.border-arqly-200]="toast.type === 'success'"
                 [class.border-red-200]="toast.type === 'error'"
                 [class.border-slate-200]="toast.type === 'info'">
          <div class="flex gap-3">
            <div class="grid h-9 w-9 shrink-0 place-items-center rounded-full"
                 [class.bg-arqly-50]="toast.type === 'success'"
                 [class.text-arqly-700]="toast.type === 'success'"
                 [class.bg-red-50]="toast.type === 'error'"
                 [class.text-red-600]="toast.type === 'error'"
                 [class.bg-slate-100]="toast.type === 'info'"
                 [class.text-slate-600]="toast.type === 'info'">
              <lucide-icon [name]="toast.type === 'error' ? 'CircleAlert' : toast.type === 'success' ? 'Check' : 'Info'" size="18"></lucide-icon>
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-sm font-bold text-slate-950">{{ toast.title }}</p>
              @if (toast.message) {
                <p class="mt-1 text-sm leading-5 text-slate-500">{{ toast.message }}</p>
              }
            </div>
            <button class="text-slate-400 transition hover:text-slate-700" type="button" (click)="toastService.dismiss(toast.id)">
              <lucide-icon name="X" size="18"></lucide-icon>
            </button>
          </div>
        </article>
      }
    </div>
  `
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);
}
