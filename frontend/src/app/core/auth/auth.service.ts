import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { ThemeService } from '../theme/theme.service';
import { ApiResponse, AuthResponse, AuthScope } from './auth.models';

const API_URL = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSignal = signal<AuthResponse | null>(this.readUser());
  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isAuthenticated = computed(() => !!this.currentUserSignal());

  constructor(private readonly http: HttpClient, private readonly router: Router, private readonly theme: ThemeService) {
    this.theme.applyUserPreferences(this.currentUserSignal());
    const scope = this.scope();
    if (this.token() && scope) {
      this.theme.refreshFromServer(scope);
    }
  }

  login(scope: AuthScope, email: string, password: string) {
    const path = scope === 'platform' ? 'auth/platform/login' : 'auth/tenant/login';
    return this.http.post<ApiResponse<AuthResponse>>(`${API_URL}/${path}`, { email, password }).pipe(
      tap((response) => this.persist(scope, response.data))
    );
  }

  forgotPassword(scope: AuthScope, email: string) {
    const path = scope === 'platform' ? 'auth/platform/forgot-password' : 'auth/tenant/forgot-password';
    return this.http.post<ApiResponse<void>>(`${API_URL}/${path}`, { email });
  }

  resetPassword(scope: AuthScope, token: string, password: string, firstAccess = false, name?: string) {
    const path = firstAccess ? 'auth/tenant/first-access' : scope === 'platform' ? 'auth/platform/reset-password' : 'auth/tenant/reset-password';
    const body = firstAccess ? { token, password, name } : { token, password };
    return this.http.post<ApiResponse<void>>(`${API_URL}/${path}`, body);
  }

  changePassword(currentPassword: string, newPassword: string) {
    const scope = this.scope() || 'tenant';
    const path = scope === 'platform' ? 'platform/me/password' : 'tenant/me/password';
    return this.http.put<ApiResponse<void>>(`${API_URL}/${path}`, { currentPassword, newPassword });
  }

  token(): string | null {
    return localStorage.getItem('arqly.token');
  }

  scope(): AuthScope | null {
    return localStorage.getItem('arqly.scope') as AuthScope | null;
  }

  logout() {
    localStorage.removeItem('arqly.token');
    localStorage.removeItem('arqly.user');
    localStorage.removeItem('arqly.scope');
    this.currentUserSignal.set(null);
    void this.router.navigateByUrl('/login');
  }

  private persist(scope: AuthScope, user: AuthResponse) {
    localStorage.setItem('arqly.token', user.accessToken);
    localStorage.setItem('arqly.user', JSON.stringify(user));
    localStorage.setItem('arqly.scope', scope);
    this.currentUserSignal.set(user);
    this.theme.applyUserPreferences(user);
  }

  private readUser(): AuthResponse | null {
    const raw = localStorage.getItem('arqly.user');
    return raw ? JSON.parse(raw) as AuthResponse : null;
  }
}
