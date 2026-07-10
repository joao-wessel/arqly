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
import { TenantUsersComponent } from './features/tenant/tenant-users.component';
import { ChangePasswordComponent } from './features/account/change-password.component';
import { ClientsComponent } from './features/tenant/clients.component';
import { ClientPortalComponent } from './features/portal/client-portal.component';
import { ProposalPortalComponent } from './features/portal/proposal-portal.component';
import { ServiceCatalogComponent } from './features/tenant/service-catalog.component';
import { ProposalsComponent } from './features/tenant/proposals.component';
import { ProjectDetailComponent, ProjectsComponent } from './features/tenant/projects.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, data: { scope: 'tenant' } },
  { path: 'login/admin', component: LoginComponent, data: { scope: 'platform' } },
  { path: 'forgot-password/admin', component: ForgotPasswordComponent, data: { scope: 'platform' } },
  { path: 'forgot-password', component: ForgotPasswordComponent, data: { scope: 'tenant' } },
  { path: 'first-access', component: PasswordFlowComponent, data: { firstAccess: true } },
  { path: 'reset-password/admin', component: PasswordFlowComponent, data: { scope: 'platform' } },
  { path: 'reset-password', component: PasswordFlowComponent, data: { scope: 'tenant' } },
  { path: 'portal/:token', component: ClientPortalComponent },
  { path: 'portal/:token/proposals/:proposalId', component: ProposalPortalComponent },
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
      { path: 'account/password', component: ChangePasswordComponent },
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
      { path: 'clients', component: ClientsComponent },
      { path: 'services', component: ServiceCatalogComponent },
      { path: 'proposals', component: ProposalsComponent },
      { path: 'projects', component: ProjectsComponent },
      { path: 'projects/:id', component: ProjectDetailComponent },
      { path: 'users', component: TenantUsersComponent },
      { path: 'account/password', component: ChangePasswordComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' }
];
