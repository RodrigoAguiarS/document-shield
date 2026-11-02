import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <main class="login-page">
      <section class="login-panel" aria-labelledby="login-title">
        <a class="login-brand" routerLink="/dashboard"
          ><span><i class="pi pi-shield"></i></span>Document Shield</a
        >
        <div class="login-copy">
          <h1 id="login-title">Acesso administrativo</h1>
          <p>
            Entre com a conta provisionada no servidor para gerenciar integrações e credenciais.
          </p>
        </div>
        <form (ngSubmit)="submit()" novalidate>
          <div class="field">
            <label for="admin-username">Usuário</label>
            <input
              id="admin-username"
              class="control"
              name="username"
              [(ngModel)]="username"
              autocomplete="username"
              required
              [attr.aria-invalid]="error() ? true : null"
            />
          </div>
          <div class="field">
            <label for="admin-password">Senha</label>
            <input
              id="admin-password"
              class="control"
              name="password"
              type="password"
              [(ngModel)]="password"
              autocomplete="current-password"
              required
              [attr.aria-invalid]="error() ? true : null"
            />
          </div>
          @if (error()) {
            <p class="form-error" role="alert">
              <i class="pi pi-exclamation-circle"></i>{{ error() }}
            </p>
          }
          <button
            class="btn btn-primary"
            type="submit"
            [disabled]="loading() || !username.trim() || !password"
          >
            @if (loading()) {
              <i class="pi pi-spin pi-spinner"></i>Entrando…
            } @else {
              Entrar
            }
          </button>
        </form>
        <p class="login-note">
          <i class="pi pi-lock"></i>A sessão é mantida pelo servidor. A senha não é armazenada no
          navegador.
        </p>
      </section>
    </main>
  `,
  styles: [
    `
      .login-page {
        min-height: 100vh;
        display: grid;
        place-items: center;
        padding: 1.25rem;
        background: var(--ds-midnight-black);
      }
      .login-panel {
        width: min(27rem, 100%);
        padding: 1.5rem;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: var(--radius);
      }
      .login-brand {
        display: flex;
        align-items: center;
        gap: 0.65rem;
        font-weight: 750;
        font-size: 0.9rem;
      }
      .login-brand span {
        width: 2rem;
        height: 2rem;
        display: grid;
        place-items: center;
        color: var(--ds-acid-lime);
        background: #111820;
        border: 1px solid var(--line);
        border-radius: 5px;
      }
      .login-copy {
        margin: 2rem 0 1.25rem;
      }
      .login-copy h1 {
        font-size: 1.65rem;
      }
      .login-copy p {
        margin: 0;
        color: var(--muted);
        font-size: 0.88rem;
      }
      form {
        display: grid;
        gap: 1rem;
      }
      .control {
        width: 100%;
        padding-right: 0.75rem;
        font-size: 1rem;
      }
      .btn {
        width: 100%;
        margin-top: 0.25rem;
      }
      .form-error {
        display: flex;
        align-items: flex-start;
        gap: 0.5rem;
        margin: 0;
        padding: 0.7rem 0.75rem;
        color: #ff9aa4;
        background: #251318;
        border: 1px solid #59303a;
        border-radius: 5px;
        font-size: 0.82rem;
      }
      .login-note {
        display: flex;
        gap: 0.5rem;
        margin: 1.25rem 0 0;
        padding-top: 1rem;
        color: var(--muted-2);
        border-top: 1px solid var(--line);
        font-size: 0.75rem;
      }
      .login-note i {
        margin-top: 0.15rem;
      }
    `,
  ],
})
export class LoginPage {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  username = '';
  password = '';
  loading = signal(false);
  error = signal('');

  submit() {
    if (!this.username.trim() || !this.password || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.auth
      .login(this.username.trim(), this.password)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.password = '';
          const requested = this.route.snapshot.queryParamMap.get('returnUrl');
          const target =
            requested?.startsWith('/') && !requested.startsWith('//')
              ? requested
              : '/integracoes/api-keys';
          void this.router.navigateByUrl(target);
        },
        error: (error) => {
          this.password = '';
          this.error.set(error.detail ?? 'Não foi possível iniciar a sessão.');
        },
      });
  }
}
