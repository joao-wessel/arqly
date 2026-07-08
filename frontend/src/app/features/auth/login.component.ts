import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthScope } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { LogoComponent } from '../../shared/components/logo.component';
import { ToastService } from '../../shared/components/toast/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LucideAngularModule, LogoComponent],
  template: `
    <main class="grid min-h-screen lg:grid-cols-[1fr_0.9fr]">
      <section class="flex items-center justify-center p-6">
        <div class="w-full max-w-md">
          <app-logo />
          <div class="mt-12">
            <p class="text-sm font-bold uppercase tracking-[0.2em] text-arqly-600">{{ scope === 'platform' ? 'Área administrativa' : 'Área do escritório' }}</p>
            <h1 class="mt-3 text-4xl font-extrabold tracking-tight">Entre no Arqly</h1>
            <p class="mt-3 text-slate-500">Gestão de arquitetura com uma base limpa, segura e preparada para crescer.</p>
          </div>

          <form class="mt-8 space-y-4" [formGroup]="form" (ngSubmit)="submit()">
            <input class="field" type="email" placeholder="E-mail" formControlName="email">
            <input class="field" type="password" placeholder="Senha" formControlName="password">
            @if (error) {
              <p class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{{ error }}</p>
            }
            <button class="btn-primary w-full" type="submit" [disabled]="loading">
              <lucide-icon name="LogIn" size="18"></lucide-icon>
              {{ loading ? 'Entrando...' : 'Entrar' }}
            </button>
          </form>

          @if (scope === 'tenant') {
            <a routerLink="/forgot-password" class="mt-5 inline-block text-sm font-semibold text-arqly-700">Esqueci minha senha</a>
          }
        </div>
      </section>

      <section class="hidden bg-arqly-700 p-8 lg:block">
        <div class="relative flex h-full overflow-hidden rounded-[2rem] bg-gradient-to-br from-arqly-700 to-arqly-900 p-10 text-white shadow-2xl">
          <div class="absolute inset-0 opacity-20"
               style="background-image: linear-gradient(rgba(255,255,255,.22) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.22) 1px, transparent 1px); background-size: 42px 42px;"></div>
          <div class="relative z-10 grid w-full place-items-center">
            <div class="w-full max-w-xl rounded-[1.75rem] border border-white/15 bg-white/12 p-6 backdrop-blur">
              <div class="aspect-[4/3] rounded-3xl bg-white p-5 text-slate-900">
                <div class="grid h-full grid-cols-[1.2fr_0.8fr] gap-4">
                  <div class="rounded-2xl border border-slate-200 p-4">
                    <div class="h-28 rounded-xl bg-gradient-to-br from-slate-100 to-arqly-100"></div>
                    <div class="mt-5 h-3 w-2/3 rounded-full bg-slate-200"></div>
                    <div class="mt-3 h-3 w-1/2 rounded-full bg-slate-100"></div>
                    <div class="mt-8 space-y-3">
                      <div class="h-2 rounded-full bg-arqly-600"></div>
                      <div class="h-2 w-4/5 rounded-full bg-slate-200"></div>
                      <div class="h-2 w-3/5 rounded-full bg-slate-200"></div>
                    </div>
                  </div>
                  <div class="space-y-4">
                    @for (item of previewItems; track item.title) {
                      <div class="rounded-2xl bg-slate-50 p-4">
                        <div class="flex items-center justify-between">
                          <span class="text-xs font-bold text-slate-500">{{ item.kicker }}</span>
                          <span class="h-2 w-2 rounded-full bg-arqly-600"></span>
                        </div>
                        <p class="mt-3 text-sm font-bold">{{ item.title }}</p>
                        <p class="mt-1 text-xs text-slate-500">{{ item.text }}</p>
                      </div>
                    }
                  </div>
                </div>
              </div>
              <div class="mt-6 max-w-md">
                <div>
                  <h2 class="text-3xl font-extrabold tracking-tight">Projetos com clareza.</h2>
                  <p class="mt-2 max-w-sm text-sm text-white/70">Uma operação visual, organizada e pronta para os próximos módulos do escritório.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  readonly scope = (this.route.snapshot.data['scope'] || 'tenant') as AuthScope;
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });
  loading = false;
  error = '';
  previewItems = [
    { kicker: 'Briefing', title: 'Residência Vila Nova', text: 'Escopo e equipe alinhados' },
    { kicker: 'Entrega', title: 'Compatibilização', text: 'Checklist preparado' },
    { kicker: 'Cliente', title: 'Studio Concept', text: 'Portal em construção' }
  ];

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.validation('Informe e-mail e senha para continuar.');
      return;
    }
    this.loading = true;
    this.error = '';
    const { email, password } = this.form.getRawValue();
    this.auth.login(this.scope, email, password).subscribe({
      next: () => {
        this.toast.success('Login realizado', 'Bem-vindo ao Arqly.');
        void this.router.navigateByUrl(this.scope === 'platform' ? '/admin/dashboard' : '/app/dashboard');
      },
      error: () => {
        this.error = 'Não foi possível autenticar com esses dados.';
        this.toast.error('Falha no login', 'Verifique e-mail e senha.');
        this.loading = false;
      }
    });
  }
}
