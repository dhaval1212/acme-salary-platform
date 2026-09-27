import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [MatIconModule, NgIf],
  template: `
    <div class="page-header">
      <div class="page-header__icon-wrapper">
        <mat-icon class="page-header__icon">{{ icon }}</mat-icon>
      </div>
      <div>
        <h1 class="page-header__title">{{ title }}</h1>
        <p class="page-header__subtitle" *ngIf="subtitle">{{ subtitle }}</p>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 24px;
    }
    .page-header__icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: var(--radius-md);
      background: var(--color-primary-subtle);
      border: 1px solid var(--color-border);
      flex-shrink: 0;
    }
    .page-header__icon {
      font-size: 24px;
      width: 24px;
      height: 24px;
      color: var(--color-primary);
    }
    .page-header__title {
      margin: 0;
      font-size: var(--font-size-2xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      letter-spacing: -0.02em;
      line-height: var(--line-height-tight);
    }
    .page-header__subtitle {
      margin: 4px 0 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }
  `]
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() icon = 'info';
}
