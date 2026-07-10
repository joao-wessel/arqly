import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  template: `
    <section class="mx-auto max-w-3xl space-y-6">
      <div>
        <p class="text-sm font-bold uppercase tracking-[0.18em] text-arqly-600">Minha conta</p>
        <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Alterar senha</h2>
        <p class="mt-2 text-slate-500">Atualize sua senha de acesso mantendo sua conta protegida.</p>
      </div>

      <form class="card overflow-hidden" [formGroup]="form" (ngSubmit)="submit()">
        <div class="border-b border-slate-200 p-6">
          <div class="flex items-center gap-3">
            <div class="grid h-12 w-12 place-items-center rounded-2xl bg-arqly-50 text-arqly-700">
              <lucide-icon name="ShieldCheck" size="22"></lucide-icon>
            </div>
            <div>
              <h3 class="text-lg font-extrabold">Credenciais</h3>
              <p class="text-sm text-slate-500">Informe sua senha atual e escolha uma nova senha.</p>
            </div>
          </div>
        </div>

        <div class="space-y-5 p-6">
          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-1 md:col-span-2">
              <span class="text-xs font-bold text-slate-500">Senha atual <span class="text-red-500">*</span></span>
              <input class="field" type="password" placeholder="Senha atual" formControlName="currentPassword">
            </label>
            <label class="space-y-1">
              <span class="text-xs font-bold text-slate-500">Nova senha <span class="text-red-500">*</span></span>
              <input class="field" type="password" placeholder="Nova senha" formControlName="newPassword">
            </label>
            <label class="space-y-1">
              <span class="text-xs font-bold text-slate-500">Confirmar nova senha <span class="text-red-500">*</span></span>
              <input class="field" type="password" placeholder="Confirmar nova senha" formControlName="confirmPassword">
            </label>
          </div>

          @if (message) {
            <p class="rounded-xl bg-arqly-50 px-4 py-3 text-sm font-semibold text-arqly-700">{{ message }}</p>
          }

          <div class="flex justify-end gap-3">
            <button class="btn-primary" type="submit">
              <lucide-icon name="Save" size="18"></lucide-icon>
              Salvar nova senha
            </button>
          </div>
        </div>
      </form>
    </section>
  `
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
  });
  message = '';

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.validation('Informe a senha atual e uma nova senha com pelo menos 8 caracteres.');
      return;
    }
    const value = this.form.getRawValue();
    if (value.newPassword !== value.confirmPassword) {
      this.toast.error('Senhas diferentes', 'A confirmação precisa ser igual à nova senha.');
      return;
    }
    this.auth.changePassword(value.currentPassword, value.newPassword).subscribe({
      next: () => {
        this.form.reset();
        this.message = 'Senha alterada com sucesso.';
        this.toast.success('Senha alterada', 'Use a nova senha no próximo login.');
      },
      error: () => this.toast.error('Não foi possível alterar', 'Confira sua senha atual e tente novamente.')
    });
  }
}
