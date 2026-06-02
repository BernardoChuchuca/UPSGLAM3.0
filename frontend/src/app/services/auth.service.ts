import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

const BACKEND_URL = `${environment.apiUrl}/auth`;

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
}

export interface AuthResponse {
  token: string;
  profile: any;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${BACKEND_URL}/login`, data).pipe(
      tap(res => {
        localStorage.setItem('token', res.token);
        localStorage.setItem('profile', JSON.stringify(res.profile));
      })
    );
  }

  register(data: RegisterRequest): Observable<any> {
    return this.http.post(`${BACKEND_URL}/register`, data);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('profile');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getProfile(): any {
    const p = localStorage.getItem('profile');
    return p ? JSON.parse(p) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}