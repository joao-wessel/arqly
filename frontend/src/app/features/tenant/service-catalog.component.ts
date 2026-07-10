import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

type BillingUnit = 'UN' | 'M2' | 'M' | 'HOUR' | 'DAY' | 'MONTH' | 'PROJECT' | 'VISIT' | 'OTHER';
type ViewMode = 'services' | 'categories';
type ServiceTab = 'general' | 'commercial' | 'settings';

interface Page<T> {
  content: T[];
  number: number;
  totalElements: number;
  totalPages: number;
}

interface ServiceCategory {
  id: string;
  name: string;
  description: string;
  color: string;
  icon: string;
  active: boolean;
  servicesCount: number;
  createdAt: string;
  updatedAt: string;
}

interface CatalogService {
  id: string;
  name: string;
  categoryId: string | null;
  categoryName: string | null;
  categoryColor?: string | null;
  shortDescription?: string;
  fullDescription?: string;
  baseValue: number | null;
  currency: string;
  billingUnit: BillingUnit;
  active: boolean;
  featured: boolean;
  createdAt: string;
  updatedAt: string;
}

interface Stats {
  services: number;
  categories: number;
  activeServices: number;
  inactiveServices: number;
}

@Component({
  selector: 'app-service-catalog',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, LucideAngularModule, ArqlySelectComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Catálogo</p>
          <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Serviços</h2>
          <p class="mt-2 text-slate-500">Organize os serviços vendidos pelo escritório para propostas, projetos e indicadores futuros.</p>
        </div>
        <button class="btn-primary" type="button" (click)="mode() === 'services' ? openServiceModal() : openCategoryModal()">
          <lucide-icon name="Plus" size="18"></lucide-icon>
          {{ mode() === 'services' ? 'Novo serviço' : 'Nova categoria' }}
        </button>
      </div>

      <div class="grid gap-4 md:grid-cols-4">
        <div class="card p-5">
          <p class="text-sm font-bold text-slate-500">Serviços</p>
          <strong class="mt-2 block text-3xl">{{ stats()?.services || 0 }}</strong>
        </div>
        <div class="card p-5">
          <p class="text-sm font-bold text-slate-500">Categorias</p>
          <strong class="mt-2 block text-3xl">{{ stats()?.categories || 0 }}</strong>
        </div>
        <div class="card p-5">
          <p class="text-sm font-bold text-slate-500">Ativos</p>
          <strong class="mt-2 block text-3xl text-arqly-700">{{ stats()?.activeServices || 0 }}</strong>
        </div>
        <div class="card p-5">
          <p class="text-sm font-bold text-slate-500">Inativos</p>
          <strong class="mt-2 block text-3xl text-slate-500">{{ stats()?.inactiveServices || 0 }}</strong>
        </div>
      </div>

      <div class="card overflow-hidden">
        <div class="flex flex-col gap-3 border-b border-slate-200 p-4 md:flex-row md:items-center md:justify-between">
          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-2">
            <button class="rounded-xl px-4 py-3 text-sm font-bold transition"
                    [class.bg-white]="mode() === 'services'"
                    [class.text-arqly-700]="mode() === 'services'"
                    [class.shadow-sm]="mode() === 'services'"
                    [class.text-slate-500]="mode() !== 'services'"
                    type="button" (click)="switchMode('services')">Serviços</button>
            <button class="rounded-xl px-4 py-3 text-sm font-bold transition"
                    [class.bg-white]="mode() === 'categories'"
                    [class.text-arqly-700]="mode() === 'categories'"
                    [class.shadow-sm]="mode() === 'categories'"
                    [class.text-slate-500]="mode() !== 'categories'"
                    type="button" (click)="switchMode('categories')">Categorias</button>
          </div>
        </div>

        @if (mode() === 'services') {
          <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-7" [formGroup]="serviceFilterForm" (ngSubmit)="searchServices()">
            <input class="field md:col-span-2" placeholder="Pesquisar serviço" formControlName="name">
            <app-arqly-select formControlName="categoryId" placeholder="Categoria" [options]="categoryFilterOptions()" />
            <app-arqly-select formControlName="active" placeholder="Status" [options]="statusFilterOptions" />
            <app-arqly-select formControlName="featured" placeholder="Destaque" [options]="featuredFilterOptions" />
            <input class="field" type="number" placeholder="Valor mín." formControlName="minValue">
            <input class="field" type="number" placeholder="Valor máx." formControlName="maxValue">
            <button class="btn-secondary md:col-span-7" type="submit"><lucide-icon name="Search" size="18"></lucide-icon>Pesquisar</button>
          </form>

          <div class="overflow-x-auto">
            <table class="w-full min-w-[980px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th class="px-6 py-4">Nome</th>
                  <th class="px-6 py-4">Categoria</th>
                  <th class="px-6 py-4">Valor base</th>
                  <th class="px-6 py-4">Unidade</th>
                  <th class="px-6 py-4">Status</th>
                  <th class="px-6 py-4">Destaque</th>
                  <th class="px-6 py-4">Cadastro</th>
                  <th class="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (service of services(); track service.id) {
                  <tr class="border-t border-slate-100">
                    <td class="px-6 py-4">
                      <p class="font-bold">{{ service.name }}</p>
                      <p class="line-clamp-1 text-xs text-slate-500">{{ service.shortDescription || 'Sem descrição curta' }}</p>
                    </td>
                    <td class="px-6 py-4">
                      @if (service.categoryName) {
                        <span class="rounded-full px-3 py-1 text-xs font-bold" [style.background]="softColor(service.categoryColor)" [style.color]="service.categoryColor || 'var(--arqly-700)'">{{ service.categoryName }}</span>
                      } @else {
                        <span class="text-slate-400">Sem categoria</span>
                      }
                    </td>
                    <td class="px-6 py-4">{{ service.currency }} {{ service.baseValue || 0 | number:'1.2-2' }}</td>
                    <td class="px-6 py-4">{{ unitLabel(service.billingUnit) }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class.bg-arqly-50]="service.active" [class.text-arqly-700]="service.active" [class.bg-slate-100]="!service.active" [class.text-slate-500]="!service.active">{{ service.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td class="px-6 py-4">{{ service.featured ? 'Sim' : 'Não' }}</td>
                    <td class="px-6 py-4 text-slate-500">{{ service.createdAt | date:'dd/MM/yyyy' }}</td>
                    <td class="px-6 py-4">
                      <div class="flex justify-end gap-2">
                        <button class="btn-secondary px-3 py-2" type="button" title="Visualizar" (click)="openServiceModal(service.id, true)"><lucide-icon name="Search" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openServiceModal(service.id)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="toggleService(service)">{{ service.active ? 'Inativar' : 'Ativar' }}</button>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" title="Excluir" (click)="deleteService(service)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="8" class="px-6 py-12 text-center text-slate-500">Nenhum serviço cadastrado.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="flex flex-col gap-3 border-t border-slate-100 p-4 text-sm text-slate-500 md:flex-row md:items-center md:justify-between">
            <span>{{ serviceTotalElements() }} serviço(s) encontrado(s)</span>
            <div class="flex items-center gap-2">
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="servicePage() === 0" (click)="previousServicePage()">Anterior</button>
              <span class="px-2 font-bold text-slate-700">Página {{ servicePage() + 1 }} de {{ serviceTotalPages() || 1 }}</span>
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="servicePage() + 1 >= serviceTotalPages()" (click)="nextServicePage()">Próxima</button>
            </div>
          </div>
        } @else {
          <form class="grid gap-3 border-b border-slate-200 bg-slate-50/70 p-4 md:grid-cols-4" [formGroup]="categoryFilterForm" (ngSubmit)="searchCategories()">
            <input class="field md:col-span-2" placeholder="Pesquisar categoria" formControlName="name">
            <app-arqly-select formControlName="active" placeholder="Status" [options]="statusFilterOptions" />
            <button class="btn-secondary" type="submit"><lucide-icon name="Search" size="18"></lucide-icon>Pesquisar</button>
          </form>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[820px] text-left text-sm">
              <thead class="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th class="px-6 py-4">Nome</th>
                  <th class="px-6 py-4">Serviços</th>
                  <th class="px-6 py-4">Status</th>
                  <th class="px-6 py-4">Cor</th>
                  <th class="px-6 py-4">Ícone</th>
                  <th class="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (category of categories(); track category.id) {
                  <tr class="border-t border-slate-100">
                    <td class="px-6 py-4">
                      <p class="font-bold">{{ category.name }}</p>
                      <p class="line-clamp-1 text-xs text-slate-500">{{ category.description || 'Sem descrição' }}</p>
                    </td>
                    <td class="px-6 py-4">{{ category.servicesCount }}</td>
                    <td class="px-6 py-4"><span class="rounded-full px-3 py-1 text-xs font-bold" [class.bg-arqly-50]="category.active" [class.text-arqly-700]="category.active" [class.bg-slate-100]="!category.active" [class.text-slate-500]="!category.active">{{ category.active ? 'Ativa' : 'Inativa' }}</span></td>
                    <td class="px-6 py-4"><span class="inline-flex h-8 w-8 rounded-xl border border-slate-200" [style.background]="category.color || '#e2e8f0'"></span></td>
                    <td class="px-6 py-4">
                      <span class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
                        <lucide-icon [name]="category.icon || 'Folder'" size="18"></lucide-icon>
                      </span>
                    </td>
                    <td class="px-6 py-4">
                      <div class="flex justify-end gap-2">
                        <button class="btn-secondary px-3 py-2" type="button" title="Visualizar" (click)="openCategoryModal(category.id, true)"><lucide-icon name="Search" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openCategoryModal(category.id)"><lucide-icon name="Pencil" size="16"></lucide-icon></button>
                        <button class="btn-secondary px-3 py-2" type="button" (click)="toggleCategory(category)">{{ category.active ? 'Inativar' : 'Ativar' }}</button>
                        <button class="btn-secondary px-3 py-2 text-red-600" type="button" title="Excluir" (click)="deleteCategory(category)"><lucide-icon name="Trash2" size="16"></lucide-icon></button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="px-6 py-12 text-center text-slate-500">Nenhuma categoria cadastrada.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <div class="flex flex-col gap-3 border-t border-slate-100 p-4 text-sm text-slate-500 md:flex-row md:items-center md:justify-between">
            <span>{{ categoryTotalElements() }} categoria(s) encontrada(s)</span>
            <div class="flex items-center gap-2">
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="categoryPage() === 0" (click)="previousCategoryPage()">Anterior</button>
              <span class="px-2 font-bold text-slate-700">Página {{ categoryPage() + 1 }} de {{ categoryTotalPages() || 1 }}</span>
              <button class="btn-secondary px-3 py-2" type="button" [disabled]="categoryPage() + 1 >= categoryTotalPages()" (click)="nextCategoryPage()">Próxima</button>
            </div>
          </div>
        }
      </div>
    </section>

    @if (serviceModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card max-h-[92vh] w-full max-w-4xl space-y-5 overflow-y-auto p-6" [formGroup]="serviceForm" (ngSubmit)="saveService()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ readOnly() ? 'Visualizar serviço' : editingService() ? 'Editar serviço' : 'Novo serviço' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeServiceModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <div class="grid gap-2 rounded-2xl bg-slate-50/70 p-2 md:grid-cols-3">
            @for (tab of serviceTabs; track tab.value) {
              <button class="rounded-xl px-4 py-3 text-sm font-bold transition" type="button"
                      [class.bg-white]="activeServiceTab() === tab.value"
                      [class.text-arqly-700]="activeServiceTab() === tab.value"
                      [class.shadow-sm]="activeServiceTab() === tab.value"
                      [class.text-slate-500]="activeServiceTab() !== tab.value"
                      (click)="activeServiceTab.set(tab.value)">{{ tab.label }}</button>
            }
          </div>
          @if (activeServiceTab() === 'general') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Dados gerais</p>
              <div class="mt-4 grid gap-4 md:grid-cols-2">
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Nome do serviço <span class="text-red-500">*</span></span>
                  <input class="field" placeholder="Ex.: Projeto arquitetônico residencial" formControlName="name">
                </label>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Categoria</span>
                  <app-arqly-select formControlName="categoryId" placeholder="Selecione uma categoria" [options]="categoryFormOptions()" />
                </label>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Descrição curta</span>
                  <textarea class="field min-h-24" placeholder="Resumo exibido em listas e propostas" formControlName="shortDescription"></textarea>
                </label>
                <label class="space-y-1 md:col-span-2">
                  <span class="text-xs font-bold text-slate-500">Descrição completa</span>
                  <textarea class="field min-h-36" placeholder="Detalhe escopo, entregáveis e observações comerciais" formControlName="fullDescription"></textarea>
                </label>
              </div>
            </section>
          }
          @if (activeServiceTab() === 'commercial') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Comercial</p>
              <div class="mt-4 grid gap-4 md:grid-cols-2">
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Valor base</span>
                  <input class="field" type="number" min="0" step="0.01" placeholder="0,00" formControlName="baseValue">
                </label>
                <label class="space-y-1">
                  <span class="text-xs font-bold text-slate-500">Unidade de cobrança <span class="text-red-500">*</span></span>
                  <app-arqly-select formControlName="billingUnit" placeholder="Selecione a unidade" [options]="billingUnitOptions" panelMode="fixed" />
                </label>
              </div>
            </section>
          }
          @if (activeServiceTab() === 'settings') {
            <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Configurações</p>
              <p class="mt-1 text-sm text-slate-500">Controle a visibilidade do serviço nas listagens comerciais.</p>
              <div class="mt-4 grid gap-4 md:grid-cols-2">
                <label class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white p-4">
                  <input class="arqly-checkbox mt-1" type="checkbox" formControlName="active">
                  <span>
                    <span class="block text-sm font-extrabold text-slate-800">Serviço ativo</span>
                    <span class="mt-1 block text-xs text-slate-500">Serviços ativos ficam disponíveis para uso em propostas e módulos futuros.</span>
                  </span>
                </label>
                <label class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white p-4">
                  <input class="arqly-checkbox mt-1" type="checkbox" formControlName="featured">
                  <span>
                    <span class="block text-sm font-extrabold text-slate-800">Destacar serviço</span>
                    <span class="mt-1 block text-xs text-slate-500">Use destaque para serviços estratégicos ou mais vendidos.</span>
                  </span>
                </label>
              </div>
            </section>
          }
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeServiceModal()">Fechar</button>
            @if (!readOnly()) {
              <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button>
            }
          </div>
        </form>
      </div>
    }

    @if (categoryModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-2xl space-y-5 p-6" [formGroup]="categoryForm" (ngSubmit)="saveCategory()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ readOnly() ? 'Visualizar categoria' : editingCategory() ? 'Editar categoria' : 'Nova categoria' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeCategoryModal()"><lucide-icon name="X" size="18"></lucide-icon></button>
          </div>
          <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4">
            <div class="mb-4">
              <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Dados da categoria</p>
              <p class="mt-1 text-sm text-slate-500">Categorias ajudam a agrupar serviços por área de atuação ou tipo de entrega.</p>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <label class="space-y-1 md:col-span-2">
                <span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span>
                <input class="field" placeholder="Ex.: Arquitetura" formControlName="name">
              </label>
              <label class="space-y-1 md:col-span-2">
                <span class="text-xs font-bold text-slate-500">Descrição</span>
                <textarea class="field min-h-28" placeholder="Descreva o tipo de serviço desta categoria" formControlName="description"></textarea>
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Cor</span>
                <div class="flex h-12 items-center gap-3 rounded-xl border border-slate-200 bg-white px-4">
                  <input class="h-8 w-12 rounded-xl border-0 bg-transparent p-0" type="color" title="Cor" formControlName="color">
                  <span class="text-sm font-bold text-slate-600">{{ categoryForm.controls.color.value }}</span>
                </div>
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Ícone</span>
                <app-arqly-select formControlName="icon" placeholder="Selecione um ícone" [options]="iconOptions" panelMode="fixed" />
              </label>
              <label class="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white p-4">
                <input class="arqly-checkbox mt-1" type="checkbox" formControlName="active">
                <span>
                  <span class="block text-sm font-extrabold text-slate-800">Categoria ativa</span>
                  <span class="mt-1 block text-xs text-slate-500">Categorias ativas aparecem na seleção de serviços.</span>
                </span>
              </label>
            </div>
          </section>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeCategoryModal()">Fechar</button>
            @if (!readOnly()) {
              <button class="btn-primary" type="submit"><lucide-icon name="Save" size="18"></lucide-icon>Salvar</button>
            }
          </div>
        </form>
      </div>
    }
  `
})
export class ServiceCatalogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = 'http://localhost:8080/api/tenant/service-catalog';

  readonly mode = signal<ViewMode>('services');
  readonly stats = signal<Stats | null>(null);
  readonly services = signal<CatalogService[]>([]);
  readonly categories = signal<ServiceCategory[]>([]);
  readonly categoryOptions = signal<ServiceCategory[]>([]);
  readonly servicePage = signal(0);
  readonly serviceTotalPages = signal(0);
  readonly serviceTotalElements = signal(0);
  readonly categoryPage = signal(0);
  readonly categoryTotalPages = signal(0);
  readonly categoryTotalElements = signal(0);
  readonly serviceModalOpen = signal(false);
  readonly categoryModalOpen = signal(false);
  readonly editingService = signal<string | null>(null);
  readonly editingCategory = signal<string | null>(null);
  readonly readOnly = signal(false);
  readonly activeServiceTab = signal<ServiceTab>('general');
  readonly serviceTabs: { label: string; value: ServiceTab }[] = [
    { label: 'Dados gerais', value: 'general' },
    { label: 'Comercial', value: 'commercial' },
    { label: 'Configurações', value: 'settings' }
  ];
  readonly statusFilterOptions = [
    { label: 'Todos', value: '' },
    { label: 'Ativos', value: 'true' },
    { label: 'Inativos', value: 'false' }
  ];
  readonly featuredFilterOptions = [
    { label: 'Todos', value: '' },
    { label: 'Em destaque', value: 'true' },
    { label: 'Sem destaque', value: 'false' }
  ];
  readonly billingUnitOptions = [
    { label: 'UN', value: 'UN' },
    { label: 'M²', value: 'M2' },
    { label: 'M', value: 'M' },
    { label: 'Hora', value: 'HOUR' },
    { label: 'Dia', value: 'DAY' },
    { label: 'Mês', value: 'MONTH' },
    { label: 'Projeto', value: 'PROJECT' },
    { label: 'Visita', value: 'VISIT' },
    { label: 'Outro', value: 'OTHER' }
  ];
  readonly iconOptions = [
    { label: 'Pasta', value: 'Folder' },
    { label: 'Edifício', value: 'Building2' },
    { label: 'Documento', value: 'FileText' },
    { label: 'Lista', value: 'ListChecks' },
    { label: 'Calendário', value: 'CalendarDays' },
    { label: 'Atividade', value: 'Activity' },
    { label: 'Equipe', value: 'Users' },
    { label: 'Destaque', value: 'Sparkles' },
    { label: 'Configurações', value: 'Settings' },
    { label: 'Segurança', value: 'ShieldCheck' }
  ];
  readonly serviceFilterForm = this.fb.nonNullable.group({
    name: [''],
    categoryId: [''],
    active: [''],
    featured: [''],
    minValue: [''],
    maxValue: ['']
  });
  readonly categoryFilterForm = this.fb.nonNullable.group({
    name: [''],
    active: ['']
  });
  readonly serviceForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    categoryId: [''],
    shortDescription: [''],
    fullDescription: [''],
    baseValue: [0],
    billingUnit: ['PROJECT' as BillingUnit, Validators.required],
    active: [true],
    featured: [false]
  });
  readonly categoryForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    color: ['#0f766e'],
    icon: ['Folder'],
    active: [true]
  });

  ngOnInit() {
    this.reloadAll();
  }

  reloadAll() {
    this.loadStats();
    this.loadCategories();
    this.loadCategoryOptions();
    this.loadServices();
  }

  switchMode(mode: ViewMode) {
    this.mode.set(mode);
    mode === 'services' ? this.loadServices() : this.loadCategories();
  }

  loadStats() {
    this.http.get<ApiResponse<Stats>>(`${this.baseUrl}/stats`).subscribe((response) => this.stats.set(response.data));
  }

  loadServices() {
    const params = this.paramsFrom(this.serviceFilterForm.getRawValue());
    params.set('page', String(this.servicePage()));
    params.set('size', '10');
    params.set('sort', 'name,asc');
    this.http.get<ApiResponse<Page<CatalogService>>>(`${this.baseUrl}/services?${params}`)
      .subscribe((response) => {
        this.services.set(response.data.content);
        this.servicePage.set(response.data.number);
        this.serviceTotalPages.set(response.data.totalPages);
        this.serviceTotalElements.set(response.data.totalElements);
      });
  }

  loadCategories() {
    const params = this.paramsFrom(this.categoryFilterForm.getRawValue());
    params.set('page', String(this.categoryPage()));
    params.set('size', '10');
    params.set('sort', 'name,asc');
    this.http.get<ApiResponse<Page<ServiceCategory>>>(`${this.baseUrl}/categories?${params}`)
      .subscribe((response) => {
        this.categories.set(response.data.content);
        this.categoryPage.set(response.data.number);
        this.categoryTotalPages.set(response.data.totalPages);
        this.categoryTotalElements.set(response.data.totalElements);
      });
  }

  loadCategoryOptions() {
    this.http.get<ApiResponse<ServiceCategory[]>>(`${this.baseUrl}/categories/options`)
      .subscribe((response) => this.categoryOptions.set(response.data));
  }

  categoryFilterOptions() {
    return [{ label: 'Todas', value: '' }, ...this.categoryOptions().map((category) => ({ label: category.name, value: category.id }))];
  }

  categoryFormOptions() {
    return [{ label: 'Sem categoria', value: '' }, ...this.categoryOptions().filter((category) => category.active).map((category) => ({ label: category.name, value: category.id }))];
  }

  searchServices() {
    this.servicePage.set(0);
    this.loadServices();
  }

  searchCategories() {
    this.categoryPage.set(0);
    this.loadCategories();
  }

  previousServicePage() {
    if (this.servicePage() === 0) return;
    this.servicePage.update((page) => page - 1);
    this.loadServices();
  }

  nextServicePage() {
    if (this.servicePage() + 1 >= this.serviceTotalPages()) return;
    this.servicePage.update((page) => page + 1);
    this.loadServices();
  }

  previousCategoryPage() {
    if (this.categoryPage() === 0) return;
    this.categoryPage.update((page) => page - 1);
    this.loadCategories();
  }

  nextCategoryPage() {
    if (this.categoryPage() + 1 >= this.categoryTotalPages()) return;
    this.categoryPage.update((page) => page + 1);
    this.loadCategories();
  }

  openServiceModal(id?: string, readOnly = false) {
    this.readOnly.set(readOnly);
    this.editingService.set(id || null);
    this.activeServiceTab.set('general');
    this.serviceForm.enable();
    this.serviceForm.reset({
      name: '',
      categoryId: '',
      shortDescription: '',
      fullDescription: '',
      baseValue: 0,
      billingUnit: 'PROJECT',
      active: true,
      featured: false
    });
    if (id) {
      this.http.get<ApiResponse<CatalogService>>(`${this.baseUrl}/services/${id}`).subscribe((response) => {
        this.serviceForm.patchValue({
          name: response.data.name,
          categoryId: response.data.categoryId || '',
          shortDescription: response.data.shortDescription || '',
          fullDescription: response.data.fullDescription || '',
          baseValue: response.data.baseValue || 0,
          billingUnit: response.data.billingUnit,
          active: response.data.active,
          featured: response.data.featured
        });
        if (readOnly) this.serviceForm.disable();
      });
    }
    this.serviceModalOpen.set(true);
  }

  closeServiceModal() {
    this.serviceModalOpen.set(false);
  }

  saveService() {
    if (this.serviceForm.invalid) {
      this.serviceForm.markAllAsTouched();
      this.toast.validation('Preencha os dados obrigatórios do serviço.');
      return;
    }
    const id = this.editingService();
    const payload = this.normalizedServicePayload();
    const request = id
      ? this.http.put<ApiResponse<CatalogService>>(`${this.baseUrl}/services/${id}`, payload)
      : this.http.post<ApiResponse<CatalogService>>(`${this.baseUrl}/services`, payload);
    request.subscribe({
      next: () => {
        this.closeServiceModal();
        this.reloadAll();
        this.toast.success(id ? 'Serviço atualizado' : 'Serviço criado');
      },
      error: () => this.toast.error('Não foi possível salvar o serviço')
    });
  }

  toggleService(service: CatalogService) {
    const action = service.active ? 'deactivate' : 'activate';
    this.http.patch<ApiResponse<CatalogService>>(`${this.baseUrl}/services/${service.id}/${action}`, {})
      .subscribe(() => {
        this.reloadAll();
        this.toast.success(service.active ? 'Serviço inativado' : 'Serviço ativado');
      });
  }

  deleteService(service: CatalogService) {
    if (!confirm(`Remover o serviço "${service.name}"?`)) return;
    this.http.delete(`${this.baseUrl}/services/${service.id}`).subscribe({
      next: () => {
        this.reloadAll();
        this.toast.success('Serviço removido');
      },
      error: () => this.toast.error('Não foi possível remover o serviço')
    });
  }

  openCategoryModal(id?: string, readOnly = false) {
    this.readOnly.set(readOnly);
    this.editingCategory.set(id || null);
    this.categoryForm.enable();
    this.categoryForm.reset({ name: '', description: '', color: '#0f766e', icon: 'Folder', active: true });
    if (id) {
      this.http.get<ApiResponse<ServiceCategory>>(`${this.baseUrl}/categories/${id}`).subscribe((response) => {
        this.categoryForm.patchValue({
          name: response.data.name,
          description: response.data.description || '',
          color: response.data.color || '#0f766e',
          icon: response.data.icon || 'Folder',
          active: response.data.active
        });
        if (readOnly) this.categoryForm.disable();
      });
    }
    this.categoryModalOpen.set(true);
  }

  closeCategoryModal() {
    this.categoryModalOpen.set(false);
  }

  saveCategory() {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      this.toast.validation('Informe o nome da categoria.');
      return;
    }
    const id = this.editingCategory();
    const request = id
      ? this.http.put<ApiResponse<ServiceCategory>>(`${this.baseUrl}/categories/${id}`, this.normalizedCategoryPayload())
      : this.http.post<ApiResponse<ServiceCategory>>(`${this.baseUrl}/categories`, this.normalizedCategoryPayload());
    request.subscribe({
      next: () => {
        this.closeCategoryModal();
        this.reloadAll();
        this.toast.success(id ? 'Categoria atualizada' : 'Categoria criada');
      },
      error: () => this.toast.error('Não foi possível salvar a categoria')
    });
  }

  toggleCategory(category: ServiceCategory) {
    const action = category.active ? 'deactivate' : 'activate';
    this.http.patch<ApiResponse<ServiceCategory>>(`${this.baseUrl}/categories/${category.id}/${action}`, {})
      .subscribe(() => {
        this.reloadAll();
        this.toast.success(category.active ? 'Categoria inativada' : 'Categoria ativada');
      });
  }

  deleteCategory(category: ServiceCategory) {
    if (!confirm(`Remover a categoria "${category.name}"?`)) return;
    this.http.delete(`${this.baseUrl}/categories/${category.id}`).subscribe({
      next: () => {
        this.reloadAll();
        this.toast.success('Categoria removida');
      },
      error: () => this.toast.error('Categorias com serviços vinculados devem ser apenas inativadas.')
    });
  }

  unitLabel(unit: BillingUnit) {
    return this.billingUnitOptions.find((option) => option.value === unit)?.label || unit;
  }

  softColor(color?: string | null) {
    return color ? `${color}18` : 'var(--arqly-50)';
  }

  private paramsFrom(values: Record<string, unknown>) {
    const params = new URLSearchParams();
    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') params.set(key, String(value));
    });
    return params;
  }

  private normalizedServicePayload() {
    const value = this.serviceForm.getRawValue();
    return {
      ...value,
      categoryId: value.categoryId || null,
      baseValue: value.baseValue || 0,
      currency: 'BRL'
    };
  }

  private normalizedCategoryPayload() {
    return {
      ...this.categoryForm.getRawValue()
    };
  }
}
