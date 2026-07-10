import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ToastService } from '../../shared/components/toast/toast.service';

interface PlatformUser {
  id: string;
  name: string;
  email: string;
  active: boolean;
  roles: string[];
  lastAccessAt: string | null;
  createdAt: string;
}

interface Page<T> {
  content: T[];
}

@Component({
  selector: 'app-platform-users',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, LucideAngularModule],
  template: `
    <section class="card overflow-hidden">
      <div class="flex flex-col gap-4 border-b border-slate-200 p-6 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 class="text-3xl font-extrabold tracking-tight">Usuários administrativos</h2>
          <p class="mt-2 text-slate-500">Administradores globais da plataforma Arqly.</p>
        </div>
        <button class="btn-primary" type="button" (click)="openUserModal()">
          <lucide-icon name="UserPlus" size="18"></lucide-icon>
          Novo administrador
        </button>
      </div>

      <div class="hidden overflow-x-auto md:block">
        <table class="w-full min-w-[860px] text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th class="px-6 py-4">Nome</th>
              <th class="px-6 py-4">E-mail</th>
              <th class="px-6 py-4">Status</th>
              <th class="px-6 py-4">Último acesso</th>
              <th class="px-6 py-4">Criado em</th>
              <th class="px-6 py-4 text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            @for (user of users(); track user.id) {
              <tr class="border-t border-slate-100">
                <td class="px-6 py-4">
                  <p class="font-bold">{{ user.name }}</p>
                  <p class="text-xs text-slate-500">ROLE_PLATFORM_ADMIN</p>
                </td>
                <td class="px-6 py-4 text-slate-600">{{ user.email }}</td>
                <td class="px-6 py-4">
                  <span class="rounded-full px-3 py-1 text-xs font-bold"
                        [class.bg-arqly-50]="user.active"
                        [class.text-arqly-700]="user.active"
                        [class.bg-slate-100]="!user.active"
                        [class.text-slate-500]="!user.active">
                    {{ user.active ? 'Ativo' : 'Inativo' }}
                  </span>
                </td>
                <td class="px-6 py-4 text-slate-500">{{ user.lastAccessAt ? (user.lastAccessAt | date:'short') : '-' }}</td>
                <td class="px-6 py-4 text-slate-500">{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                <td class="px-6 py-4">
                  <div class="flex justify-end gap-2">
                    <button class="btn-secondary px-3 py-2" type="button" title="Editar" (click)="openUserModal(user)">
                      <lucide-icon name="Pencil" size="16"></lucide-icon>
                    </button>
                    <button class="btn-secondary px-3 py-2 text-red-600 hover:border-red-200 hover:text-red-700" type="button" title="Excluir" (click)="openDeleteModal(user)">
                      <lucide-icon name="Trash2" size="16"></lucide-icon>
                    </button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="6" class="px-6 py-12 text-center text-slate-500">Nenhum administrador cadastrado.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="space-y-3 p-4 md:hidden">
        @for (user of users(); track user.id) {
          <article class="rounded-2xl border border-slate-200 bg-white p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate font-extrabold">{{ user.name }}</p>
                <p class="mt-1 truncate text-sm text-slate-500">{{ user.email }}</p>
              </div>
              <span class="shrink-0 rounded-full px-3 py-1 text-xs font-bold"
                    [class.bg-arqly-50]="user.active"
                    [class.text-arqly-700]="user.active"
                    [class.bg-slate-100]="!user.active"
                    [class.text-slate-500]="!user.active">
                {{ user.active ? 'Ativo' : 'Inativo' }}
              </span>
            </div>
            <p class="mt-4 text-xs text-slate-500">Último acesso: {{ user.lastAccessAt ? (user.lastAccessAt | date:'short') : '-' }}</p>
            <div class="mt-4 grid grid-cols-2 gap-2">
              <button class="btn-secondary px-3 py-2" type="button" (click)="openUserModal(user)">
                <lucide-icon name="Pencil" size="16"></lucide-icon>
                Editar
              </button>
              <button class="btn-secondary px-3 py-2 text-red-600 hover:border-red-200 hover:text-red-700" type="button" (click)="openDeleteModal(user)">
                <lucide-icon name="Trash2" size="16"></lucide-icon>
                Excluir
              </button>
            </div>
          </article>
        } @empty {
          <p class="py-8 text-center text-sm text-slate-500">Nenhum administrador cadastrado.</p>
        }
      </div>
    </section>

    @if (userModalOpen()) {
      <div class="modal-overlay fixed inset-0 z-50 grid place-items-center p-4">
        <form class="modal-panel card w-full max-w-xl space-y-6 p-6" [formGroup]="userForm" (ngSubmit)="saveUser()">
          <div class="flex items-center justify-between">
            <h3 class="text-xl font-extrabold">{{ editingUser() ? 'Editar administrador' : 'Novo administrador' }}</h3>
            <button class="btn-secondary px-3 py-2" type="button" (click)="closeUserModal()">
              <lucide-icon name="X" size="18"></lucide-icon>
            </button>
          </div>

          <section class="space-y-3">
            <div>
              <p class="text-sm font-extrabold text-slate-900">Dados do usuário</p>
              <p class="mt-1 text-xs text-slate-500">Identificação e credenciais de acesso à área administrativa.</p>
            </div>
            <div class="grid gap-4 md:grid-cols-2">
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">Nome <span class="text-red-500">*</span></span>
                <input class="field" placeholder="Nome completo" formControlName="name">
              </label>
              <label class="space-y-1">
                <span class="text-xs font-bold text-slate-500">E-mail <span class="text-red-500">*</span></span>
                <input class="field" type="email" placeholder="admin@arqly.com" formControlName="email">
              </label>
              <label class="space-y-1 md:col-span-2">
                <span class="text-xs font-bold text-slate-500">{{ editingUser() ? 'Nova senha' : 'Senha inicial' }} <span class="text-red-500" [class.hidden]="editingUser()">*</span></span>
                <input class="field" type="password" [placeholder]="editingUser() ? 'Opcional' : 'Senha inicial'" formControlName="password">
              </label>
            </div>
          </section>

          <section class="space-y-3 border-t border-slate-200 pt-5">
            <div>
              <p class="text-sm font-extrabold text-slate-900">Acesso</p>
              <p class="mt-1 text-xs text-slate-500">Controle se o administrador pode entrar na plataforma.</p>
            </div>
            <label class="flex items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-slate-50/70 px-4 py-3">
              <span>
                <span class="block text-sm font-bold text-slate-800">Usuário ativo</span>
                <span class="mt-1 block text-xs text-slate-500">Usuários inativos não conseguem autenticar.</span>
              </span>
              <input class="checkbox" type="checkbox" formControlName="active">
            </label>
          </section>

          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeUserModal()">Cancelar</button>
            <button class="btn-primary" type="submit">
              <lucide-icon name="Save" size="18"></lucide-icon>
              Salvar
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
              <h3 class="text-xl font-extrabold">Excluir administrador</h3>
              <p class="text-sm text-slate-500">Esta ação não pode ser desfeita.</p>
            </div>
          </div>
          <p class="text-sm text-slate-600">Deseja excluir <strong>{{ selectedUser()?.name }}</strong>?</p>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" type="button" (click)="closeDeleteModal()">Cancelar</button>
            <button class="btn-primary bg-red-600 hover:bg-red-700" type="button" (click)="deleteUser()">Excluir</button>
          </div>
        </div>
      </div>
    }
  `
})
export class PlatformUsersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  readonly users = signal<PlatformUser[]>([]);
  readonly userModalOpen = signal(false);
  readonly deleteModalOpen = signal(false);
  readonly editingUser = signal<PlatformUser | null>(null);
  readonly selectedUser = signal<PlatformUser | null>(null);
  readonly userForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    active: [true]
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.http.get<ApiResponse<Page<PlatformUser>>>('http://localhost:8080/api/platform/users')
      .subscribe((response) => this.users.set(response.data.content));
  }

  openUserModal(user?: PlatformUser) {
    this.editingUser.set(user || null);
    const passwordValidators = user ? [] : [Validators.required, Validators.minLength(8)];
    this.userForm.controls.password.setValidators(passwordValidators);
    this.userForm.controls.password.updateValueAndValidity();
    this.userForm.reset({
      name: user?.name || '',
      email: user?.email || '',
      password: '',
      active: user?.active ?? true
    });
    this.userModalOpen.set(true);
  }

  closeUserModal() {
    this.userModalOpen.set(false);
  }

  saveUser() {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      this.toast.validation('Preencha nome, e-mail e senha inicial quando necessário.');
      return;
    }
    const user = this.editingUser();
    const request = user
      ? this.http.put<ApiResponse<PlatformUser>>(`http://localhost:8080/api/platform/users/${user.id}`, this.userForm.getRawValue())
      : this.http.post<ApiResponse<PlatformUser>>('http://localhost:8080/api/platform/users', this.userForm.getRawValue());
    request.subscribe({
      next: () => {
        this.closeUserModal();
        this.load();
        this.toast.success(user ? 'Usuário atualizado' : 'Usuário criado', 'As informações foram salvas.');
      },
      error: () => this.toast.error('Não foi possível salvar', 'Verifique se o e-mail já está cadastrado.')
    });
  }

  openDeleteModal(user: PlatformUser) {
    this.selectedUser.set(user);
    this.deleteModalOpen.set(true);
  }

  closeDeleteModal() {
    this.deleteModalOpen.set(false);
  }

  deleteUser() {
    const user = this.selectedUser();
    if (!user) return;
    this.http.delete(`http://localhost:8080/api/platform/users/${user.id}`).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.load();
        this.toast.success('Usuário excluído');
      },
      error: () => this.toast.error('Não foi possível excluir', 'Pelo menos um administrador deve permanecer ativo.')
    });
  }
}
