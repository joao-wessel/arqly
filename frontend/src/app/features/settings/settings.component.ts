import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ToastService } from '../../shared/components/toast/toast.service';

type SettingsTab = 'general' | 'smtp';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  template: `
    <form class="space-y-6" [formGroup]="settingsForm" (ngSubmit)="saveAll()">
      <section class="card overflow-hidden">
        <div class="flex flex-col gap-5 border-b border-slate-200 p-6 lg:flex-row lg:items-center lg:justify-between">
          <div class="flex items-start gap-4">
            <div class="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-arqly-50 text-arqly-700">
              <lucide-icon name="Settings" size="24"></lucide-icon>
            </div>
            <div>
              <p class="text-sm font-bold uppercase tracking-[0.18em] text-arqly-600">Administração</p>
              <h2 class="mt-1 text-3xl font-extrabold tracking-tight">Configurações</h2>
              <p class="mt-2 max-w-2xl text-slate-500">Parâmetros globais da plataforma, URLs públicas e servidor de e-mail transacional.</p>
            </div>
          </div>
          <button class="btn-primary" type="submit">
            <lucide-icon name="Save" size="18"></lucide-icon>
            Salvar configurações
          </button>
        </div>

        <div class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-3 sm:grid-cols-2">
          <button class="flex items-center gap-3 rounded-2xl px-4 py-3 text-left transition"
                  type="button"
                  [class.bg-white]="activeTab() === 'general'"
                  [class.shadow-sm]="activeTab() === 'general'"
                  [class.text-arqly-700]="activeTab() === 'general'"
                  [class.text-slate-500]="activeTab() !== 'general'"
                  (click)="activeTab.set('general')">
            <span class="grid h-10 w-10 place-items-center rounded-xl bg-arqly-50 text-arqly-700">
              <lucide-icon name="SlidersHorizontal" size="18"></lucide-icon>
            </span>
            <span>
              <span class="block text-sm font-extrabold">Gerais</span>
              <span class="block text-xs">Nome, idioma e URLs</span>
            </span>
          </button>
          <button class="flex items-center gap-3 rounded-2xl px-4 py-3 text-left transition"
                  type="button"
                  [class.bg-white]="activeTab() === 'smtp'"
                  [class.shadow-sm]="activeTab() === 'smtp'"
                  [class.text-arqly-700]="activeTab() === 'smtp'"
                  [class.text-slate-500]="activeTab() !== 'smtp'"
                  (click)="activeTab.set('smtp')">
            <span class="grid h-10 w-10 place-items-center rounded-xl bg-arqly-50 text-arqly-700">
              <lucide-icon name="Mail" size="18"></lucide-icon>
            </span>
            <span>
              <span class="block text-sm font-extrabold">SMTP</span>
              <span class="block text-xs">Envio de convites e senhas</span>
            </span>
          </button>
        </div>
      </section>

      @if (activeTab() === 'general') {
        <section class="card overflow-hidden" formGroupName="general">
          <div class="border-b border-slate-200 p-6">
            <h3 class="text-xl font-extrabold">Configurações gerais</h3>
            <p class="mt-1 text-sm text-slate-500">Defina como a plataforma aparece e quais URLs serão usadas nos links enviados por e-mail.</p>
          </div>
          <div class="space-y-6 p-6">
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="Nome da plataforma" formControlName="platformName">
              <input class="field" placeholder="URL pública" formControlName="publicUrl">
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="URL frontend" formControlName="frontendUrl">
              <input class="field" placeholder="URL backend" formControlName="backendUrl">
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="Idioma" formControlName="language">
              <input class="field" placeholder="Timezone" formControlName="timezone">
            </div>
          </div>
        </section>
      }

      @if (activeTab() === 'smtp') {
        <section class="card overflow-hidden" formGroupName="smtp">
          <div class="flex flex-col gap-4 border-b border-slate-200 p-6 md:flex-row md:items-center md:justify-between">
            <div>
              <h3 class="text-xl font-extrabold">Servidor SMTP</h3>
              <p class="mt-1 text-sm text-slate-500">Usado para primeiro acesso, recuperação de senha e mensagens transacionais.</p>
            </div>
            <button class="btn-secondary" type="button" (click)="testSmtp()">
              <lucide-icon name="MailCheck" size="18"></lucide-icon>
              Testar envio
            </button>
          </div>
          <div class="space-y-6 p-6">
            <div class="grid gap-4 md:grid-cols-[1fr_12rem]">
              <input class="field" placeholder="Host" formControlName="host">
              <input class="field" type="number" placeholder="Porta" formControlName="port">
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="Usuário" formControlName="username">
              <input class="field" type="password" placeholder="Senha" formControlName="password">
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="E-mail remetente" formControlName="senderEmail">
              <input class="field" placeholder="Nome do remetente" formControlName="senderName">
            </div>
            <div class="grid gap-3 md:grid-cols-2">
              <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3">
                <span>
                  <span class="block text-sm font-bold text-slate-800">SSL</span>
                  <span class="mt-1 block text-xs text-slate-500">Conexão segura direta.</span>
                </span>
                <input class="checkbox" type="checkbox" formControlName="ssl">
              </label>
              <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3">
                <span>
                  <span class="block text-sm font-bold text-slate-800">TLS</span>
                  <span class="mt-1 block text-xs text-slate-500">Atualiza a conexão para modo seguro.</span>
                </span>
                <input class="checkbox" type="checkbox" formControlName="tls">
              </label>
            </div>
          </div>
        </section>
      }

      @if (message()) {
        <p class="rounded-xl bg-arqly-50 px-4 py-3 text-sm font-semibold text-arqly-700">{{ message() }}</p>
      }
    </form>
  `
})
export class SettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  readonly activeTab = signal<SettingsTab>('general');
  readonly message = signal('');
  readonly settingsForm = this.fb.nonNullable.group({
    general: this.fb.nonNullable.group({
      platformName: ['', Validators.required],
      publicUrl: ['', Validators.required],
      frontendUrl: ['', Validators.required],
      backendUrl: ['', Validators.required],
      language: ['', Validators.required],
      timezone: ['', Validators.required]
    }),
    smtp: this.fb.nonNullable.group({
      host: ['', Validators.required],
      port: [587, Validators.required],
      username: [''],
      password: [''],
      ssl: [false],
      tls: [true],
      senderEmail: ['', [Validators.required, Validators.email]],
      senderName: ['', Validators.required]
    })
  });

  ngOnInit() {
    this.http.get<ApiResponse<Record<string, unknown>>>('http://localhost:8080/api/platform/settings/general')
      .subscribe((response) => this.settingsForm.controls.general.patchValue(response.data));
    this.http.get<ApiResponse<Record<string, unknown>>>('http://localhost:8080/api/platform/settings/smtp')
      .subscribe((response) => this.settingsForm.controls.smtp.patchValue(response.data));
  }

  saveAll() {
    if (this.settingsForm.invalid) {
      this.settingsForm.markAllAsTouched();
      this.toast.validation('Preencha os campos obrigatórios das configurações.');
      return;
    }
    const value = this.settingsForm.getRawValue();
    this.http.put('http://localhost:8080/api/platform/settings/general', value.general).subscribe(() => {
      this.http.put('http://localhost:8080/api/platform/settings/smtp', value.smtp)
        .subscribe({
          next: () => {
            this.message.set('Configurações salvas.');
            this.toast.success('Configurações salvas', 'As alterações foram aplicadas.');
          },
          error: () => this.toast.error('Não foi possível salvar', 'Revise as configurações de SMTP.')
        });
    });
  }

  testSmtp() {
    if (this.settingsForm.controls.smtp.invalid) {
      this.settingsForm.controls.smtp.markAllAsTouched();
      this.toast.validation('Preencha os dados obrigatórios do SMTP antes de testar.');
      return;
    }
    const to = this.settingsForm.controls.smtp.getRawValue().senderEmail;
    this.http.post('http://localhost:8080/api/platform/settings/smtp/test', { to })
      .subscribe({
        next: () => {
          this.message.set('E-mail de teste enviado.');
          this.toast.success('E-mail de teste enviado');
        },
        error: () => {
          this.message.set('Não foi possível enviar o e-mail de teste.');
          this.toast.error('Falha no envio', 'Confira as credenciais do servidor SMTP.');
        }
      });
  }
}
