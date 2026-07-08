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
    <form class="card overflow-hidden" [formGroup]="settingsForm" (ngSubmit)="saveAll()">
      <div class="flex flex-col gap-4 border-b border-slate-200 p-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 class="text-3xl font-extrabold tracking-tight">Configurações</h2>
          <p class="mt-2 text-slate-500">Parâmetros globais da plataforma e servidor SMTP.</p>
        </div>
        <button class="btn-primary" type="submit">
          <lucide-icon name="Save" size="18"></lucide-icon>
          Salvar configurações
        </button>
      </div>

      <div class="border-b border-slate-200 px-6">
        <div class="flex gap-2">
          <button class="border-b-2 px-4 py-4 text-sm font-bold transition"
                  type="button"
                  [class.border-arqly-600]="activeTab() === 'general'"
                  [class.text-arqly-700]="activeTab() === 'general'"
                  [class.border-transparent]="activeTab() !== 'general'"
                  [class.text-slate-500]="activeTab() !== 'general'"
                  (click)="activeTab.set('general')">
            Gerais
          </button>
          <button class="border-b-2 px-4 py-4 text-sm font-bold transition"
                  type="button"
                  [class.border-arqly-600]="activeTab() === 'smtp'"
                  [class.text-arqly-700]="activeTab() === 'smtp'"
                  [class.border-transparent]="activeTab() !== 'smtp'"
                  [class.text-slate-500]="activeTab() !== 'smtp'"
                  (click)="activeTab.set('smtp')">
            SMTP
          </button>
        </div>
      </div>

      <div class="p-6">
        @if (activeTab() === 'general') {
          <div class="grid gap-4 md:grid-cols-2" formGroupName="general">
            <input class="field" placeholder="Nome da plataforma" formControlName="platformName">
            <input class="field" placeholder="URL pública" formControlName="publicUrl">
            <input class="field" placeholder="URL frontend" formControlName="frontendUrl">
            <input class="field" placeholder="URL backend" formControlName="backendUrl">
            <input class="field" placeholder="Idioma" formControlName="language">
            <input class="field" placeholder="Timezone" formControlName="timezone">
          </div>
        }

        @if (activeTab() === 'smtp') {
          <div class="space-y-5" formGroupName="smtp">
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="Host" formControlName="host">
              <input class="field" type="number" placeholder="Porta" formControlName="port">
              <input class="field" placeholder="Usuário" formControlName="username">
              <input class="field" type="password" placeholder="Senha" formControlName="password">
              <input class="field" placeholder="E-mail remetente" formControlName="senderEmail">
              <input class="field" placeholder="Nome do remetente" formControlName="senderName">
            </div>
            <div class="flex gap-4 text-sm">
              <label class="flex items-center gap-2 font-semibold text-slate-600"><input class="checkbox" type="checkbox" formControlName="ssl"> SSL</label>
              <label class="flex items-center gap-2 font-semibold text-slate-600"><input class="checkbox" type="checkbox" formControlName="tls"> TLS</label>
            </div>
            <button class="btn-secondary" type="button" (click)="testSmtp()">
              <lucide-icon name="Mail" size="18"></lucide-icon>
              Testar envio
            </button>
          </div>
        }

        @if (message()) {
          <p class="mt-6 rounded-xl bg-arqly-50 px-4 py-3 text-sm text-arqly-700">{{ message() }}</p>
        }
      </div>
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
