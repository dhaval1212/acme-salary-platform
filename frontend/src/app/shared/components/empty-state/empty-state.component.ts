import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon class="empty-state__icon">{{ icon }}</mat-icon>
      <p class="empty-state__message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; padding: 48px 24px; color: #9e9e9e;
      &__icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; }
      &__message { font-size: 1rem; }
    }
  `]
})
export class EmptyStateComponent {
  @Input() message = 'No data found';
  @Input() icon = 'inbox';
}
