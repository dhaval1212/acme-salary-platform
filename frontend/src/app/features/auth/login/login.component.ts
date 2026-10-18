import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  errorMessage = '';
  hidePassword = true;

  private returnUrl = '/employees';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Always build the form first — the template binds [formGroup]="form"
    // on first detectChanges, even when we redirect immediately after.
    this.form = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
    });

    // If already authenticated, redirect away from login page
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/employees']);
      return;
    }

    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/employees';
  }

  login(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate([this.returnUrl]);
      },
      error: () => {
        // Generic message — never reveal which field was wrong
        this.errorMessage = 'Invalid credentials. Please try again.';
        this.loading = false;
        // Clear password field after failed attempt
        this.form.get('password')?.setValue('');
      }
    });
  }
}
