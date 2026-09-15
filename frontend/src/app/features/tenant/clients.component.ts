import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type ClientPersonType = 'NATURAL_PERSON' | 'LEGAL_ENTITY';
type ClientStatus = 'ACTIVE' | 'INACTIVE';
type ClientTab = 'general' | 'address' | 'notes';

interface ClientSummary {
  id: string;
  personType: ClientPersonType;
  displayName: string;
  document: string;
  email: string;
  phone: string;
  city: string;
  state: string;
  status: ClientStatus;
  portalActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ClientPortalAccess {
  id: string;
  token: string;
  portalUrl: string;
  createdAt: string;
  expiresAt: string;
  revoked: boolean;
  lastAccessAt: string | null;
  active: boolean;
}

interface ClientDetail extends ClientSummary {
  name: string;
  cpf: string;
  rg: string;
  birthDate: string;
  legalName: string;
  tradeName: string;
  cnpj: string;
  stateRegistration: string;
  whatsapp: string;
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  district: string;
  notes: string;
  portalAccess: ClientPortalAccess | null;
}

interface Page<T> {
  content: T[];
  number: number;
  totalElements: number;
  totalPages: number;
}

@Component({
  selector: 'app-clients',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LucideAngularModule, ArqlySelectComponent, ArqlyDatePickerComponent],
  template: `
    <section class="space-y-5">
      <div class="card overflow-hidden">
        <div class="flex flex-col gap-4 border-b border-slate-200 p-6 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 class="text-3xl font-extrabold tracking-tight">Clientes</h2>
            <p class="mt-2 text-slate-500">Cadastro de clientes do escritório e acesso ao portal.</p>
          </div>
          <button class="btn-primary" type="button" (click)="openClientModal()">
            <lucide-icon name="Plus" size="18"></lucide-icon>
            Novo cliente
          </button>
        </div>

        <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-7" [formGroup]="filterForm" (ngSubmit)="search()">
          <input class="field md:col-span-2" placeholder="Nome" formControlName="name">
          <input class="field" placeholder="CPF/CNPJ" formControlName="document">
          <input class="field" placeholder="Cidade" formControlName="city">
          <app-arqly-select formControlName="status" placeholder="Status" [options]="statusFilterOptions" />
          <app-arqly-select formControlName="personType" placeholder="Tipo" [options]="personTypeFilterOptions" />
          <app-arqly-select formControlName="portal" placeholder="Portal" [options]="portalFilterOptions" />
          <button class="btn-secondary md:col-span-7" type="submit">
            <lucide-icon name="Search" size="18"></lucide-icon>
            Pesquisar
          </button>
        </form>

        <div class="hidden overflow-x-auto md:block">
          <table class="w-full min-w-[980px] text-left text-sm">
            <thead class="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th class="px-6 py-4">Cliente</th>
                <th class="px-6 py-4">Documento</th>
                <th class="px-6 py-4">Contato</th>
                <th class="px-6 py-4">Cidade/UF</th>
                <th class="px-6 py-4">Portal</th>
                <th class="px-6 py-4">Status</th>
                <th class="px-6 py-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (client of clients(); track client.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-6 py-4">
                    <p class="font-bold">{{ client.displayName }}</p>
                    <p class="text-xs text-slate-500">{{ client.personType === 'NATURAL_PERSON' ? 'Pessoa física' : 'Pessoa jurídica' }}</p>
                  </td>
                  <td class="px-6 py-4 text-slate-600">{{ client.document }}</td>
                  <td class="px-6 py-4 text-slate-600">
                    <p>{{ client.email }}</p>
                    <p class="text-xs text-slate-500">{{ client.phone }}</p>
                  </td>
                  <td class="px-6 py-4 text-slate-600">{{ client.city }}/{{ client.state }}</td>
                  <td class="px-6 py-4">
                    <span class="rounded-full px-3 py-1 text-xs font-bold"
                          [class.bg-arqly-50]="client.portalActive"
                          [class.text-arqly-700]="client.portalActive"
                          [class.bg-slate-100]="!client.portalActive"
                          [class.text-slate-500]="!client.portalActive">
                      {{ client.portalActive ? 'Ativo' : 'Sem acesso' }}
                    </span>
                  </td>
                  <td class="px-6 py-4">
                    <span class="rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ client.status === 'ACTIVE' ? 'Ativo' : 'Inativo' }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex justify-end gap-2">
                      <button class="btn-secondary px-3 py-2" type="button" title="Visualizar" (click)="openClientModal(client.id, true)">
                        <lucide-icon name="Search" size="16"></lucide-icon>
                      </button>
                      <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openClientModal(client.id)">
                        <lucide-icon name="Pencil" size="16"></lucide-icon>
                      </button>
                      <button class="btn-secondary px-3 py-2" type="button" title="Portal do cliente" (click)="openPortalModal(client)">
                        <lucide-icon name="KeyRound" size="16"></lucide-icon>
                      </button>
                      <button class="btn-secondary px-3 py-2 text-red-600 hover:border-red-200 hover:text-red-700" type="button" title="Excluir" (click)="openDeleteModal(client)">
                        <lucide-icon name="Trash2" size="16"></lucide-icon>
                      </button>
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="px-6 py-12 text-center text-slate-500">Nenhum cliente cadastrado.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <div class="space-y-3 p-4 md:hidden">
          @for (client of clients(); track client.id) {
            <article class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="truncate font-extrabold">{{ client.displayName }}</p>
                  <p class="mt-1 truncate text-sm text-slate-500">{{ client.document }}</p>
                </div>
                <span class="shrink-0 rounded-full bg-arqly-50 px-3 py-1 text-xs font-bold text-arqly-700">{{ client.status === 'ACTIVE' ? 'Ativo' : 'Inativo' }}</span>
              </div>
              <p class="mt-3 text-sm text-slate-600">{{ client.city }}/{{ client.state }} · {{ client.email }}</p>
              <div class="mt-4 grid grid-cols-4 gap-2">
                <button class="btn-secondary px-3 py-2" type="button" (click)="openClientModal(client.id, true)">Ver</button>
                <button class="btn-secondary px-3 py-2" type="button" (click)="openClientModal(client.id)">Editar</button>
                <button class="btn-secondary px-3 py-2" type="button" (click)="openPortalModal(client)">Portal</button>
                <button class="btn-secondary px-3 py-2 text-red-600" type="button" (click)="openDeleteModal(client)">Excluir</button>
              </div>
            </article>
          } @empty {
            <p class="py-8 text-center text-sm text-slate-500">Nenhum cliente cadastrado.</p>
          }
        </div>

        <div class="flex flex-col gap-3 border-t border-slate-100 p-4 text-sm text-slate-500 md:flex-row md:items-center md:justify-between">
          <span>{{ totalElements() }} cliente(s) encontrado(s)</span>
          <div class="flex items-center gap-2">
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() === 0" (click)="previousPage()">Anterior</button>
            <span class="px-2 font-bold text-slate-700">Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span>
            <button class="btn-secondary px-3 py-2" type="button" [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">Próxima</button>
          </div>
        </div>
      </div>
    </section>

    @if (clientModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-4xl space-y-5 overflow-y-auto p-6" [formGroup]="clientForm" (ngSubmit)="saveClient()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ readOnly() ? 'Visualizar cliente' : editingClient() ? 'Editar cliente' : 'Novo cliente' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeClientModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>

          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-3">
            @for (tab of tabs; track tab.value) {
              <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                      [class.bg-white]="activeTab() === tab.value"
                      [class.text-arqly-700]="activeTab() === tab.value"
                      [class.shadow-sm]="activeTab() === tab.value"
                      [class.text-slate-500]="activeTab() !== tab.value"
                      (click)="activeTab.set(tab.value)">
                {{ tab.label }}
              </button>
            }
          </div>

          @if (activeTab() === 'general') {
            <section class="space-y-4">
              <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Identificação</p>
                  <p class="mt-1 text-sm text-slate-500">Preencha somente os dados essenciais agora. O restante pode ser completado depois.</p>
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Tipo de pessoa <span class="text-red-500">*</span></span>
                    <app-arqly-select formControlName="personType" placeholder="Selecione o tipo" [options]="personTypeOptions" />
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Status <span class="text-red-500">*</span></span>
                    <app-arqly-select formControlName="status" placeholder="Selecione o status" [options]="statusOptions" />
                  </label>
                  @if (clientForm.controls.personType.value === 'NATURAL_PERSON') {
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">Nome completo <span class="text-red-500">*</span></span>
                      <input class="field" placeholder="Nome completo" formControlName="name">
                    </label>
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">CPF <span class="text-red-500">*</span></span>
                      <input class="field" placeholder="000.000.000-00" inputmode="numeric" maxlength="14" formControlName="cpf" (input)="maskDocument($event, 'cpf')">
                    </label>
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">RG</span>
                      <input class="field" placeholder="00.000.000-0" inputmode="numeric" maxlength="12" formControlName="rg" (input)="maskRg($event)">
                    </label>
                    <div class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">Data de nascimento</span>
                      <app-arqly-date-picker formControlName="birthDate" placeholder="Selecione" />
                    </div>
                  } @else {
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">Razão social <span class="text-red-500">*</span></span>
                      <input class="field" placeholder="Razão social" formControlName="legalName">
                    </label>
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">Nome fantasia <span class="text-red-500">*</span></span>
                      <input class="field" placeholder="Nome fantasia" formControlName="tradeName">
                    </label>
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">CNPJ <span class="text-red-500">*</span></span>
                      <input class="field" placeholder="00.000.000/0000-00" inputmode="numeric" maxlength="18" formControlName="cnpj" (input)="maskDocument($event, 'cnpj')">
                    </label>
                    <label class="space-y-1">
                      <span class="text-xs font-bold text-slate-500">Inscrição estadual</span>
                      <input class="field" placeholder="Opcional" formControlName="stateRegistration">
                    </label>
                  }
                </div>
              </div>

              <div class="rounded-2xl border border-slate-200 bg-white p-4">
                <div class="mb-4">
                  <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Contato</p>
                </div>
                <div class="grid gap-4 md:grid-cols-3">
                  <label class="space-y-1 md:col-span-3">
                    <span class="text-xs font-bold text-slate-500">E-mail <span class="text-red-500">*</span></span>
                    <input class="field" type="email" placeholder="cliente@email.com" formControlName="email">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">Telefone</span>
                    <input class="field" placeholder="(00) 00000-0000" inputmode="tel" maxlength="15" formControlName="phone" (input)="maskPhone($event, 'phone')">
                  </label>
                  <label class="space-y-1">
                    <span class="text-xs font-bold text-slate-500">WhatsApp</span>
                    <input class="field" placeholder="(00) 00000-0000" inputmode="tel" maxlength="15" formControlName="whatsapp" (input)="maskPhone($event, 'whatsapp')">
                  </label>
                </div>
              </div>
            </section>
          }

          @if (activeTab() === 'address') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <div class="mb-4">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Endereço</p>
                <p class="mt-1 text-sm text-slate-500">Informe o CEP para preencher o endereço automaticamente.</p>
              </div>
              <div class="grid gap-4 md:grid-cols-2">
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">CEP <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="00000-000" inputmode="numeric" maxlength="9" formControlName="zipCode" (input)="onZipCodeInput($event)">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Número <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Número" formControlName="number">
                </label>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Logradouro <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Rua, avenida, travessa..." formControlName="street">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Complemento</span>
                  <input class="field" placeholder="Apartamento, sala, bloco..." formControlName="complement">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Bairro <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Bairro" formControlName="district">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Cidade <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Cidade" formControlName="city">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Estado <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="UF" maxlength="2" formControlName="state">
                </label>
              </div>
            </section>
          }

          @if (activeTab() === 'notes') {
            <section class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="mb-4">
                <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Observações</p>
                <p class="mt-1 text-sm text-slate-500">Use este espaço para preferências, restrições, contexto comercial ou lembretes internos.</p>
              </div>
              <textarea class="field min-h-44" placeholder="Escreva observações úteis para a equipe..." formControlName="notes"></textarea>
            </section>
          }

          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeClientModal()">Fechar</button>
            @if (!readOnly()) {
              <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button>
            }
          </div>
        </form>
      </div>
    }

    @if (deleteModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <div class="modal-panel card w-full max-w-md space-y-5 p-6">
          <h3 class="text-xl font-extrabold">Excluir cliente</h3>
          <p class="text-sm text-slate-600">Deseja excluir <strong>{{ selectedClient()?.displayName }}</strong>?</p>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeDeleteModal()">Cancelar</button>
            <button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="deleteClient()">Excluir</button>
          </div>
        </div>
      </div>
    }

    @if (portalModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <div class="modal-panel card max-h-[92vh] w-full max-w-3xl space-y-5 overflow-y-auto p-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Portal do cliente</p>
              <h3 class="mt-2 text-xl font-extrabold">{{ currentClient()?.displayName || selectedClient()?.displayName }}</h3>
              <p class="mt-1 text-sm text-slate-500">Gerencie o link público de acesso deste cliente.</p>
            </div>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closePortalModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            @if (currentClient()?.portalAccess) {
              <div class="grid gap-3 text-sm md:grid-cols-2">
                <p><strong>Status:</strong> {{ currentClient()?.portalAccess?.active && !currentClient()?.portalAccess?.revoked ? 'Ativo' : 'Revogado' }}</p>
                <p><strong>Expiração:</strong> {{ currentClient()?.portalAccess?.expiresAt | date:'short' }}</p>
                <p><strong>Último acesso:</strong> {{ currentClient()?.portalAccess?.lastAccessAt ? (currentClient()?.portalAccess?.lastAccessAt | date:'short') : '-' }}</p>
                <p class="break-all"><strong>Token:</strong> {{ currentClient()?.portalAccess?.token }}</p>
              </div>
              <div class="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
                <input class="field" type="datetime-local" [value]="portalExpiresAt()" (input)="portalExpiresAt.set($any($event.target).value)">
                <button class="btn-secondary" type="button" (click)="updatePortalValidity()">Alterar validade</button>
              </div>
              <div class="mt-4 flex flex-wrap gap-3">
                <button class="btn-secondary" type="button" (click)="copyPortalLink()"><lucide-icon name="Copy" size="18"></lucide-icon>Copiar link</button>
                <button class="btn-secondary" type="button" (click)="generatePortalAccess()">Gerar novo acesso</button>
                <button class="btn-secondary text-red-600" type="button" (click)="revokePortalAccess()">Revogar acesso</button>
              </div>
            } @else {
              <p class="text-sm text-slate-500">Este cliente ainda não possui acesso ativo ao portal.</p>
              <button class="btn-primary mt-4" type="button" (click)="generatePortalAccess()">Gerar novo acesso</button>
            }
          </div>

          @if (portalHistory().length) {
            <div class="rounded-2xl border border-slate-200 bg-white p-4">
              <h4 class="font-extrabold">Histórico de acessos</h4>
              <div class="mt-3 max-h-72 space-y-2 overflow-y-auto pr-2">
                @for (access of portalHistory(); track access.id) {
                  <div class="flex flex-col gap-1 rounded-xl bg-slate-50 p-3 text-sm md:flex-row md:items-center md:justify-between">
                    <span>{{ access.createdAt | date:'short' }}</span>
                    <span>{{ access.active && !access.revoked ? 'Ativo' : 'Revogado' }}</span>
                    <span>Expira em {{ access.expiresAt | date:'short' }}</span>
                    <span>Último acesso: {{ access.lastAccessAt ? (access.lastAccessAt | date:'short') : '-' }}</span>
                  </div>
                }
              </div>
            </div>
          }
        </div>
      </div>
    }
  `
})
export class ClientsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private lastZipCodeLookup = '';
  readonly clients = signal<ClientSummary[]>([]);
  readonly clientModalOpen = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly portalModalOpen = signal(false);
  readonly editingClient = signal<string | null>(null);
  readonly selectedClient = signal<ClientSummary | null>(null);
  readonly currentClient = signal<ClientDetail | null>(null);
  readonly portalHistory = signal<ClientPortalAccess[]>([]);
  readonly portalExpiresAt = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly readOnly = signal(false);
  readonly activeTab = signal<ClientTab>('general');
  readonly tabs: { label: string; value: ClientTab }[] = [
    { label: 'Dados gerais', value: 'general' },
    { label: 'Endereço', value: 'address' },
    { label: 'Observações', value: 'notes' }
  ];
  readonly personTypeOptions = [
    { label: 'Pessoa física', value: 'NATURAL_PERSON' },
    { label: 'Pessoa jurídica', value: 'LEGAL_ENTITY' }
  ];
  readonly personTypeFilterOptions = [{ label: 'Todos', value: '' }, ...this.personTypeOptions];
  readonly statusOptions = [
    { label: 'Ativo', value: 'ACTIVE' },
    { label: 'Inativo', value: 'INACTIVE' }
  ];
  readonly statusFilterOptions = [{ label: 'Todos', value: '' }, ...this.statusOptions];
  readonly portalFilterOptions = [
    { label: 'Todos', value: '' },
    { label: 'Com portal ativo', value: 'true' },
    { label: 'Sem portal ativo', value: 'false' }
  ];
  readonly filterForm = this.fb.nonNullable.group({
    name: [''],
    document: [''],
    city: [''],
    status: [''],
    personType: [''],
    portal: ['']
  });
  readonly clientForm = this.fb.nonNullable.group({
    personType: ['NATURAL_PERSON' as ClientPersonType, Validators.required],
    status: ['ACTIVE' as ClientStatus, Validators.required],
    name: [''],
    cpf: [''],
    rg: [''],
    birthDate: [''],
    legalName: [''],
    tradeName: [''],
    cnpj: [''],
    stateRegistration: [''],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    whatsapp: [''],
    zipCode: ['', Validators.required],
    street: ['', Validators.required],
    number: ['', Validators.required],
    complement: [''],
    district: ['', Validators.required],
    city: ['', Validators.required],
    state: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
    notes: ['']
  });

  ngOnInit() {
    this.load();
  }

  load() {
    const filters = this.filterForm.getRawValue();
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    params.set('page', String(this.page()));
    params.set('size', '10');
    params.set('sort', 'createdAt,desc');
    this.http.get<ApiResponse<Page<ClientSummary>>>(`http://localhost:8080/api/tenant/clients?${params.toString()}`)
      .subscribe((response) => {
        this.clients.set(response.data.content);
        this.page.set(response.data.number);
        this.totalPages.set(response.data.totalPages);
        this.totalElements.set(response.data.totalElements);
      });
  }

  search() {
    this.page.set(0);
    this.load();
  }

  previousPage() {
    if (this.page() === 0) return;
    this.page.update((page) => page - 1);
    this.load();
  }

  nextPage() {
    if (this.page() + 1 >= this.totalPages()) return;
    this.page.update((page) => page + 1);
    this.load();
  }

  openClientModal(id?: string, readOnly = false) {
    this.readOnly.set(readOnly);
    this.editingClient.set(id || null);
    this.currentClient.set(null);
    this.portalHistory.set([]);
    this.portalExpiresAt.set('');
    this.activeTab.set('general');
    this.clientForm.enable();
    this.clientForm.reset({
      personType: 'NATURAL_PERSON',
      status: 'ACTIVE',
      name: '',
      cpf: '',
      rg: '',
      birthDate: '',
      legalName: '',
      tradeName: '',
      cnpj: '',
      stateRegistration: '',
      email: '',
      phone: '',
      whatsapp: '',
      zipCode: '',
      street: '',
      number: '',
      complement: '',
      district: '',
      city: '',
      state: '',
      notes: ''
    });
    if (id) {
      this.http.get<ApiResponse<ClientDetail>>(`http://localhost:8080/api/tenant/clients/${id}`)
        .subscribe((response) => {
          this.currentClient.set(response.data);
          this.clientForm.patchValue(response.data);
          this.portalExpiresAt.set(this.toLocalDateTimeInput(response.data.portalAccess?.expiresAt));
          if (readOnly) this.clientForm.disable();
        });
    }
    this.clientModalOpen.set(true);
  }

  closeClientModal() {
    this.clientModalOpen.set(false);
  }

  openPortalModal(client: ClientSummary) {
    this.selectedClient.set(client);
    this.editingClient.set(client.id);
    this.currentClient.set(null);
    this.portalHistory.set([]);
    this.portalExpiresAt.set('');
    this.portalModalOpen.set(true);
    this.http.get<ApiResponse<ClientDetail>>(`http://localhost:8080/api/tenant/clients/${client.id}`)
      .subscribe((response) => {
        this.currentClient.set(response.data);
        this.portalExpiresAt.set(this.toLocalDateTimeInput(response.data.portalAccess?.expiresAt));
        this.loadPortalHistory(client.id);
      });
  }

  closePortalModal() {
    this.portalModalOpen.set(false);
    this.portalHistory.set([]);
    this.portalExpiresAt.set('');
    this.currentClient.set(null);
    this.editingClient.set(null);
  }

  saveClient() {
    this.applyPersonValidators();
    if (this.clientForm.invalid) {
      this.clientForm.markAllAsTouched();
      const addressControls = ['zipCode', 'street', 'number', 'district', 'city', 'state'];
      if (addressControls.some((control) => this.clientForm.get(control)?.invalid)) this.activeTab.set('address');
      this.toast.validation('Preencha os dados obrigatórios do cliente.');
      return;
    }
    const id = this.editingClient();
    const request = id
      ? this.http.put<ApiResponse<ClientDetail>>(`http://localhost:8080/api/tenant/clients/${id}`, this.clientForm.getRawValue())
      : this.http.post<ApiResponse<ClientDetail>>('http://localhost:8080/api/tenant/clients', this.clientForm.getRawValue());
    request.subscribe({
      next: (response) => {
        this.currentClient.set(response.data);
        this.editingClient.set(response.data.id);
        this.portalExpiresAt.set(this.toLocalDateTimeInput(response.data.portalAccess?.expiresAt));
        this.closeClientModal();
        this.load();
        this.toast.success(id ? 'Cliente atualizado' : 'Cliente criado', 'As informações foram salvas.');
      },
      error: () => this.toast.error('Não foi possível salvar', 'Revise os dados e tente novamente.')
    });
  }

  generatePortalAccess() {
    const id = this.editingClient();
    if (!id) return;
    this.http.post<ApiResponse<ClientPortalAccess>>(`http://localhost:8080/api/tenant/clients/${id}/portal-access`, {})
      .subscribe((response) => {
        this.currentClient.update((client) => client ? { ...client, portalAccess: response.data } : client);
        this.portalExpiresAt.set(this.toLocalDateTimeInput(response.data.expiresAt));
        this.loadPortalHistory(id);
        this.load();
        this.toast.success('Acesso gerado', 'O link anterior foi revogado.');
      });
  }

  revokePortalAccess() {
    const id = this.editingClient();
    if (!id) return;
    this.http.delete<ApiResponse<ClientPortalAccess>>(`http://localhost:8080/api/tenant/clients/${id}/portal-access`)
      .subscribe(() => {
        this.currentClient.update((client) => client ? { ...client, portalAccess: null } : client);
        this.portalExpiresAt.set('');
        this.loadPortalHistory(id);
        this.load();
        this.toast.success('Acesso revogado');
      });
  }

  copyPortalLink() {
    const link = this.currentClient()?.portalAccess?.portalUrl;
    if (!link) return;
    void navigator.clipboard.writeText(link);
    this.toast.success('Link copiado');
  }

  updatePortalValidity() {
    const id = this.editingClient();
    const value = this.portalExpiresAt();
    if (!id || !value) {
      this.toast.validation('Informe uma validade para o acesso.');
      return;
    }
    this.http.patch<ApiResponse<ClientPortalAccess>>(`http://localhost:8080/api/tenant/clients/${id}/portal-access/validity`, {
      expiresAt: new Date(value).toISOString()
    }).subscribe({
      next: (response) => {
        this.currentClient.update((client) => client ? { ...client, portalAccess: response.data } : client);
        this.portalExpiresAt.set(this.toLocalDateTimeInput(response.data.expiresAt));
        this.loadPortalHistory(id);
        this.toast.success('Validade atualizada');
      },
      error: () => this.toast.error('Não foi possível alterar a validade')
    });
  }

  loadPortalHistory(id: string) {
    this.http.get<ApiResponse<ClientPortalAccess[]>>(`http://localhost:8080/api/tenant/clients/${id}/portal-access/history`)
      .subscribe((response) => this.portalHistory.set(response.data));
  }

  openDeleteModal(client: ClientSummary) {
    this.selectedClient.set(client);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
  }

  deleteClient() {
    const client = this.selectedClient();
    if (!client) return;
    this.http.delete(`http://localhost:8080/api/tenant/clients/${client.id}`).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.load();
        this.toast.success('Cliente excluído');
      },
      error: () => this.toast.error('Não foi possível excluir')
    });
  }

  maskDocument(event: Event, control: 'cpf' | 'cnpj') {
    const input = event.target as HTMLInputElement;
    const digits = this.onlyDigits(input.value);
    const value = control === 'cpf'
      ? digits.slice(0, 11).replace(/^(\d{3})(\d)/, '$1.$2').replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3').replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4')
      : digits.slice(0, 14).replace(/^(\d{2})(\d)/, '$1.$2').replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3').replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4').replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5');
    this.clientForm.controls[control].setValue(value, { emitEvent: false });
    input.value = value;
  }

  maskPhone(event: Event, control: 'phone' | 'whatsapp') {
    const input = event.target as HTMLInputElement;
    const digits = this.onlyDigits(input.value).slice(0, 11);
    const value = digits.length <= 10
      ? digits.replace(/^(\d{2})(\d)/, '($1) $2').replace(/(\d{4})(\d)/, '$1-$2')
      : digits.replace(/^(\d{2})(\d)/, '($1) $2').replace(/(\d{5})(\d)/, '$1-$2');
    this.clientForm.controls[control].setValue(value, { emitEvent: false });
    input.value = value;
  }

  maskRg(event: Event) {
    const input = event.target as HTMLInputElement;
    const digits = this.onlyDigits(input.value).slice(0, 9);
    const value = digits
      .replace(/^(\d{2})(\d)/, '$1.$2')
      .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4');
    this.clientForm.controls.rg.setValue(value, { emitEvent: false });
    input.value = value;
  }

  onZipCodeInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const value = this.onlyDigits(input.value).slice(0, 8).replace(/^(\d{5})(\d)/, '$1-$2');
    this.clientForm.controls.zipCode.setValue(value, { emitEvent: false });
    input.value = value;
    const digits = this.onlyDigits(value);
    if (digits.length !== 8 || digits === this.lastZipCodeLookup) return;
    this.lastZipCodeLookup = digits;
    this.http.get<{ logradouro: string; bairro: string; localidade: string; uf: string; erro?: boolean }>(`https://viacep.com.br/ws/${digits}/json/`)
      .subscribe((response) => {
        if (response.erro) return;
        this.clientForm.patchValue({
          street: response.logradouro || '',
          district: response.bairro || '',
          city: response.localidade || '',
          state: response.uf || ''
        });
      });
  }

  private applyPersonValidators() {
    const isNatural = this.clientForm.controls.personType.value === 'NATURAL_PERSON';
    this.clientForm.controls.name.setValidators(isNatural ? Validators.required : []);
    this.clientForm.controls.cpf.setValidators(isNatural ? Validators.required : []);
    this.clientForm.controls.legalName.setValidators(isNatural ? [] : Validators.required);
    this.clientForm.controls.tradeName.setValidators(isNatural ? [] : Validators.required);
    this.clientForm.controls.cnpj.setValidators(isNatural ? [] : Validators.required);
    ['name', 'cpf', 'legalName', 'tradeName', 'cnpj'].forEach((control) => this.clientForm.get(control)?.updateValueAndValidity());
  }

  private onlyDigits(value: string) {
    return value.replace(/\D/g, '');
  }

  private toLocalDateTimeInput(value?: string | null) {
    if (!value) return '';
    const date = new Date(value);
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
  }
}
