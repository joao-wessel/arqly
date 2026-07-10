import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthScope } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { LogoComponent } from '../../shared/components/logo.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LogoComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <section class="card w-full max-w-md p-8">
        <app-logo />
        <p class="mt-6 text-sm font-bold uppercase tracking-[0.18em] text-arqly-600">{{ scope === 'platform' ? 'Área administrativa' : 'Área do escritório' }}</p>
        <h1 class="mt-3 text-3xl font-extrabold">Recuperar senha</h1>
        <p class="mt-2 text-sm text-slate-500">Informe seu e-mail para receber um link seguro de redefinição.</p>
        <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
          <input class="field" type="email" placeholder="E-mail" formControlName="email">
          @if (message) {
            <p class="rounded-xl bg-arqly-50 px-4 py-3 text-sm text-arqly-700">{{ message }}</p>
          }
          <button class="btn-primary w-full" type="submit">Enviar instruções</button>
        </form>
        <a [routerLink]="scope === 'platform' ? '/login/admin' : '/login'" class="mt-5 inline-block text-sm font-semibold text-arqly-700">Voltar ao login</a>
      </section>
    </main>
  `
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  readonly scope = (this.route.snapshot.data['scope'] || 'tenant') as AuthScope;
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });
  message = '';

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.validation('Informe um e-mail válido para continuar.');
      return;
    }
    this.auth.forgotPassword(this.scope, this.form.getRawValue().email)
      .subscribe({
        next: () => {
          this.message = 'Se o e-mail existir, enviaremos as instruções.';
          this.toast.success('Solicitação enviada', 'Confira as instruções no e-mail informado.');
        },
        error: () => this.toast.error('Não foi possível enviar', 'Tente novamente em instantes.')
      });
  }
}
