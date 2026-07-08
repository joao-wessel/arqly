import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

interface Tenant {
  id: string;
  tradeName: string;
  legalName: string;
  cnpj: string;
  primaryEmail: string;
  phone: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  status: string;
  createdAt: string;
}

interface Page<T> {
  content: T[];
}

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LucideAngularModule, ArqlySelectComponent],
  template: `
    <section class="card overflow-hidden">
      <div class="flex flex-col gap-4 border-b border-slate-200 p-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 class="text-3xl font-extrabold tracking-tight">Tenants</h2>
          <p class="mt-2 text-slate-500">Escritórios cadastrados na plataforma Arqly.</p>
        </div>
        <button class="btn-primary" type="button" (click)="openTenantModal()">
          <lucide-icon name="Plus" size="18"></lucide-icon>
          Novo tenant
        </button>
      </div>

      <div class="hidden overflow-x-auto md:block">
        <table class="w-full min-w-[980px] text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th class="px-6 py-4">Nome fantasia</th>
              <th class="px-6 py-4">CNPJ</th>
              <th class="px-6 py-4">E-mail</th>
              <th class="px-6 py-4">Cidade/UF</th>
              <th class="px-6 py-4">Status</th>
              <th class="px-6 py-4">Criado em</th>
              <th class="px-6 py-4 text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            @for (tenant of tenants(); track tenant.id) {
              <tr class="border-t border-slate-100">
                <td class="px-6 py-4">
                  <p class="font-bold">{{ tenant.tradeName }}</p>
                  <p class="text-xs text-slate-500">{{ tenant.legalName }}</p>
                </td>
                <td class="px-6 py-4 text-slate-600">{{ tenant.cnpj }}</td>
                <td class="px-6 py-4 text-slate-600">{{ tenant.primaryEmail }}</td>
                <td class="px-6 py-4 text-slate-600">{{ tenant.city || '-' }}{{ tenant.state ? '/' + tenant.state : '' }}</td>
                <td class="px-6 py-4">
                  <span class="rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ tenant.status }}</span>
                </td>
                <td class="px-6 py-4 text-slate-500">{{ tenant.createdAt | date:'dd/MM/yyyy' }}</td>
                <td class="px-6 py-4">
                  <div class="flex justify-end gap-2">
                    <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openTenantModal(tenant)">
                      <lucide-icon name="Pencil" size="16"></lucide-icon>
                    </button>
                    <button class="btn-secondary px-3 py-2" type="button" title="Gerar primeiro acesso" (click)="openFirstAccessModal(tenant)">
                      <lucide-icon name="KeyRound" size="16"></lucide-icon>
                    </button>
                    <button class="btn-secondary px-3 py-2 text-red-600 hover:border-red-200 hover:text-red-700" type="button" title="Excluir" (click)="openDeleteModal(tenant)">
                      <lucide-icon name="Trash2" size="16"></lucide-icon>
                    </button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="px-6 py-12 text-center text-slate-500">Nenhum tenant cadastrado.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="space-y-3 p-4 md:hidden">
        @for (tenant of tenants(); track tenant.id) {
          <article class="rounded-2xl border border-slate-200 bg-white p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate font-extrabold">{{ tenant.tradeName }}</p>
                <p class="mt-1 truncate text-xs text-slate-500">{{ tenant.legalName }}</p>
              </div>
              <span class="shrink-0 rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ tenant.status }}</span>
            </div>
            <div class="mt-4 space-y-1 text-sm text-slate-600">
              <p>{{ tenant.primaryEmail }}</p>
              <p>{{ tenant.cnpj }}</p>
              <p>{{ tenant.city || '-' }}{{ tenant.state ? '/' + tenant.state : '' }}</p>
            </div>
            <div class="mt-4 grid grid-cols-3 gap-2">
              <button class="btn-secondary px-3 py-2" type="button" (click)="openTenantModal(tenant)">
                <lucide-icon name="Pencil" size="16"></lucide-icon>
                Editar
              </button>
              <button class="btn-secondary px-3 py-2" type="button" (click)="openFirstAccessModal(tenant)">
                <lucide-icon name="KeyRound" size="16"></lucide-icon>
                Acesso
              </button>
              <button class="btn-secondary px-3 py-2 text-red-600 hover:border-red-200 hover:text-red-700" type="button" (click)="openDeleteModal(tenant)">
                <lucide-icon name="Trash2" size="16"></lucide-icon>
                Excluir
              </button>
            </div>
          </article>
        } @empty {
          <p class="py-8 text-center text-sm text-slate-500">Nenhum tenant cadastrado.</p>
        }
      </div>
    </section>

    @if (tenantModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-3xl space-y-5 p-6" [formGroup]="tenantForm" (ngSubmit)="saveTenant()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ editingTenant() ? 'Editar tenant' : 'Novo tenant' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeTenantModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <input class="field" placeholder="Nome fantasia" formControlName="tradeName">
            <input class="field" placeholder="Razão social" formControlName="legalName">
            <input class="field" placeholder="CNPJ" formControlName="cnpj">
            <input class="field" placeholder="E-mail principal" formControlName="primaryEmail">
            <input class="field" placeholder="Telefone" formControlName="phone">
            <app-arqly-select
              formControlName="status"
              placeholder="Status"
              [options]="tenantStatusOptions"
            />
            <input class="field md:col-span-2" placeholder="Endereço" formControlName="address">
            <input class="field" placeholder="Cidade" formControlName="city">
            <input class="field" placeholder="Estado" formControlName="state">
            <input class="field" placeholder="CEP" formControlName="zipCode">
          </div>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeTenantModal()">Cancelar</button>
            <button class="btn-primary" type="submit">
              <lucide-icon name="Save" size="18"></lucide-icon>
              Salvar
            </button>
          </div>
        </form>
      </div>
    }

    @if (firstAccessModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-md space-y-5 p-6" [formGroup]="firstAccessForm" (ngSubmit)="generateFirstAccess()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">Gerar primeiro acesso</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeFirstAccessModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <p class="text-sm text-slate-500">Informe o e-mail do administrador do tenant {{ selectedTenant()?.tradeName }}.</p>
          <input class="field" type="email" placeholder="E-mail" formControlName="email">
          @if (firstAccessUrl()) {
            <p class="break-all rounded-xl bg-arqly-50 p-3 text-xs text-arqly-800">{{ firstAccessUrl() }}</p>
          }
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeFirstAccessModal()">Cancelar</button>
            <button class="btn-primary" type="submit">
              <lucide-icon name="Send" size="18"></lucide-icon>
              Gerar
            </button>
          </div>
        </form>
      </div>
    }

    @if (deleteModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <div class="modal-panel card w-full max-w-md space-y-5 p-6">
          <div class="flex items-center gap-3">
            <div class="grid h-12 w-12 place-items-center rounded-full bg-red-50 text-red-600">
              <lucide-icon name="Trash2" size="22"></lucide-icon>
            </div>
            <div>
              <h3 class="text-xl font-extrabold">Excluir tenant</h3>
              <p class="text-sm text-slate-500">Esta ação não pode ser desfeita.</p>
            </div>
          </div>
          <p class="text-sm text-slate-600">Deseja excluir <strong>{{ selectedTenant()?.tradeName }}</strong>?</p>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeDeleteModal()">Cancelar</button>
            <button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="deleteTenant()">Excluir</button>
          </div>
        </div>
      </div>
    }
  `
})
export class TenantsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  readonly tenants = signal<Tenant[]>([]);
  readonly tenantModalOpen = signal(false);
  readonly firstAccessModalOpen = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly editingTenant = signal<Tenant | null>(null);
  readonly selectedTenant = signal<Tenant | null>(null);
  readonly firstAccessUrl = signal('');
  readonly tenantForm = this.fb.nonNullable.group({
    tradeName: ['', Validators.required],
    legalName: ['', Validators.required],
    cnpj: ['', Validators.required],
    primaryEmail: ['', [Validators.required, Validators.email]],
    phone: [''],
    address: [''],
    city: [''],
    state: [''],
    zipCode: [''],
    status: ['ACTIVE']
  });
  readonly firstAccessForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });
  readonly tenantStatusOptions = [
    { label: 'Ativo', value: 'ACTIVE' },
    { label: 'Inativo', value: 'INACTIVE' },
    { label: 'Suspenso', value: 'SUSPENDED' }
  ];

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<ApiResponse<Page<Tenant>>>('http://localhost:8080/api/platform/tenants')
      .subscribe((response) => this.tenants.set(response.data.content));
  }

  openTenantModal(tenant?: Tenant) {
    this.editingTenant.set(tenant || null);
    this.tenantForm.reset({
      tradeName: tenant?.tradeName || '',
      legalName: tenant?.legalName || '',
      cnpj: tenant?.cnpj || '',
      primaryEmail: tenant?.primaryEmail || '',
      phone: tenant?.phone || '',
      address: tenant?.address || '',
      city: tenant?.city || '',
      state: tenant?.state || '',
      zipCode: tenant?.zipCode || '',
      status: tenant?.status || 'ACTIVE'
    });
    this.tenantModalOpen.set(true);
  }

  closeTenantModal() {
    this.tenantModalOpen.set(false);
  }

  saveTenant() {
    if (this.tenantForm.invalid) {
      this.tenantForm.markAllAsTouched();
      this.toast.validation('Preencha os dados obrigatórios do tenant.');
      return;
    }
    const tenant = this.editingTenant();
    const request = tenant
      ? this.http.put<ApiResponse<Tenant>>(`http://localhost:8080/api/platform/tenants/${tenant.id}`, this.tenantForm.getRawValue())
      : this.http.post<ApiResponse<Tenant>>('http://localhost:8080/api/platform/tenants', this.tenantForm.getRawValue());
    request.subscribe({
      next: () => {
        this.closeTenantModal();
        this.load();
        this.toast.success(tenant ? 'Tenant atualizado' : 'Tenant criado', 'As informações foram salvas.');
      },
      error: () => this.toast.error('Não foi possível salvar', 'Revise os dados e tente novamente.')
    });
  }

  openFirstAccessModal(tenant: Tenant) {
    this.selectedTenant.set(tenant);
    this.firstAccessUrl.set('');
    this.firstAccessForm.reset({ email: tenant.primaryEmail || '' });
    this.firstAccessModalOpen.set(true);
  }

  closeFirstAccessModal() {
    this.firstAccessModalOpen.set(false);
  }

  generateFirstAccess() {
    if (this.firstAccessForm.invalid) {
      this.firstAccessForm.markAllAsTouched();
      this.toast.validation('Informe um e-mail válido para gerar o primeiro acesso.');
      return;
    }
    const tenant = this.selectedTenant();
    if (!tenant) return;
    const email = this.firstAccessForm.getRawValue().email;
    const name = email.split('@')[0].replace(/[._-]+/g, ' ') || 'Administrador';
    this.http.post<ApiResponse<{ firstAccessUrl: string }>>(`http://localhost:8080/api/platform/tenants/${tenant.id}/admins`, {
      name,
      email
    }).subscribe({
      next: (response) => {
        this.firstAccessUrl.set(response.data.firstAccessUrl);
        this.toast.success('Primeiro acesso gerado', 'O link foi criado para o usuário informado.');
      },
      error: () => this.toast.error('Não foi possível gerar', 'Talvez este e-mail já esteja cadastrado.')
    });
  }

  openDeleteModal(tenant: Tenant) {
    this.selectedTenant.set(tenant);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
  }

  deleteTenant() {
    const tenant = this.selectedTenant();
    if (!tenant) return;
    this.http.delete(`http://localhost:8080/api/platform/tenants/${tenant.id}`).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.load();
        this.toast.success('Tenant excluído');
      },
      error: () => this.toast.error('Não foi possível excluir', 'Remova usuários vinculados antes de excluir o tenant.')
    });
  }
}
