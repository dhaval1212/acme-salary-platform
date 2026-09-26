import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" opened class="shell__sidenav">
        <div class="shell__logo">
          <mat-icon>account_balance</mat-icon>
          <span>ACME Comp Hub</span>
        </div>
        <mat-nav-list>
          @for (item of navItems; track item.route) {
            <a mat-list-item
               [routerLink]="item.route"
               routerLinkActive="active-link">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="shell__content">
        <mat-toolbar color="primary" class="shell__toolbar">
          <span>ACME Compensation Hub</span>
        </mat-toolbar>
        <main class="shell__main">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell {
      height: 100vh;

      &__sidenav {
        width: 220px;
        background: #1a237e;
        color: white;

        mat-nav-list a { color: rgba(255,255,255,0.85); }
        .active-link { background: rgba(255,255,255,0.15); color: white; }
      }

      &__logo {
        display: flex; align-items: center; gap: 10px;
        padding: 20px 16px; font-size: 1rem; font-weight: 500;
        border-bottom: 1px solid rgba(255,255,255,0.12);
        mat-icon { color: white; }
        span { color: white; }
      }

      &__toolbar { position: sticky; top: 0; z-index: 10; }

      &__main { padding: 24px; max-width: 1200px; margin: 0 auto; }
    }
  `]
})
export class ShellComponent {
  navItems: NavItem[] = [
    { label: 'Employees',  icon: 'people',      route: '/employees'  },
    { label: 'Analytics',  icon: 'bar_chart',   route: '/analytics'  },
  ];
}
