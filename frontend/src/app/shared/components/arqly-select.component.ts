import { Component, ElementRef, HostListener, Input, ViewChild, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

export interface ArqlySelectOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-arqly-select',
  standalone: true,
  imports: [LucideAngularModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ArqlySelectComponent),
      multi: true
    }
  ],
  template: `
    <div class="relative" (click)="$event.stopPropagation()">
      <button
        #trigger
        class="field flex items-center justify-between gap-3 text-left"
        type="button"
        [class.cursor-not-allowed]="disabled()"
        [class.bg-slate-50]="disabled()"
        [class.text-slate-400]="disabled()"
        [attr.aria-expanded]="open()"
        (click)="toggle()"
      >
        <span [class.text-slate-400]="!selectedLabel()">{{ selectedLabel() || placeholder }}</span>
        <lucide-icon
          name="ChevronDown"
          size="18"
          class="shrink-0 text-arqly-700 transition"
          [class.rotate-180]="open()"
        ></lucide-icon>
      </button>

      @if (open()) {
        <div
          class="z-[70] max-h-64 overflow-y-auto overscroll-contain rounded-2xl border border-slate-200 bg-white p-1.5 shadow-[0_18px_45px_rgba(15,23,42,0.14)]"
          [class.absolute]="panelMode === 'absolute'"
          [class.fixed]="panelMode === 'fixed'"
          [class.left-0]="panelMode === 'absolute'"
          [class.right-0]="panelMode === 'absolute'"
          [class.mt-2]="panelMode === 'inline'"
          [style.top]="panelMode === 'absolute' ? 'calc(100% + 0.5rem)' : null"
          [style.left.px]="panelMode === 'fixed' ? panelPosition().left : null"
          [style.top.px]="panelMode === 'fixed' ? panelPosition().top : null"
          [style.width.px]="panelMode === 'fixed' ? panelPosition().width : null"
        >
          @for (option of options; track option.value) {
            <button
              class="flex w-full items-center justify-between rounded-xl px-3.5 py-2.5 text-left text-sm font-semibold text-slate-700 transition hover:bg-arqly-50 hover:text-arqly-800"
              type="button"
              [class.bg-arqly-50]="option.value === value()"
              [class.text-arqly-800]="option.value === value()"
              (click)="select(option.value)"
            >
              <span>{{ option.label }}</span>
              @if (option.value === value()) {
                <lucide-icon name="Check" size="17" class="text-arqly-700"></lucide-icon>
              }
            </button>
          }
        </div>
      }
    </div>
  `
})
export class ArqlySelectComponent implements ControlValueAccessor {
  @Input() options: ArqlySelectOption[] = [];
  @Input() placeholder = 'Selecione';
  @Input() panelMode: 'absolute' | 'inline' | 'fixed' = 'absolute';
  @ViewChild('trigger') trigger?: ElementRef<HTMLButtonElement>;

  readonly open = signal(false);
  readonly value = signal('');
  readonly disabled = signal(false);
  readonly panelPosition = signal({ left: 0, top: 0, width: 0 });

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  @HostListener('document:click')
  close() {
    this.open.set(false);
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  reposition() {
    if (this.open() && this.panelMode === 'fixed') {
      this.updatePanelPosition();
    }
  }

  selectedLabel() {
    return this.options.find((option) => option.value === this.value())?.label || '';
  }

  toggle() {
    if (this.disabled()) return;
    const next = !this.open();
    if (next && this.panelMode === 'fixed') {
      this.updatePanelPosition();
    }
    this.open.set(next);
    this.onTouched();
  }

  select(value: string) {
    this.value.set(value);
    this.onChange(value);
    this.onTouched();
    this.open.set(false);
  }

  writeValue(value: string | null): void {
    this.value.set(value || '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
    if (isDisabled) {
      this.open.set(false);
    }
  }

  private updatePanelPosition() {
    const rect = this.trigger?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    this.panelPosition.set({
      left: rect.left,
      top: rect.bottom + 8,
      width: rect.width
    });
  }
}
