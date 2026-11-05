// login.component.ts
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  EventEmitter,
  inject,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Router } from '@angular/router';
import { AuthApiService, LoginResponse } from './auth-api.service';

type LoginState = 'empty' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  @Output() readonly authenticated = new EventEmitter<LoginResponse>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  state: LoginState = 'empty';
  isSubmitting = false;
  submitted = false;
  errorMessage = '';
  successUsername = '';

  get usernameControl() {
    return this.form.controls.username;
  }

  get passwordControl() {
    return this.form.controls.password;
  }

  submit(): void {
    if (this.isSubmitting) {
      return;
    }

    this.submitted = true;
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.state = 'error';
      this.errorMessage = 'Revisa los campos indicados antes de continuar.';
      return;
    }

    const credentials = this.form.getRawValue();

    this.isSubmitting = true;
    this.state = 'loading';
    this.successUsername = '';

    this.authApi
      .login(credentials)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSubmitting = false;
        }),
      )
      .subscribe({
        next: (response) => {
          this.state = 'success';
          this.successUsername = response.user.username;
          this.submitted = false;
          this.form.controls.password.reset('');
          localStorage.setItem('workshop_token', response.token);
          localStorage.setItem('workshop_user', JSON.stringify(response.user));
          this.authenticated.emit(response);
          void this.router.navigate(['/dashboard']);
        },
        error: (error: unknown) => {
          this.state = 'error';
          this.errorMessage = this.getErrorMessage(error);
        },
      });
  }

  onCredentialsChanged(): void {
    if (this.isSubmitting) {
      return;
    }

    this.errorMessage = '';
    this.successUsername = '';
    this.state = 'empty';
  }

  private getErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo completar el inicio de sesión. Inténtalo de nuevo.';
    }

    if (error.status === 401) {
      return 'Usuario o contraseña incorrectos. Verifica los datos e inténtalo de nuevo.';
    }

    if (error.status === 0) {
      return 'No se pudo conectar con el servicio. Inténtalo de nuevo.';
    }

    return 'No se pudo completar el inicio de sesión. Inténtalo de nuevo.';
  }
}