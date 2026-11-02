import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { AdminSession } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private base = `${inject(APP_CONFIG).apiBaseUrl}/admin/auth`;
  readonly session = signal<AdminSession | null>(null);

  check(): Observable<boolean> {
    return this.http.get<AdminSession>(`${this.base}/me`, { withCredentials: true }).pipe(
      tap((session) => this.session.set(session.authenticated ? session : null)),
      map((session) => session.authenticated),
      catchError(() => {
        this.session.set(null);
        return of(false);
      }),
    );
  }

  login(username: string, password: string): Observable<AdminSession> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.csrf().pipe(
      switchMap(() =>
        this.http.post<AdminSession>(`${this.base}/login`, body.toString(), {
          withCredentials: true,
          headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
        }),
      ),
      switchMap((session) => this.csrf().pipe(map(() => session))),
      tap((session) => this.session.set(session)),
    );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.base}/logout`, null, { withCredentials: true })
      .pipe(tap(() => this.session.set(null)));
  }

  csrf(): Observable<unknown> {
    return this.http.get(`${this.base}/csrf`, { withCredentials: true });
  }
}
