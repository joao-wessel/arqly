import { Component, Input, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-arqly-currency-input',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ArqlyCurrencyInputComponent),
      multi: true
    }
  ],
  template: `
    <input
      class="field"
      inputmode="numeric"
      autocomplete="off"
      [placeholder]="placeholder"
      [value]="displayValue()"
      [disabled]="disabled()"
      (keydown)="handleKeydown($event)"
      (paste)="handlePaste($event)"
      (input)="handleInput($any($event.target).value)"
      (blur)="onTouched()"
    />
  `,
  styles: [':host { display: block; width: 100%; }']
})
export class ArqlyCurrencyInputComponent implements ControlValueAccessor {
  @Input() placeholder = '0,00';

  readonly value = signal<number | null>(0);
  readonly disabled = signal(false);

  onTouched: () => void = () => {};
  private onChange: (value: number | null) => void = () => {};

  displayValue() {
    if (this.value() === null) return '';
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(this.value() || 0));
  }

  handleInput(raw: string) {
    const digits = raw.replace(/\D/g, '');
    const value = digits ? Number(digits) / 100 : null;
    this.value.set(value);
    this.onChange(value);
  }

  handleKeydown(event: KeyboardEvent) {
    const allowed = ['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];
    if (allowed.includes(event.key) || event.ctrlKey || event.metaKey) return;
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  handlePaste(event: ClipboardEvent) {
    const text = event.clipboardData?.getData('text') || '';
    if (/\D/.test(text)) {
      event.preventDefault();
      this.handleInput(text);
    }
  }

  writeValue(value: number | string | null): void {
    this.value.set(value === null || value === '' ? null : Number(value));
  }

  registerOnChange(fn: (value: number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}
