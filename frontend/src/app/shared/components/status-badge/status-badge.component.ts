import { Component, Input } from '@angular/core';
import { NgClass } from '@angular/common';
import { EmployeeStatus } from '../../../core/models/employee.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="badge" [ngClass]="status === 'ACTIVE' ? 'badge--active' : 'badge--inactive'">
      {{ status === 'ACTIVE' ? 'Active' : 'Inactive' }}
    </span>
  `,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: var(--radius-full);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
      line-height: 1.2;
      letter-spacing: 0.02em;

      &::before {
        content: '';
        display: inline-block;
        width: 6px;
        height: 6px;
        border-radius: var(--radius-full);
      }
    }

    .badge--active {
      background: var(--color-success-bg);
      color: var(--color-success);
      border: 1px solid var(--color-success-border);

      &::before {
        background-color: var(--color-success);
      }
    }

    .badge--inactive {
      background: var(--color-error-bg);
      color: var(--color-error);
      border: 1px solid var(--color-error-border);

      &::before {
        background-color: var(--color-error);
      }
    }
  `]
})
export class StatusBadgeComponent {
  @Input() status: EmployeeStatus = 'ACTIVE';
}
