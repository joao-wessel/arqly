import { Component, ElementRef, HostListener, Input, ViewChild, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-arqly-date-picker',
  standalone: true,
  imports: [LucideAngularModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ArqlyDatePickerComponent),
      multi: true
    }
  ],
  template: `
    <div class="relative" (click)="$event.stopPropagation()">
      <div
        #trigger
        class="field flex items-center justify-between gap-3 text-left"
        role="button"
        tabindex="0"
        [class.cursor-not-allowed]="disabled()"
        [class.bg-slate-50]="disabled()"
        [attr.aria-expanded]="open()"
        (click)="toggle($event)"
        (keydown.enter)="toggle($event)"
        (keydown.space)="toggle($event)"
      >
        <span [class.text-slate-400]="!value()">{{ value() ? displayValue() : placeholder }}</span>
        <lucide-icon name="CalendarDays" size="18" class="shrink-0 text-arqly-700"></lucide-icon>
      </div>

      @if (open()) {
        <div
          class="z-[75] rounded-3xl border border-slate-200 bg-white p-3 shadow-[0_24px_70px_rgba(15,23,42,0.18)]"
          [class.fixed]="panelMode === 'fixed'"
          [class.absolute]="panelMode === 'absolute'"
          [class.left-0]="panelMode === 'absolute'"
          [class.mt-2]="panelMode === 'absolute'"
          [style.width]="panelMode === 'absolute' ? '20rem' : null"
          [style.left.px]="panelMode === 'fixed' ? panelPosition().left : null"
          [style.top.px]="panelMode === 'fixed' ? panelPosition().top : null"
          [style.width.px]="panelMode === 'fixed' ? panelPosition().width : null"
          (click)="$event.stopPropagation()"
          (mousedown)="$event.stopPropagation()"
        >
          <div class="flex items-center justify-between gap-2 px-1 py-1">
            <div class="rounded-xl p-2 text-slate-500 transition hover:bg-arqly-50 hover:text-arqly-700" role="button" tabindex="0" (click)="previousMonth()" (keydown.enter)="previousMonth()" (keydown.space)="previousMonth()">
              <lucide-icon name="ChevronLeft" size="18"></lucide-icon>
            </div>
            <strong class="text-sm">{{ monthLabel() }} {{ viewYear() }}</strong>
            <div class="rounded-xl p-2 text-slate-500 transition hover:bg-arqly-50 hover:text-arqly-700" role="button" tabindex="0" (click)="nextMonth()" (keydown.enter)="nextMonth()" (keydown.space)="nextMonth()">
              <lucide-icon name="ChevronRight" size="18"></lucide-icon>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-7 gap-1 text-center text-[11px] font-extrabold uppercase text-slate-400">
            @for (day of weekDays; track day) {
              <span class="py-1">{{ day }}</span>
            }
          </div>

          <div class="mt-1 grid grid-cols-7 gap-1">
            @for (day of days(); track day.key) {
              <div
                class="grid h-9 place-items-center rounded-xl text-sm font-bold transition hover:bg-arqly-50 hover:text-arqly-700"
                role="button"
                tabindex="0"
                [class.text-slate-300]="!day.currentMonth"
                [class.text-slate-600]="day.currentMonth && !isSelected(day.date)"
                [class.bg-arqly-600]="isSelected(day.date)"
                [class.text-white]="isSelected(day.date)"
                (click)="select(day.date)"
                (keydown.enter)="select(day.date)"
                (keydown.space)="select(day.date)"
              >
                {{ day.date.getDate() }}
              </div>
            }
          </div>

          <div class="mt-3 flex justify-between border-t border-slate-100 pt-3">
            <div class="rounded-xl px-3 py-2 text-xs font-bold text-slate-500 transition hover:bg-slate-50" role="button" tabindex="0" (click)="clear()" (keydown.enter)="clear()" (keydown.space)="clear()">Limpar</div>
            <div class="rounded-xl px-3 py-2 text-xs font-bold text-arqly-700 transition hover:bg-arqly-50" role="button" tabindex="0" (click)="selectToday()" (keydown.enter)="selectToday()" (keydown.space)="selectToday()">Hoje</div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [':host { display: block; width: 100%; }']
})
export class ArqlyDatePickerComponent implements ControlValueAccessor {
  @Input() placeholder = 'Selecione uma data';
  @Input() panelMode: 'absolute' | 'fixed' = 'fixed';
  @ViewChild('trigger') trigger?: ElementRef<HTMLElement>;

  readonly weekDays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
  readonly open = signal(false);
  readonly value = signal('');
  readonly disabled = signal(false);
  readonly viewDate = signal(new Date());
  readonly panelPosition = signal({ left: 0, top: 0, width: 320 });

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

  toggle(event?: Event) {
    event?.preventDefault();
    event?.stopPropagation();
    if (this.disabled()) return;
    const next = !this.open();
    if (next) {
      this.syncViewDate();
      this.updatePanelPosition();
    }
    this.open.set(next);
    this.onTouched();
  }

  displayValue() {
    const [year, month, day] = this.value().split('-');
    return year && month && day ? `${day}/${month}/${year}` : '';
  }

  monthLabel() {
    return this.viewDate().toLocaleDateString('pt-BR', { month: 'long' });
  }

  viewYear() {
    return this.viewDate().getFullYear();
  }

  days() {
    const view = this.viewDate();
    const first = new Date(view.getFullYear(), view.getMonth(), 1);
    const start = new Date(first);
    start.setDate(first.getDate() - first.getDay());
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      return {
        key: this.toValue(date),
        date,
        currentMonth: date.getMonth() === view.getMonth()
      };
    });
  }

  previousMonth() {
    const date = new Date(this.viewDate());
    date.setMonth(date.getMonth() - 1);
    this.viewDate.set(date);
  }

  nextMonth() {
    const date = new Date(this.viewDate());
    date.setMonth(date.getMonth() + 1);
    this.viewDate.set(date);
  }

  select(date: Date) {
    const value = this.toValue(date);
    this.value.set(value);
    this.onChange(value);
    this.onTouched();
    this.open.set(false);
  }

  selectToday() {
    this.select(new Date());
  }

  clear() {
    this.value.set('');
    this.onChange('');
    this.onTouched();
    this.open.set(false);
  }

  isSelected(date: Date) {
    return this.value() === this.toValue(date);
  }

  writeValue(value: string | null): void {
    this.value.set(value || '');
    this.syncViewDate();
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

  private syncViewDate() {
    const parsed = this.fromValue(this.value());
    this.viewDate.set(parsed || new Date());
  }

  private updatePanelPosition() {
    const rect = this.trigger?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    const margin = 12;
    const gap = 8;
    const panelWidth = Math.max(300, rect.width);
    const panelHeight = 372;
    const belowTop = rect.bottom + gap;
    const aboveTop = rect.top - panelHeight - gap;
    const fitsBelow = belowTop + panelHeight <= window.innerHeight - margin;
    const top = fitsBelow || aboveTop < margin
      ? Math.min(belowTop, Math.max(margin, window.innerHeight - panelHeight - margin))
      : aboveTop;

    this.panelPosition.set({
      left: Math.max(margin, Math.min(rect.left, window.innerWidth - panelWidth - margin)),
      top: Math.max(margin, top),
      width: panelWidth
    });
  }

  private fromValue(value: string) {
    const [year, month, day] = value.split('-').map(Number);
    if (!year || !month || !day) return null;
    return new Date(year, month - 1, day);
  }

  private toValue(date: Date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
