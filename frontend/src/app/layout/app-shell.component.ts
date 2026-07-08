import { Component, computed, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../core/auth/auth.service';
import { AppearanceControlsComponent } from '../shared/components/appearance-controls.component';
import { LogoComponent } from '../shared/components/logo.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, LucideAngularModule, LogoComponent, AppearanceControlsComponent],
  template: `
    <div class="min-h-screen lg:grid lg:grid-cols-[280px_1fr]">
      <aside class="fixed inset-y-0 left-0 z-20 hidden w-[280px] border-r border-slate-200/80 bg-white/90 p-6 backdrop-blur lg:flex lg:flex-col">
        <app-logo />

        <nav class="mt-10 space-y-2">
          @for (item of navItems(); track item.label) {
            <a [routerLink]="item.path"
               [class.bg-arqly-600]="isActive(item)"
               [class.text-white]="isActive(item)"
               [class.shadow-lg]="isActive(item)"
               class="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-slate-600 transition hover:bg-arqly-50 hover:text-arqly-700">
              <lucide-icon [name]="item.icon" size="20"></lucide-icon>
              {{ item.label }}
            </a>
          }
        </nav>

        <button class="mt-auto flex items-center gap-3 rounded-2xl border border-slate-200 p-3 text-left" (click)="auth.logout()">
          <div class="grid h-11 w-11 place-items-center rounded-full bg-arqly-100 font-bold text-arqly-700">
            {{ initials() }}
          </div>
          <div class="min-w-0">
            <p class="truncate text-sm font-bold">{{ auth.currentUser()?.name || 'Usuário' }}</p>
            <p class="truncate text-xs text-slate-500">{{ roleLabel() }}</p>
          </div>
          <lucide-icon class="ml-auto text-slate-400" name="LogOut" size="18"></lucide-icon>
        </button>
      </aside>

      <main class="lg:col-start-2">
        <header class="sticky top-0 z-10 flex items-center gap-3 border-b border-slate-200/70 bg-white/75 px-4 py-3 backdrop-blur lg:gap-4 lg:px-10 lg:py-4">
          <button class="grid h-11 w-11 place-items-center rounded-2xl border border-slate-200 bg-white lg:hidden" type="button" (click)="mobileMenuOpen.set(true)" aria-label="Abrir menu">
            <lucide-icon name="Menu" size="20"></lucide-icon>
          </button>
          <div class="min-w-0">
            <p class="text-xs font-bold uppercase tracking-[0.18em] text-arqly-600">Arqly</p>
            <h1 class="truncate text-lg font-extrabold lg:text-xl">{{ title() }}</h1>
          </div>
          <div class="ml-auto hidden w-full max-w-xl items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-400 shadow-sm md:flex">
            <lucide-icon name="Search" size="20"></lucide-icon>
            <span class="text-sm">Buscar projetos, tarefas, documentos...</span>
          </div>
          <button class="hidden h-11 w-11 place-items-center rounded-2xl border border-slate-200 bg-white sm:grid">
            <lucide-icon name="Bell" size="20"></lucide-icon>
          </button>
          <app-appearance-controls />
        </header>

        <section class="p-4 pb-28 lg:p-10">
          <router-outlet />
        </section>
      </main>

      @if (mobileMenuOpen()) {
        <div class="modal-overlay fixed inset-0 z-40 lg:hidden" (click)="mobileMenuOpen.set(false)"></div>
        <aside class="fixed inset-y-0 left-0 z-50 flex w-[min(22rem,86vw)] flex-col border-r border-slate-200 bg-white p-5 shadow-2xl lg:hidden">
          <div class="flex items-center justify-between">
            <app-logo />
            <button class="btn-secondary px-3 py-2" type="button" (click)="mobileMenuOpen.set(false)" aria-label="Fechar menu">
              <lucide-icon name="X" size="18"></lucide-icon>
            </button>
          </div>
          <nav class="mt-8 space-y-2">
            @for (item of navItems(); track item.label) {
              <a [routerLink]="item.path"
                 [class.bg-arqly-600]="isActive(item)"
                 [class.text-white]="isActive(item)"
                 [class.shadow-lg]="isActive(item)"
                 class="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-slate-600 transition hover:bg-arqly-50 hover:text-arqly-700"
                 (click)="mobileMenuOpen.set(false)">
                <lucide-icon [name]="item.icon" size="20"></lucide-icon>
                {{ item.label }}
              </a>
            }
          </nav>
          <button class="mt-auto flex items-center gap-3 rounded-2xl border border-slate-200 p-3 text-left" (click)="auth.logout()">
            <div class="grid h-11 w-11 place-items-center rounded-full bg-arqly-100 font-bold text-arqly-700">
              {{ initials() }}
            </div>
            <div class="min-w-0">
              <p class="truncate text-sm font-bold">{{ auth.currentUser()?.name || 'Usuário' }}</p>
              <p class="truncate text-xs text-slate-500">{{ roleLabel() }}</p>
            </div>
            <lucide-icon class="ml-auto text-slate-400" name="LogOut" size="18"></lucide-icon>
          </button>
        </aside>
      }

      <nav class="fixed inset-x-3 bottom-3 z-30 grid grid-cols-4 rounded-3xl border border-slate-200 bg-white p-2 shadow-[0_18px_60px_rgba(15,23,42,0.18)] lg:hidden">
        @for (item of mobileNavItems(); track item.label) {
          <a [routerLink]="item.path"
             [class.bg-arqly-600]="isActive(item)"
             [class.text-white]="isActive(item)"
             class="flex min-w-0 flex-col items-center gap-1 rounded-2xl px-2 py-2 text-[0.68rem] font-bold text-slate-500 transition hover:bg-arqly-50 hover:text-arqly-700">
            <lucide-icon [name]="item.icon" size="19"></lucide-icon>
            <span class="max-w-full truncate">{{ item.label }}</span>
          </a>
        }
      </nav>
    </div>
  `
})
export class AppShellComponent {
  readonly mobileMenuOpen = signal(false);
  readonly navItems = computed(() => this.auth.scope() === 'platform'
    ? [
        { label: 'Dashboard', path: '/admin/dashboard', icon: 'LayoutDashboard' },
        { label: 'Usuários', path: '/admin/users', icon: 'UserCog' },
        { label: 'Tenants', path: '/admin/tenants', icon: 'Building2' },
        { label: 'Configurações', path: '/admin/settings', icon: 'Settings' }
      ]
    : [
        { label: 'Dashboard', path: '/app/dashboard', icon: 'LayoutDashboard' },
        { label: 'Projetos', path: '/app/dashboard', icon: 'Folder' },
        { label: 'Tarefas', path: '/app/dashboard', icon: 'ListChecks' },
        { label: 'Documentos', path: '/app/dashboard', icon: 'FileText' },
        { label: 'Equipe', path: '/app/dashboard', icon: 'Users' }
      ]);
  readonly mobileNavItems = computed(() => this.navItems().slice(0, 4));

  constructor(readonly auth: AuthService, private readonly router: Router) {}

  title() {
    return this.auth.scope() === 'platform' ? 'Administração da plataforma' : 'Bom dia, arquiteto';
  }

  initials() {
    return (this.auth.currentUser()?.name || 'AR').split(' ').slice(0, 2).map((word) => word[0]).join('').toUpperCase();
  }

  roleLabel() {
    return this.auth.scope() === 'platform' ? 'Administrador da plataforma' : 'Usuário do tenant';
  }

  isActive(item: { label: string; path: string }) {
    return this.navItems().find((navItem) => this.router.url.startsWith(navItem.path))?.label === item.label;
  }
}
