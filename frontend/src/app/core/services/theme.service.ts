import { Injectable, signal, PLATFORM_ID, Inject, Optional } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ThemeMode = 'light' | 'dark' | 'system';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly STORAGE_KEY = 'acme-theme-preference';
  private readonly isBrowser: boolean;

  readonly currentTheme = signal<ThemeMode>('system');
  readonly isDark = signal<boolean>(false);

  private mediaQuery?: MediaQueryList;

  constructor(@Optional() @Inject(PLATFORM_ID) platformId?: object) {
    this.isBrowser = platformId ? isPlatformBrowser(platformId) : (typeof window !== 'undefined');
    const initial = this.getInitialTheme();
    this.currentTheme.set(initial);

    if (this.isBrowser && typeof window !== 'undefined' && window.matchMedia) {
      this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      this.mediaQuery.addEventListener('change', () => {
        if (this.currentTheme() === 'system') {
          this.applyTheme('system');
        }
      });
      this.applyTheme(initial);
    }
  }

  setTheme(theme: ThemeMode): void {
    this.currentTheme.set(theme);
    if (this.isBrowser) {
      try {
        localStorage.setItem(this.STORAGE_KEY, theme);
      } catch {
        // Storage access may be restricted
      }
      this.applyTheme(theme);
    }
  }

  toggleTheme(): void {
    const next: ThemeMode = this.isDark() ? 'light' : 'dark';
    this.setTheme(next);
  }

  private getInitialTheme(): ThemeMode {
    if (!this.isBrowser || typeof localStorage === 'undefined') return 'system';
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY) as ThemeMode | null;
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored;
      }
    } catch {
      // Fall through to system default
    }
    return 'system';
  }

  private applyTheme(theme: ThemeMode): void {
    if (!this.isBrowser || typeof document === 'undefined') return;

    let dark = false;
    if (theme === 'dark') {
      dark = true;
    } else if (theme === 'light') {
      dark = false;
    } else {
      dark = this.mediaQuery ? this.mediaQuery.matches : false;
    }

    this.isDark.set(dark);

    const root = document.documentElement;
    const body = document.body;
    root.setAttribute('data-theme', dark ? 'dark' : 'light');
    if (body) {
      body.setAttribute('data-theme', dark ? 'dark' : 'light');
    }
    if (dark) {
      root.classList.add('dark-theme');
      root.classList.remove('light-theme');
      if (body) {
        body.classList.add('dark-theme');
        body.classList.remove('light-theme');
      }
    } else {
      root.classList.add('light-theme');
      root.classList.remove('dark-theme');
      if (body) {
        body.classList.add('light-theme');
        body.classList.remove('dark-theme');
      }
    }
  }
}
