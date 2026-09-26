import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [MatIconModule, NgIf],
  template: `
    <div class="page-header">
      <mat-icon class="page-header__icon">{{ icon }}</mat-icon>
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
      gap: 12px;
      margin-bottom: 24px;
    }
    .page-header__icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #3f51b5;
    }
    .page-header__title {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 500;
    }
    .page-header__subtitle {
      margin: 4px 0 0;
      color: #666;
      font-size: 0.875rem;
    }
  `]
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() icon = 'info';
}
