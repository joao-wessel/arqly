import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { LogoComponent } from '../../shared/components/logo.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-password-flow',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LogoComponent],
  template: `
    <main class="flex min-h-screen items-center justify-center p-6">
      <section class="card w-full max-w-md p-8">
        <app-logo />
        <h1 class="mt-10 text-3xl font-extrabold">{{ firstAccess ? 'Defina sua senha' : 'Redefinir senha' }}</h1>
        <p class="mt-2 text-sm text-slate-500">{{ firstAccess ? 'Use o token recebido no convite de primeiro acesso.' : 'Informe o token enviado para o seu e-mail.' }}</p>
        <form class="mt-8 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
          <input class="field" placeholder="Token" formControlName="token">
          <input class="field" type="password" placeholder="Nova senha" formControlName="password">
          @if (message) {
            <p class="rounded-xl bg-arqly-50 px-4 py-3 text-sm text-arqly-700">{{ message }}</p>
          }
          <button class="btn-primary w-full" type="submit">Salvar senha</button>
        </form>
        <a routerLink="/login" class="mt-5 inline-block text-sm font-semibold text-arqly-700">Voltar ao login</a>
      </section>
    </main>
  `
})
export class PasswordFlowComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  readonly firstAccess = !!this.route.snapshot.data['firstAccess'];
  readonly form = this.fb.nonNullable.group({
    token: [this.route.snapshot.queryParamMap.get('token') || '', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });
  message = '';

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.validation('Informe o token e uma senha com pelo menos 8 caracteres.');
      return;
    }
    const value = this.form.getRawValue();
    this.auth.resetPassword(value.token, value.password, this.firstAccess).subscribe({
      next: () => {
        this.message = 'Senha salva com sucesso. Você já pode fazer login.';
        this.toast.success('Senha salva', 'Você já pode fazer login.');
      },
      error: () => {
        this.message = 'Token inválido ou expirado.';
        this.toast.error('Token inválido', 'Solicite um novo token e tente novamente.');
      }
    });
  }
}
