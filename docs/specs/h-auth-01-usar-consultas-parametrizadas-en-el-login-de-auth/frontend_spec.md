# Especificación frontend — H-AUTH-01

## Alcance

Formulario Angular para `POST /auth/login`, usando exclusivamente los campos y la ruta del contrato aprobado. No se presupone una ruta de navegación, un formato específico del cuerpo de error ni una política para persistir el token.

## Campos y validación

| Campo | Tipo | Reglas |
|---|---|---|
| `username` | Texto | Obligatorio. No se imponen formato ni longitud adicionales. |
| `password` | Contraseña | Obligatorio. No se modifica ni se recorta su contenido. |

La validación se realiza antes de enviar. Si falta un campo, se muestran errores junto a la etiqueta correspondiente y no se hace la solicitud.

## Acciones y estados

- **Inicial/vacío:** se presenta el formulario y una indicación para completar las credenciales.
- **Enviar:** el formulario envía `username` y `password` a `POST /auth/login`.
- **Carga:** se indica que el inicio de sesión está en curso y se deshabilita el botón. También se ignoran invocaciones adicionales mientras la solicitud está activa.
- **Éxito (`200`):** se muestra el nombre de usuario confirmado por `user.username`. La respuesta completa, incluido `token`, se entrega al componente contenedor mediante el evento `authenticated`; no se muestra ni se persiste el token en este componente.
- **Credenciales inválidas (`401`):** se muestra un mensaje genérico de usuario o contraseña incorrectos. La interfaz no depende de que la respuesta de error contenga `token`.
- **Otros errores:** se muestra un mensaje genérico de conexión o servicio, sin presentar detalles del servidor, SQL ni credenciales.

Los valores del formulario se envían como datos literales: la interfaz no elimina ni transforma caracteres que podrían formar parte de las credenciales. No se registran contraseñas en mensajes ni en consola.

## Mensajes

- Inicial: “Completa tus credenciales para iniciar sesión.”
- Campo `username`: “Ingresa tu usuario.”
- Campo `password`: “Ingresa tu contraseña.”
- Validación del formulario: “Revisa los campos indicados antes de continuar.”
- Carga: “Iniciando sesión…”
- Éxito: “Sesión iniciada para {user.username}.”
- `401`: “Usuario o contraseña incorrectos. Verifica los datos e inténtalo de nuevo.”
- Sin conexión: “No se pudo conectar con el servicio. Inténtalo de nuevo.”
- Otros errores: “No se pudo completar el inicio de sesión. Inténtalo de nuevo.”

## Accesibilidad e integración

- Cada campo tiene una etiqueta visible, estado de validación anunciado y controles nativos operables con teclado.
- Los mensajes de carga y éxito usan una región de estado; los errores generales usan una alerta.
- El foco de teclado tiene un indicador visible.
- El componente emite `authenticated` con la respuesta aprobada para que el contenedor aplique la política de sesión existente. La aplicación debe tener `HttpClient` configurado y montar `app-login` donde corresponda. Se usa la ruta relativa aprobada; no se añade una ruta ni una URL base nueva.

## Pasos para comprobar el recorrido y sus errores

1. Ejecuta el backend y la aplicación Angular con la configuración de origen o proxy ya existente para que `/auth/login` sea accesible.
2. Deja ambos campos vacíos y envía el formulario. Comprueba que se muestran los errores de campo y no se envía una solicitud.
3. Inicia sesión con `student` y `Workshop123!`. Comprueba en la pestaña de red que se envían solo `username` y `password` a `POST /auth/login`, y que la vista confirma `user.username`.
4. Prueba una credencial incorrecta y comprueba el mensaje genérico correspondiente a `401`.
5. Como pruebas defensivas del contrato, envía `' OR '1'='1` como `username` (con la contraseña de prueba) y luego como `password` (con `student`). En ambos casos comprueba `401`, que la vista no presenta un token y que no se muestra información interna del servidor.
6. Simula falta de conexión y un error de servidor; comprueba los mensajes genéricos y que se puede volver a intentar.
7. Mientras la solicitud está pendiente, intenta enviar de nuevo. Comprueba que el botón queda deshabilitado y no se duplica la solicitud.
8. Desde el workspace Angular, ejecuta `ng test` y `ng build` (o los scripts equivalentes del proyecto). Para verificar la prueba backend del criterio aprobado, ejecuta `mvn test` desde `auth/`. Estos son pasos sugeridos; no se afirma que se hayan ejecutado.

