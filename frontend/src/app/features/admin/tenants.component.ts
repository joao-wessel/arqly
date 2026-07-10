import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type PersonType = 'NATURAL_PERSON' | 'LEGAL_ENTITY';

interface Tenant {
  id: string;
  tradeName: string;
  legalName: string;
  personType: PersonType;
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

interface ViaCepResponse {
  cep: string;
  logradouro: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean;
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
              <th class="px-6 py-4">CPF/CNPJ</th>
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
              <p>{{ personTypeLabel(tenant.personType) }} · {{ tenant.cnpj }}</p>
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

          <section class="space-y-3">
            <div>
              <p class="text-sm font-extrabold text-slate-900">Dados primários</p>
              <p class="mt-1 text-xs text-slate-500">Identificação principal do tenant e contatos.</p>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <app-arqly-select
                formControlName="personType"
                placeholder="Tipo de pessoa"
                [options]="personTypeOptions"
              />
              <input class="field" placeholder="Nome fantasia" formControlName="tradeName">
              <input class="field" placeholder="Razão social" formControlName="legalName">
              <input
                class="field"
                [placeholder]="documentPlaceholder()"
                [attr.maxlength]="tenantForm.controls.personType.value === 'NATURAL_PERSON' ? 14 : 18"
                inputmode="numeric"
                formControlName="cnpj"
                (input)="onDocumentInput($event)"
              >
              <input class="field" placeholder="E-mail principal" formControlName="primaryEmail">
              <input class="field" placeholder="Telefone" inputmode="tel" maxlength="15" formControlName="phone" (input)="onPhoneInput($event)">
              <app-arqly-select
                formControlName="status"
                placeholder="Status"
                [options]="tenantStatusOptions"
              />
            </div>
          </section>

          <section class="space-y-3 border-t border-slate-200 pt-5">
            <div>
              <p class="text-sm font-extrabold text-slate-900">Endereço</p>
              <p class="mt-1 text-xs text-slate-500">Informe o CEP para preencher endereço, cidade e estado automaticamente.</p>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <input class="field" placeholder="CEP" inputmode="numeric" maxlength="9" formControlName="zipCode" (input)="onZipCodeInput($event)">
              <input class="field md:col-span-2" placeholder="Endereço" formControlName="address">
              <input class="field" placeholder="Cidade" formControlName="city">
              <input class="field" placeholder="Estado" formControlName="state">
            </div>
          </section>

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
            <h3 class="text-xl font-extrabold">Enviar primeiro acesso</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeFirstAccessModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <p class="text-sm text-slate-500">Informe o e-mail do administrador do tenant {{ selectedTenant()?.tradeName }}. Um novo link invalida o anterior e expira em 1 hora.</p>
          <input class="field" type="email" placeholder="E-mail" formControlName="email">
          @if (firstAccessUrl()) {
            <p class="rounded-xl bg-arqly-50 p-3 text-sm font-semibold text-arqly-800">Link enviado por e-mail com validade de 1 hora.</p>
          }
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeFirstAccessModal()">Cancelar</button>
            <button class="btn-primary" type="submit">
              <lucide-icon name="Send" size="18"></lucide-icon>
              Enviar
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
  private lastZipCodeLookup = '';

  readonly tenants = signal<Tenant[]>([]);
  readonly tenantModalOpen = signal(false);
  readonly firstAccessModalOpen = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly editingTenant = signal<Tenant | null>(null);
  readonly selectedTenant = signal<Tenant | null>(null);
  readonly firstAccessUrl = signal('');

