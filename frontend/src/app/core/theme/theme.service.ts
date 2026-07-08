import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ApiResponse, AuthResponse, AuthScope } from '../auth/auth.models';

export type ColorPalette = 'arqly' | 'terracotta' | 'indigo';
export type ThemeMode = 'light' | 'dark';

const THEME_STORAGE_KEY = 'arqly.theme';
const PALETTE_STORAGE_KEY = 'arqly.palette';
const API_URL = 'http://localhost:8080/api';

interface AppearancePreferences {
  themeMode: ThemeMode;
  colorPalette: ColorPalette;
}

const FAVICON_COLORS: Record<ColorPalette, { primary: string; dark: string; soft: string }> = {
  arqly: { primary: '#066b64', dark: '#05534f', soft: '#d7f0ec' },
  terracotta: { primary: '#b0522f', dark: '#873c27', soft: '#fce8da' },
  indigo: { primary: '#524aae', dark: '#403987', soft: '#e5e8ff' }
};

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly http = inject(HttpClient);

  readonly mode = signal<ThemeMode>(this.readStoredMode());
  readonly palette = signal<ColorPalette>(this.readStoredPalette());

  constructor() {
    this.apply();
  }

  toggleMode() {
    this.mode.update((current) => current === 'dark' ? 'light' : 'dark');
    this.persist();
  }

  setPalette(palette: ColorPalette) {
    this.palette.set(palette);
    this.persist();
  }

  applyUserPreferences(user: AuthResponse | null) {
    if (!user) return;
    this.mode.set(user.themeMode || 'light');
    this.palette.set(user.colorPalette || 'arqly');
    this.apply();
    this.persistLocal();
  }

  refreshFromServer(scope: AuthScope) {
    this.http.get<ApiResponse<AppearancePreferences>>(`${API_URL}/${scope}/me/preferences`).subscribe({
      next: (response) => {
        this.mode.set(response.data.themeMode);
        this.palette.set(response.data.colorPalette);
        this.apply();
        this.persistLocal();
        this.updateStoredUser(response.data);
      }
    });
  }

  private apply() {
    const root = this.document.documentElement;
    root.classList.toggle('dark', this.mode() === 'dark');
    root.dataset['palette'] = this.palette();
    this.applyFavicon();
    this.applyThemeColor();
  }

  private persist() {
    this.apply();
    this.persistLocal();
    this.saveRemote();
  }

  private persistLocal() {
    localStorage.setItem(THEME_STORAGE_KEY, this.mode());
    localStorage.setItem(PALETTE_STORAGE_KEY, this.palette());
  }

  private saveRemote() {
    const token = localStorage.getItem('arqly.token');
    const scope = localStorage.getItem('arqly.scope') as AuthScope | null;
    if (!token || !scope) return;

    const preferences = { themeMode: this.mode(), colorPalette: this.palette() };
    this.updateStoredUser(preferences);
    this.http.put<ApiResponse<AppearancePreferences>>(`${API_URL}/${scope}/me/preferences`, preferences).subscribe({
      next: (response) => this.updateStoredUser(response.data)
    });
  }

  private updateStoredUser(preferences: AppearancePreferences) {
    const raw = localStorage.getItem('arqly.user');
    if (!raw) return;
    const user = JSON.parse(raw) as AuthResponse;
    localStorage.setItem('arqly.user', JSON.stringify({
      ...user,
      themeMode: preferences.themeMode,
      colorPalette: preferences.colorPalette
    }));
  }

  private applyFavicon() {
    const colors = FAVICON_COLORS[this.palette()];
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
        <path d="M9 55 27 9h13L22 55H9Z" fill="${colors.primary}"/>
        <path d="M33 9h12l14 46H45L33 20V9Z" fill="${colors.dark}"/>
        <path d="M24 55 33 32l9 23H24Z" fill="${colors.soft}"/>
      </svg>
    `;
    const encoded = `data:image/svg+xml,${encodeURIComponent(svg.trim())}`;
    let favicon = this.document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (!favicon) {
      favicon = this.document.createElement('link');
      favicon.rel = 'icon';
      this.document.head.appendChild(favicon);
    }
    favicon.type = 'image/svg+xml';
    favicon.href = encoded;
  }

  private applyThemeColor() {
    const color = FAVICON_COLORS[this.palette()].primary;
    let meta = this.document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (!meta) {
      meta = this.document.createElement('meta');
      meta.name = 'theme-color';
      this.document.head.appendChild(meta);
    }
    meta.content = this.mode() === 'dark' ? '#0b0b0c' : color;
  }

  private readStoredMode(): ThemeMode {
    return localStorage.getItem(THEME_STORAGE_KEY) === 'dark' ? 'dark' : 'light';
  }

  private readStoredPalette(): ColorPalette {
    const palette = localStorage.getItem(PALETTE_STORAGE_KEY);
    return palette === 'terracotta' || palette === 'indigo' ? palette : 'arqly';
  }
}