```typescript
// auth-api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: {
    username: string;
  };
  token: string;
}

function isLoginResponse(value: unknown): value is LoginResponse {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const response = value as {
    user?: unknown;
    token?: unknown;
  };

  if (typeof response.token !== 'string' || typeof response.user !== 'object' || response.user === null) {
    return false;
  }

  const user = response.user as { username?: unknown };
  return typeof user.username === 'string';
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  constructor(private readonly http: HttpClient) {}

  login(credentials: LoginCredentials): Observable<LoginResponse> {
    return this.http.post<unknown>('/auth/login', credentials).pipe(
      map((body) => {
        if (!isLoginResponse(body)) {
          throw new Error('Unexpected login response');
        }

        return body;
      }),
    );
  }
}
```

```typescript
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
          this.authenticated.emit(response);
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
```

```html
<!-- login.component.html -->
<section class="login-card" aria-labelledby="login-title">
  <h1 id="login-title">Iniciar sesión</h1>

  <form
    [formGroup]="form"
    (ngSubmit)="submit()"
    [attr.aria-busy]="isSubmitting"
    novalidate
  >
    <div class="field">
      <label for="username">Usuario</label>
      <input
        id="username"
        name="username"
        type="text"
        formControlName="username"
        autocomplete="username"
        spellcheck="false"
        required
        [readonly]="isSubmitting"
        [attr.aria-invalid]="
          usernameControl.invalid && (usernameControl.touched || submitted) ? 'true' : 'false'
        "
        [attr.aria-describedby]="
          usernameControl.invalid && (usernameControl.touched || submitted)
            ? 'username-error'
            : null
        "
        (input)="onCredentialsChanged()"
      />
      <p
        *ngIf="usernameControl.invalid && (usernameControl.touched || submitted)"
        id="username-error"
        class="field-error"
      >
        Ingresa tu usuario.
      </p>
    </div>

    <div class="field">
      <label for="password">Contraseña</label>
      <input
        id="password"
        name="password"
        type="password"
        formControlName="password"
        autocomplete="current-password"
        required
        [readonly]="isSubmitting"
        [attr.aria-invalid]="
          passwordControl.invalid && (passwordControl.touched || submitted) ? 'true' : 'false'
        "
        [attr.aria-describedby]="
          passwordControl.invalid && (passwordControl.touched || submitted)
            ? 'password-error'
            : null
        "
        (input)="onCredentialsChanged()"
      />
      <p
        *ngIf="passwordControl.invalid && (passwordControl.touched || submitted)"
        id="password-error"
        class="field-error"
      >
        Ingresa tu contraseña.
      </p>
    </div>

    <p *ngIf="state === 'empty'" class="form-message" role="status">
      Completa tus credenciales para iniciar sesión.
    </p>

    <p *ngIf="state === 'loading'" class="form-message" role="status" aria-live="polite">
      Iniciando sesión…
    </p>

    <p *ngIf="state === 'error'" class="form-message error-message" role="alert">
      {{ errorMessage }}
    </p>

    <p *ngIf="state === 'success'" class="form-message success-message" role="status" aria-live="polite">
      Sesión iniciada para {{ successUsername }}.
    </p>

    <button type="submit" [disabled]="isSubmitting">
      {{ isSubmitting ? 'Iniciando sesión…' : 'Iniciar sesión' }}
    </button>
  </form>
</section>
```

```css
/* login.component.css */
:host {
  display: block;
  padding: 1rem;
}

.login-card {
  box-sizing: border-box;
  width: min(100%, 28rem);
  margin: 2rem auto;
  padding: clamp(1rem, 4vw, 2rem);
  border: 1px solid #cbd5e1;
  border-radius: 0.75rem;
  background: #fff;
  color: #172033;
}

h1 {
  margin-top: 0;
  font-size: 1.5rem;
}

.field {
  margin-block: 1rem;
}

label {
  display: block;
  margin-bottom: 0.4rem;
  font-weight: 600;
}

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 2.75rem;
  padding: 0.6rem 0.7rem;
  border: 1px solid #64748b;
  border-radius: 0.35rem;
  background: #fff;
  color: inherit;
  font: inherit;
}

input[aria-invalid='true'] {
  border-color: #b42318;
}

button {
  min-height: 2.75rem;
  padding: 0.6rem 1rem;
  border: 0;
  border-radius: 0.35rem;
  background: #174ea6;
  color: #fff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

input:focus-visible,
button:focus-visible {
  outline: 3px solid #f59e0b;
  outline-offset: 3px;
}

.form-message,
.field-error {
  margin-block: 0.5rem;
  line-height: 1.5;
}

.field-error,
.error-message {
  color: #9b1c1c;
}

.success-message {
  color: #17663a;
}

@media (max-width: 24rem) {
  .login-card {
    margin-block: 0.5rem;
  }
}
```