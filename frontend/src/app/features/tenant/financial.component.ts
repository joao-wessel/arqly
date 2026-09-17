import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ArqlyCurrencyInputComponent } from '../../shared/components/arqly-currency-input.component';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';

interface Api<T> { data: T; }
interface Entry {
  id: string;
  type: 'RECEIVABLE' | 'PAYABLE';
  description: string;
  clientName?: string;
  projectName?: string;
  categoryName?: string;
  totalAmount: number;
  settledAmount: number;
  balance: number;
  status: string;
  installments: Installment[];
}
interface Installment { id: string; description: string; amount: number; settledAmount: number; balance: number; dueDate: string; status: string; overdue: boolean; }
interface Option { id: string; name?: string; displayName?: string; code?: string; }

@Component({
  selector: 'app-financial',
  standalone: true,
  imports: [CurrencyPipe, NgFor, NgIf, ReactiveFormsModule, LucideAngularModule, ArqlySelectComponent, ArqlyDatePickerComponent, ArqlyCurrencyInputComponent],
  template: `
    <section class="space-y-5">
      <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="text-xs font-extrabold uppercase tracking-[.2em] text-arqly-700">Gestão financeira</p>
          <h2 class="mt-1 text-3xl font-extrabold">Financeiro</h2>
          <p class="mt-2 text-slate-500">Acompanhe receitas, despesas, parcelas e movimentações do escritório.</p>
        </div>
        <button class="btn-primary" type="button" (click)="showForm()"><lucide-icon name="Plus" size="18" />Novo lançamento</button>
      </div>

      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <div class="card p-5"><p class="text-xs font-bold uppercase text-slate-400">A receber</p><strong class="mt-2 block text-2xl">{{ receivableOpen() | currency:'BRL' }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold uppercase text-slate-400">Recebido</p><strong class="mt-2 block text-2xl text-arqly-700">{{ received() | currency:'BRL' }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold uppercase text-slate-400">A pagar</p><strong class="mt-2 block text-2xl">{{ payableOpen() | currency:'BRL' }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold uppercase text-slate-400">Pago</p><strong class="mt-2 block text-2xl">{{ paid() | currency:'BRL' }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold uppercase text-slate-400">Saldo previsto</p><strong class="mt-2 block text-2xl text-arqly-700">{{ receivableOpen() - payableOpen() | currency:'BRL' }}</strong></div>
      </div>

      <div class="card overflow-hidden">
        <div class="flex flex-col gap-3 border-b p-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex gap-2">
            <button type="button" class="btn-secondary" [class.bg-arqly-700]="tab() === 'RECEIVABLE'" [class.text-white]="tab() === 'RECEIVABLE'" (click)="tab.set('RECEIVABLE')">A receber</button>
            <button type="button" class="btn-secondary" [class.bg-arqly-700]="tab() === 'PAYABLE'" [class.text-white]="tab() === 'PAYABLE'" (click)="tab.set('PAYABLE')">A pagar</button>
          </div>
          <div class="flex items-center gap-2 text-sm text-slate-500"><lucide-icon name="CreditCard" size="18" />{{ filtered().length }} lançamentos</div>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-[820px] w-full text-left text-sm">
            <thead class="bg-slate-50 text-xs font-bold uppercase text-slate-500"><tr><th class="px-5 py-4">Descrição</th><th>Cliente / projeto</th><th>Valor</th><th>Liquidado</th><th>Saldo</th><th>Status</th><th class="px-5 text-right">Ações</th></tr></thead>
            <tbody>
              <tr *ngFor="let entry of filtered()" class="border-t border-slate-100">
                <td class="px-5 py-4"><p class="font-bold text-slate-900">{{ entry.description }}</p><p class="mt-1 text-xs text-slate-400">{{ entry.categoryName || 'Sem categoria' }}</p></td>
                <td>{{ entry.clientName || entry.projectName || 'Escritório' }}</td>
                <td>{{ entry.totalAmount | currency:'BRL' }}</td><td>{{ entry.settledAmount | currency:'BRL' }}</td><td class="font-bold">{{ entry.balance | currency:'BRL' }}</td>
                <td><span class="rounded-full px-2.5 py-1 text-xs font-bold" [class.bg-amber-50]="entry.status === 'OPEN'" [class.text-amber-700]="entry.status === 'OPEN'" [class.bg-arqly-50]="entry.status === 'SETTLED'" [class.text-arqly-700]="entry.status === 'SETTLED'">{{ statusLabel(entry.status) }}</span></td>
                <td class="px-5 text-right"><button type="button" class="btn-secondary px-3 py-2" (click)="openEntry(entry)">Parcelas</button></td>
              </tr>
              <tr *ngIf="!filtered().length"><td colspan="7" class="px-5 py-16 text-center text-slate-500">Nenhuma conta cadastrada nesta visão.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <div *ngIf="formOpen()" class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
      <form class="modal-panel card max-h-[92vh] w-full max-w-4xl space-y-5 overflow-y-auto p-6" [formGroup]="form" (ngSubmit)="save()">
        <div class="flex items-start justify-between gap-4"><div><p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Financeiro</p><h3 class="mt-1 text-xl font-extrabold">Novo lançamento</h3><p class="mt-1 text-sm text-slate-500">Registre uma conta a receber ou a pagar para acompanhar o fluxo do escritório.</p></div><button class="btn-secondary shrink-0 px-3 py-2" type="button" (click)="formOpen.set(false)" aria-label="Fechar"><lucide-icon name="X" size="18" /></button></div>
        <section class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4"><div class="mb-4"><p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Dados do lançamento</p><p class="mt-1 text-sm text-slate-500">Defina o tipo, os vínculos e o valor principal.</p></div><div class="grid gap-4 md:grid-cols-2"><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Tipo <span class="text-red-500">*</span></span><app-arqly-select formControlName="type" [options]="typeOptions" panelMode="fixed" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Categoria</span><app-arqly-select formControlName="categoryId" [options]="categoryOptions()" placeholder="Selecione uma categoria" panelMode="fixed" /></label><label class="space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Descrição <span class="text-red-500">*</span></span><input class="field" formControlName="description" placeholder="Ex.: Projeto arquitetônico - Residência Silva" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Cliente</span><app-arqly-select formControlName="clientId" [options]="clientOptions()" placeholder="Selecione o cliente" panelMode="fixed" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Projeto</span><app-arqly-select formControlName="projectId" [options]="projectOptions()" placeholder="Selecione o projeto" panelMode="fixed" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Beneficiário</span><input class="field" formControlName="counterpartyName" placeholder="Informe para uma conta a pagar" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Valor total <span class="text-red-500">*</span></span><app-arqly-currency-input formControlName="totalAmount" /></label></div></section>
        <section class="rounded-2xl border border-slate-200 bg-white p-4"><div class="mb-4"><p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Parcelamento</p><p class="mt-1 text-sm text-slate-500">As parcelas serão geradas automaticamente a partir destas informações.</p></div><div class="grid gap-4 md:grid-cols-3"><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Quantidade de parcelas <span class="text-red-500">*</span></span><input class="field" type="number" min="1" formControlName="installmentCount" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Primeiro vencimento <span class="text-red-500">*</span></span><app-arqly-date-picker formControlName="firstDueDate" placeholder="Selecione a data" /></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Periodicidade</span><app-arqly-select formControlName="periodicity" [options]="periodicityOptions" panelMode="fixed" /></label><label class="space-y-1 md:col-span-3"><span class="text-xs font-bold text-slate-500">Observações</span><textarea class="field min-h-24" formControlName="notes" placeholder="Informações internas sobre este lançamento"></textarea></label></div></section>
        <div class="flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end"><button class="btn-secondary" type="button" (click)="formOpen.set(false)">Cancelar</button><button class="btn-primary" type="submit" [disabled]="form.invalid"><lucide-icon name="Save" size="17" />Salvar lançamento</button></div>
      </form>
    </div>

    <div *ngIf="selectedEntry() as entry" class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
      <div class="modal-panel card max-h-[90vh] w-full max-w-3xl overflow-y-auto p-6"><div class="flex items-center justify-between"><div><h3 class="text-xl font-extrabold">Parcelas</h3><p class="mt-1 text-sm text-slate-500">{{ entry.description }}</p></div><button type="button" class="btn-secondary px-3 py-2" (click)="selectedEntry.set(null)"><lucide-icon name="X" size="18" /></button></div><div class="mt-5 space-y-3"><div *ngFor="let installment of entry.installments" class="flex flex-col gap-3 rounded-xl border border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"><div><p class="font-bold">{{ installment.description }}</p><p class="mt-1 text-sm text-slate-500">Vencimento: {{ installment.dueDate }} · Saldo: {{ installment.balance | currency:'BRL' }}</p></div><button *ngIf="installment.balance > 0" class="btn-primary" type="button" (click)="openSettlement(installment)">Registrar {{ entry.type === 'RECEIVABLE' ? 'recebimento' : 'pagamento' }}</button><span *ngIf="installment.balance <= 0" class="text-sm font-bold text-arqly-700">Liquidada</span></div></div></div>
    </div>
    <div *ngIf="settlementOpen()" class="modal-overlay fixed inset-0 z-[60] grid place-items-center p-4"><form class="modal-panel card w-full max-w-md space-y-4 p-6" [formGroup]="settlementForm" (ngSubmit)="settle()"><div class="flex items-center justify-between"><h3 class="text-xl font-extrabold">Registrar baixa</h3><button class="btn-secondary px-3 py-2" type="button" (click)="settlementOpen.set(false)"><lucide-icon name="X" size="18" /></button></div><p class="text-sm text-slate-500">Saldo disponível: {{ selectedInstallment()?.balance | currency:'BRL' }}</p><div class="space-y-1"><span class="field-label">Valor <span class="text-red-500">*</span></span><app-arqly-currency-input formControlName="amount" /></div><div class="space-y-1"><span class="field-label">Data <span class="text-red-500">*</span></span><app-arqly-date-picker formControlName="settlementDate" /></div><div class="space-y-1"><span class="field-label">Forma de pagamento <span class="text-red-500">*</span></span><app-arqly-select formControlName="paymentMethod" [options]="paymentOptions" panelMode="fixed" /></div><label class="space-y-1"><span class="field-label">Observação</span><textarea class="field min-h-20" formControlName="description"></textarea></label><div class="flex justify-end gap-3"><button class="btn-secondary" type="button" (click)="settlementOpen.set(false)">Cancelar</button><button class="btn-primary" [disabled]="settlementForm.invalid">Confirmar baixa</button></div></form></div>
  `
})
export class FinancialComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  readonly entries = signal<Entry[]>([]);
  readonly clients = signal<Option[]>([]);
  readonly projects = signal<Option[]>([]);
  readonly categories = signal<Option[]>([]);
  readonly tab = signal<'RECEIVABLE' | 'PAYABLE'>('RECEIVABLE');
  readonly formOpen = signal(false);
  readonly selectedEntry = signal<Entry | null>(null);
  readonly selectedInstallment = signal<Installment | null>(null);
  readonly settlementOpen = signal(false);
  readonly typeOptions = [{ label: 'Conta a receber', value: 'RECEIVABLE' }, { label: 'Conta a pagar', value: 'PAYABLE' }];
  readonly periodicityOptions = [{ label: 'Mensal', value: 'MONTHLY' }, { label: 'Semanal', value: 'WEEKLY' }];
  readonly paymentOptions = ['PIX','BANK_TRANSFER','CREDIT_CARD','DEBIT_CARD','CASH','BOLETO','CHECK','OTHER'].map(value => ({ value, label: ({ PIX:'PIX', BANK_TRANSFER:'Transferência', CREDIT_CARD:'Cartão de crédito', DEBIT_CARD:'Cartão de débito', CASH:'Dinheiro', BOLETO:'Boleto', CHECK:'Cheque', OTHER:'Outro' } as Record<string,string>)[value] }));
  readonly form = this.fb.nonNullable.group({ type: ['RECEIVABLE'], categoryId: [''], description: ['', Validators.required], clientId: [''], projectId: [''], totalAmount: [0, [Validators.required, Validators.min(0.01)]], installmentCount: [1, [Validators.required, Validators.min(1)]], firstDueDate: [new Date().toISOString().slice(0, 10), Validators.required], periodicity: ['MONTHLY'], counterpartyName: [''], notes: [''] });
  readonly settlementForm = this.fb.nonNullable.group({ amount: [0, [Validators.required, Validators.min(0.01)]], settlementDate: [new Date().toISOString().slice(0, 10), Validators.required], paymentMethod: ['PIX'], description: [''] });
  readonly filtered = computed(() => this.entries().filter(entry => entry.type === this.tab()));
  readonly receivableOpen = computed(() => this.sum('RECEIVABLE', 'balance'));
  readonly payableOpen = computed(() => this.sum('PAYABLE', 'balance'));
  readonly received = computed(() => this.sum('RECEIVABLE', 'settledAmount'));
  readonly paid = computed(() => this.sum('PAYABLE', 'settledAmount'));

  ngOnInit() { this.load(); this.loadOptions(); }
  showForm() { this.form.reset({ type: 'RECEIVABLE', categoryId: '', description: '', clientId: '', projectId: '', totalAmount: 0, installmentCount: 1, firstDueDate: new Date().toISOString().slice(0, 10), periodicity: 'MONTHLY', counterpartyName: '', notes: '' }); this.formOpen.set(true); }
  load() { this.http.get<Api<Entry[]>>('/api/financial/entries').subscribe({ next: response => this.entries.set(response.data || []), error: () => this.toast.error('Não foi possível carregar o financeiro.') }); }
  loadOptions() {
    this.http.get<Api<any>>('/api/tenant/clients?size=200').subscribe(response => this.clients.set((response.data?.content || response.data || []).map((item: any) => ({ id: item.id, displayName: item.displayName || item.name || item.legalName }))));
    this.http.get<Api<any>>('/api/tenant/projects?size=200').subscribe(response => this.projects.set((response.data?.content || response.data || []).map((item: any) => ({ id: item.id, name: item.name, code: item.code }))));
    this.http.get<Api<any>>('/api/financial/categories').subscribe(response => this.categories.set((response.data || []).map((item: any) => ({ id: item.id, name: item.name }))));
  }
  clientOptions() { return this.clients().map(item => ({ label: item.displayName || item.name || 'Cliente', value: item.id })); }
  projectOptions() { return this.projects().map(item => ({ label: `${item.code || ''} ${item.name || ''}`.trim(), value: item.id })); }
  categoryOptions() { return this.categories().map(item => ({ label: item.name || 'Categoria', value: item.id })); }
  openEntry(entry: Entry) { this.selectedEntry.set(entry); }
  save() { if (this.form.invalid) return; const value = this.form.getRawValue(); const payload = { ...value, categoryId: value.categoryId || null, clientId: value.clientId || null, projectId: value.projectId || null, issueDate: value.firstDueDate, competenceDate: value.firstDueDate, clientVisible: false }; this.http.post('/api/financial/entries', payload).subscribe({ next: () => { this.formOpen.set(false); this.load(); this.toast.success('Lançamento criado com sucesso.'); }, error: () => this.toast.error('Não foi possível criar o lançamento.') }); }
  openSettlement(installment: Installment) { this.selectedInstallment.set(installment); this.settlementForm.reset({ amount: installment.balance, settlementDate: new Date().toISOString().slice(0, 10), paymentMethod: 'PIX', description: '' }); this.settlementOpen.set(true); }
  settle() { const installment = this.selectedInstallment(); if (!installment || this.settlementForm.invalid) return; this.http.post(`/api/financial/installments/${installment.id}/settlements`, this.settlementForm.getRawValue()).subscribe({ next: () => { this.settlementOpen.set(false); this.selectedEntry.set(null); this.load(); this.toast.success('Baixa registrada.'); }, error: error => this.toast.error(error?.error?.message || 'Não foi possível registrar a baixa.') }); }
  statusLabel(status: string) { return ({ OPEN: 'Em aberto', PARTIALLY_SETTLED: 'Parcialmente liquidada', SETTLED: 'Liquidada', CANCELLED: 'Cancelada' } as Record<string, string>)[status] || status; }
  private sum(type: Entry['type'], key: 'balance' | 'settledAmount') { return this.entries().filter(entry => entry.type === type).reduce((total, entry) => total + Number(entry[key] || 0), 0); }
}
