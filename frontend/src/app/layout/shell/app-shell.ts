import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ConfirmDialogModule],
  template: `
    <div class="app-shell" [class.nav-open]="navOpen()">
      <aside class="sidebar" aria-label="Navegação principal">
        <a class="brand" routerLink="/dashboard" (click)="navOpen.set(false)">
          <span class="brand-mark" aria-hidden="true"><i class="pi pi-shield"></i></span>
          <span>Document Shield</span>
        </a>
        <nav id="primary-navigation">
          @for (item of nav; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              ariaCurrentWhenActive="page"
              (click)="navOpen.set(false)"
            >
              <i [class]="item.icon"></i><span>{{ item.label }}</span>
            </a>
          }
          <div class="nav-section">Integrações</div>
          <a
            routerLink="/integracoes/api-keys"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            (click)="navOpen.set(false)"
          >
            <i class="pi pi-key"></i><span>API Keys</span>
          </a>
          <a
            routerLink="/integracoes/monitoramento"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            (click)="navOpen.set(false)"
          >
            <i class="pi pi-chart-bar"></i><span>Monitoramento</span>
          </a>
        </nav>
        <div class="side-foot">
          <span><i aria-hidden="true"></i> Motor conectado</span
          ><small>v1.0.0 · ambiente local</small>
        </div>
      </aside>
      <button
        class="scrim"
        type="button"
        aria-label="Fechar menu"
        (click)="navOpen.set(false)"
      ></button>
      <main class="content">
        <header class="topbar">
          <button
            class="mobile-menu"
            type="button"
            (click)="navOpen.set(!navOpen())"
            [attr.aria-expanded]="navOpen()"
            aria-controls="primary-navigation"
            [attr.aria-label]="navOpen() ? 'Fechar menu' : 'Abrir menu'"
          >
            <i class="pi pi-bars"></i>
          </button>
          <div class="topbar-brand">Central de análise</div>
          <div class="topbar-actions">
            @if (auth.session()?.authenticated) {
              <button
                class="btn btn-secondary btn-icon"
                type="button"
                (click)="logout()"
                aria-label="Encerrar sessão administrativa"
              >
                <i class="pi pi-sign-out"></i>
              </button>
            }
            <a class="btn btn-primary" routerLink="/documentos" fragment="upload"
              ><i class="pi pi-upload"></i><span>Enviar PDF</span></a
            >
          </div>
        </header>
        <router-outlet />
      </main>
    </div>
    <p-confirmDialog [style]="{ width: 'min(28rem, calc(100vw - 2rem))' }" />
  `,
  styles: [
    `
      .app-shell {
        min-height: 100vh;
      }
      .sidebar {
        position: fixed;
        inset: 0 auto 0 0;
        width: 15.25rem;
        background: var(--ds-midnight-black);
        color: var(--text);
        display: flex;
        flex-direction: column;
        border-right: 1px solid var(--line);
        z-index: 40;
      }
      .brand {
        height: 4.5rem;
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0 1.25rem;
        color: var(--text);
        font-weight: 750;
        font-size: 1rem;
        letter-spacing: -0.015em;
        border-bottom: 1px solid var(--line);
      }
      .brand-mark {
        width: 2rem;
        height: 2rem;
        display: grid;
        place-items: center;
        color: var(--ds-acid-lime);
        background: #111820;
        border: 1px solid var(--line);
        border-radius: 5px;
      }
      .brand-mark i {
        font-size: 1.05rem;
      }
      .sidebar nav {
        padding: 0.85rem 0.6rem;
        display: grid;
        gap: 0.2rem;
      }
      .sidebar nav a {
        min-height: 2.85rem;
        display: flex;
        align-items: center;
        gap: 0.8rem;
        padding: 0 0.85rem;
        border-radius: 5px;
        color: var(--muted);
        font-size: 0.86rem;
        font-weight: 600;
      }
      .sidebar nav a:hover {
        background: var(--ds-surface);
        color: var(--text);
      }
      .sidebar nav a.active {
        background: #102653;
        color: #fff;
        box-shadow: inset 2px 0 var(--ds-cobalt-blue);
      }
      .sidebar nav i {
        font-size: 1rem;
        width: 1.15rem;
        text-align: center;
      }
      .nav-section {
        margin: 0.85rem 0.85rem 0.25rem;
        color: var(--ds-text-disabled);
        font-size: 0.65rem;
        font-weight: 750;
        letter-spacing: 0.08em;
        text-transform: uppercase;
      }
      .side-foot {
        margin-top: auto;
        padding: 1rem 1.25rem;
        display: grid;
        gap: 0.3rem;
        border-top: 1px solid var(--line);
        font-size: 0.73rem;
        color: var(--muted);
      }
      .side-foot span {
        display: flex;
        align-items: center;
        gap: 0.45rem;
      }
      .side-foot span i {
        width: 0.45rem;
        height: 0.45rem;
        background: var(--ds-acid-lime);
        border-radius: 50%;
        box-shadow: 0 0 0 3px rgba(212, 255, 36, 0.08);
      }
      .side-foot small {
        color: var(--ds-text-disabled);
      }
      .content {
        min-height: 100vh;
        margin-left: 15.25rem;
      }
      .topbar {
        height: 4.5rem;
        background: #080b10;
        border-bottom: 1px solid var(--line);
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 1.75rem;
        position: sticky;
        top: 0;
        z-index: 25;
      }
      .topbar-brand {
        font-weight: 650;
        color: var(--muted);
        font-size: 0.8rem;
        letter-spacing: 0.02em;
      }
      .topbar-actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
      .mobile-menu {
        display: none;
        width: 2.75rem;
        height: 2.75rem;
        border: 1px solid var(--line);
        border-radius: 5px;
        background: var(--surface);
        color: var(--text);
        font-size: 1.1rem;
      }
      .scrim {
        display: none;
        padding: 0;
        border: 0;
      }
      @media (max-width: 760px) {
        .sidebar {
          transform: translateX(-100%);
          transition: transform 0.18s;
        }
        .content {
          margin-left: 0;
        }
        .nav-open .sidebar {
          transform: none;
        }
        .nav-open .scrim {
          display: block;
          position: fixed;
          inset: 0;
          background: rgba(3, 4, 7, 0.72);
          z-index: 35;
        }
        .mobile-menu {
          display: grid;
          place-items: center;
        }
        .topbar {
          height: 4rem;
          padding: 0 0.75rem;
        }
        .topbar-brand {
          margin-right: auto;
          margin-left: 0.65rem;
        }
        .topbar .btn span {
          display: none;
        }
      }
    `,
  ],
})
export class AppShell {
  auth = inject(AuthService);
  private router = inject(Router);
  navOpen = signal(false);
  nav = [
    { path: '/dashboard', label: 'Dashboard', icon: 'pi pi-home' },
    { path: '/documentos', label: 'Documentos', icon: 'pi pi-file' },
    { path: '/analises', label: 'Análises', icon: 'pi pi-search' },
    { path: '/regras', label: 'Regras', icon: 'pi pi-cog' },
  ];
  constructor() {
    this.auth.check().subscribe();
  }
  logout() {
    this.auth.logout().subscribe({ next: () => void this.router.navigate(['/login']) });
  }
}
