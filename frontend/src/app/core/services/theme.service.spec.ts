import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.classList.remove('dark-theme', 'light-theme');

    TestBed.configureTestingModule({
      providers: [ThemeService]
    });
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.classList.remove('dark-theme', 'light-theme');
  });

  it('initializes with default or system theme', () => {
    expect(['light', 'dark', 'system']).toContain(service.currentTheme());
  });

  it('sets and applies light theme', () => {
    service.setTheme('light');
    expect(service.currentTheme()).toBe('light');
    expect(service.isDark()).toBeFalse();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(document.documentElement.classList.contains('light-theme')).toBeTrue();
    expect(document.documentElement.classList.contains('dark-theme')).toBeFalse();
    expect(localStorage.getItem('acme-theme-preference')).toBe('light');
  });

  it('sets and applies dark theme', () => {
    service.setTheme('dark');
    expect(service.currentTheme()).toBe('dark');
    expect(service.isDark()).toBeTrue();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark-theme')).toBeTrue();
    expect(document.documentElement.classList.contains('light-theme')).toBeFalse();
    expect(localStorage.getItem('acme-theme-preference')).toBe('dark');
  });

  it('toggles between dark and light themes', () => {
    service.setTheme('light');
    expect(service.isDark()).toBeFalse();

    service.toggleTheme();
    expect(service.isDark()).toBeTrue();
    expect(service.currentTheme()).toBe('dark');

    service.toggleTheme();
    expect(service.isDark()).toBeFalse();
    expect(service.currentTheme()).toBe('light');
  });

  it('reads stored preference on initialization', () => {
    localStorage.setItem('acme-theme-preference', 'dark');
    const newService = new ThemeService();
    expect(newService.currentTheme()).toBe('dark');
    expect(newService.isDark()).toBeTrue();
  });
});