  readonly tenantForm = this.fb.nonNullable.group({
    personType: ['LEGAL_ENTITY' as PersonType, Validators.required],
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
  readonly personTypeOptions = [
    { label: 'Pessoa jurídica', value: 'LEGAL_ENTITY' },
    { label: 'Pessoa física', value: 'NATURAL_PERSON' }
  ];

  ngOnInit() {
    this.load();
    this.tenantForm.controls.personType.valueChanges.subscribe(() => {
      this.tenantForm.controls.cnpj.setValue(this.formatDocument(this.tenantForm.controls.cnpj.value), { emitEvent: false });
    });
  }

  load() {
    this.http.get<ApiResponse<Page<Tenant>>>('http://localhost:8080/api/platform/tenants')
      .subscribe((response) => this.tenants.set(response.data.content));
  }

  openTenantModal(tenant?: Tenant) {
    this.editingTenant.set(tenant || null);
    this.lastZipCodeLookup = this.onlyDigits(tenant?.zipCode || '');
    this.tenantForm.reset({
      personType: tenant?.personType || 'LEGAL_ENTITY',
      tradeName: tenant?.tradeName || '',
      legalName: tenant?.legalName || '',
      cnpj: tenant?.cnpj || '',
      primaryEmail: tenant?.primaryEmail || '',
      phone: tenant?.phone || '',
      address: tenant?.address || '',
      city: tenant?.city || '',
      state: tenant?.state || '',
      zipCode: this.formatZipCode(tenant?.zipCode || ''),
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
      this.toast.validation('Informe um e-mail válido para enviar o primeiro acesso.');
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
        this.toast.success('Primeiro acesso enviado', 'O link anterior foi invalidado e o novo expira em 1 hora.');
      },
      error: () => this.toast.error('Não foi possível enviar', 'Verifique as configurações de SMTP e tente novamente.')
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

  documentPlaceholder() {
    return this.tenantForm.controls.personType.value === 'NATURAL_PERSON' ? 'CPF' : 'CNPJ';
  }

  personTypeLabel(type: PersonType) {
    return type === 'NATURAL_PERSON' ? 'Pessoa física' : 'Pessoa jurídica';
  }

  onDocumentInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = this.formatDocument(input.value);
    this.tenantForm.controls.cnpj.setValue(value, { emitEvent: false });
    input.value = value;
  }

  onPhoneInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = this.formatPhone(input.value);
    this.tenantForm.controls.phone.setValue(value, { emitEvent: false });
    input.value = value;
  }

  onZipCodeInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = this.formatZipCode(input.value);
    this.tenantForm.controls.zipCode.setValue(value, { emitEvent: false });
    input.value = value;
    this.lookupZipCode(value);
  }

  private lookupZipCode(value: string) {
    const digits = this.onlyDigits(value);
    if (digits.length !== 8 || digits === this.lastZipCodeLookup) return;

    this.lastZipCodeLookup = digits;
    this.http.get<ViaCepResponse>(`https://viacep.com.br/ws/${digits}/json/`).subscribe({
      next: (response) => {
        if (response.erro) {
          this.toast.error('CEP não encontrado', 'Confira o CEP informado.');
          return;
        }
        const address = [response.logradouro, response.bairro].filter(Boolean).join(' - ');
        this.tenantForm.patchValue({
          address,
          city: response.localidade || '',
          state: response.uf || ''
        }, { emitEvent: false });
      },
      error: () => this.toast.error('Não foi possível consultar o CEP', 'Preencha o endereço manualmente.')
    });
  }

  private formatDocument(value: string) {
    const digits = this.onlyDigits(value);
    if (this.tenantForm.controls.personType.value === 'NATURAL_PERSON') {
      return digits
        .slice(0, 11)
        .replace(/^(\d{3})(\d)/, '$1.$2')
        .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
        .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4');
    }
    return digits
      .slice(0, 14)
      .replace(/^(\d{2})(\d)/, '$1.$2')
      .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4')
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5');
  }

  private formatPhone(value: string) {
    const digits = this.onlyDigits(value).slice(0, 11);
    if (digits.length <= 10) {
      return digits
        .replace(/^(\d{2})(\d)/, '($1) $2')
        .replace(/(\d{4})(\d)/, '$1-$2');
    }
    return digits
      .replace(/^(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{5})(\d)/, '$1-$2');
  }

  private formatZipCode(value: string) {
    return this.onlyDigits(value).slice(0, 8).replace(/^(\d{5})(\d)/, '$1-$2');
  }

  private onlyDigits(value: string) {
    return value.replace(/\D/g, '');
  }
}
