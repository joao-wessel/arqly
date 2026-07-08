import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  title: string;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 1;

  success(title: string, message?: string) {
    this.show('success', title, message);
  }

  error(title: string, message?: string) {
    this.show('error', title, message);
  }

  info(title: string, message?: string) {
    this.show('info', title, message);
  }

  validation(message = 'Verifique os campos obrigatórios e tente novamente.') {
    this.error('Formulário incompleto', message);
  }

  dismiss(id: number) {
    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private show(type: ToastType, title: string, message?: string) {
    const toast = { id: this.nextId++, type, title, message };
    this.toasts.update((toasts) => [...toasts, toast]);
    window.setTimeout(() => this.dismiss(toast.id), 4200);
  }
}
