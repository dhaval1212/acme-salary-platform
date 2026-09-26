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
      padding: 2px 10px;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 500;
      &--active  { background: #e8f5e9; color: #2e7d32; }
      &--inactive { background: #fce4ec; color: #c62828; }
    }
  `]
})
export class StatusBadgeComponent {
  @Input() status: EmployeeStatus = 'ACTIVE';
}
