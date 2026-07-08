import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ColorPalette, ThemeService } from '../../core/theme/theme.service';

interface PaletteOption {
  label: string;
  value: ColorPalette;
  swatch: string;
}

@Component({
  selector: 'app-appearance-controls',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="relative" (click)="$event.stopPropagation()">
      <button
        class="grid h-11 w-11 place-items-center rounded-2xl border border-slate-200 bg-white text-slate-500 shadow-sm transition hover:bg-arqly-50 hover:text-arqly-700"
        type="button"
        title="Aparência"
        [attr.aria-expanded]="open()"
        (click)="togglePanel()"
      >
        <lucide-icon name="SlidersHorizontal" size="19"></lucide-icon>
      </button>

      @if (open()) {
        <div class="absolute right-0 top-[calc(100%+0.75rem)] z-[80] w-72 rounded-2xl border border-slate-200 bg-white p-3 shadow-[0_22px_60px_rgba(15,23,42,0.16)]">
          <div class="px-2 pb-3">
            <p class="text-sm font-extrabold text-slate-950">Aparência</p>
            <p class="mt-1 text-xs text-slate-500">Ajuste o modo e a paleta do Arqly.</p>
          </div>

          <button
            class="flex w-full items-center justify-between rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-semibold text-slate-700 transition hover:border-arqly-200 hover:bg-arqly-50 hover:text-arqly-700"
            type="button"
            (click)="theme.toggleMode()"
          >
            <span class="flex items-center gap-2">
              <lucide-icon [name]="theme.mode() === 'dark' ? 'Sun' : 'Moon'" size="17"></lucide-icon>
              {{ modeTitle() }}
            </span>
            <span class="rounded-full bg-slate-100 px-2 py-1 text-[0.7rem] font-bold uppercase text-slate-500">
              {{ theme.mode() === 'dark' ? 'Dark' : 'Light' }}
            </span>
          </button>

          <div class="mt-4 space-y-2">
            <p class="px-2 text-xs font-bold uppercase tracking-[0.16em] text-slate-400">Paleta</p>
            @for (palette of palettes; track palette.value) {
              <button
                class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                type="button"
                [class.bg-arqly-50]="theme.palette() === palette.value"
                [class.text-arqly-700]="theme.palette() === palette.value"
                (click)="theme.setPalette(palette.value)"
              >
                <span
                  class="h-7 w-7 rounded-full border border-white shadow-sm ring-1 ring-slate-200"
                  [style.background]="palette.swatch"
                ></span>
                <span class="flex-1">{{ palette.label }}</span>
                @if (theme.palette() === palette.value) {
                  <lucide-icon name="Check" size="17"></lucide-icon>
                }
              </button>
            }
          </div>
        </div>
      }
    </div>
  `
})
export class AppearanceControlsComponent {
  readonly theme = inject(ThemeService);
  readonly open = signal(false);
  readonly palettes: PaletteOption[] = [
    { label: 'Verde petróleo', value: 'arqly', swatch: 'linear-gradient(135deg, #0d7b73, #9bd8cf)' },
    { label: 'Terracota editorial', value: 'terracotta', swatch: 'linear-gradient(135deg, #b95f3b, #f0b58f)' },
    { label: 'Índigo blueprint', value: 'indigo', swatch: 'linear-gradient(135deg, #5750b5, #a7b4ff)' }
  ];
  readonly modeTitle = computed(() => this.theme.mode() === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro');

  @HostListener('document:click')
  closePanel() {
    this.open.set(false);
  }

  togglePanel() {
    this.open.update((current) => !current);
  }
}
