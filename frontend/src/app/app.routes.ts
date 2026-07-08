import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { AppShellComponent } from './layout/app-shell.component';
import { LoginComponent } from './features/auth/login.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password.component';
import { PasswordFlowComponent } from './features/auth/password-flow.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard.component';
import { TenantsComponent } from './features/admin/tenants.component';
import { PlatformUsersComponent } from './features/admin/platform-users.component';
import { SettingsComponent } from './features/settings/settings.component';
import { TenantDashboardComponent } from './features/tenant/tenant-dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, data: { scope: 'tenant' } },
  { path: 'login/admin', component: LoginComponent, data: { scope: 'platform' } },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'first-access', component: PasswordFlowComponent, data: { firstAccess: true } },
  { path: 'reset-password', component: PasswordFlowComponent },
  {
    path: 'admin',
    component: AppShellComponent,
    canActivate: [authGuard('platform')],
    data: { scope: 'platform' },
    children: [
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'tenants', component: TenantsComponent },
      { path: 'users', component: PlatformUsersComponent },
      { path: 'settings', component: SettingsComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  {
    path: 'app',
    component: AppShellComponent,
    canActivate: [authGuard('tenant')],
    data: { scope: 'tenant' },
    children: [
      { path: 'dashboard', component: TenantDashboardComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
