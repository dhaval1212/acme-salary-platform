import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <div class="empty-state__icon-box">
        <mat-icon class="empty-state__icon">{{ icon }}</mat-icon>
      </div>
      <p class="empty-state__message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px 24px;
      color: var(--color-text-secondary);
    }
    .empty-state__icon-box {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 64px;
      height: 64px;
      border-radius: var(--radius-full);
      background-color: var(--color-surface-hover);
      border: 1px solid var(--color-border);
      margin-bottom: 16px;
    }
    .empty-state__icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: var(--color-text-muted);
    }
    .empty-state__message {
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-secondary);
      margin: 0;
      text-align: center;
    }
  `]
})
export class EmptyStateComponent {
  @Input() message = 'No data found';
  @Input() icon = 'inbox';
}
