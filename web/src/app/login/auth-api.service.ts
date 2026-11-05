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