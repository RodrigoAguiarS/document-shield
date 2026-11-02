import { Component, inject, signal } from '@angular/core';
import { DetectionRuleDto } from '../../core/models/api.models';
import { RuleService } from '../../core/services/rule.service';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';
@Component({
  selector: 'app-rules-page',
  standalone: true,
  imports: [RiskBadge, EmptyState, ErrorState, LoadingState],
  template: `<div class="page">
    <div class="page-head">
      <div>
        <h1>Regras de detecção</h1>
        <p>Catálogo somente leitura das verificações executadas pelo motor.</p>
      </div>
    </div>
    <section class="panel">
      @if (loading()) {
        <app-loading-state />
      } @else if (error()) {
        <app-error-state [message]="error()" (retry)="load()" />
      } @else if (rules().length) {
        <div class="rules">
          @for (r of rules(); track r.id) {
            <article>
              <div class="code">{{ r.codigo }}</div>
              <div class="rule-main">
                <div>
                  <h2>{{ r.nome }}</h2>
                  <p>{{ r.descricao }}</p>
                </div>
                <div class="rule-meta">
                  <app-risk-badge [level]="r.nivelRiscoPadrao" /><span [class.active]="r.ativa"
                    ><i class="status-dot"></i>{{ r.ativa ? 'ATIVA' : 'INATIVA' }}</span
                  >
                </div>
                <dl>
                  <div>
                    <dt>Tipo</dt>
                    <dd>{{ r.tipoOcorrencia.replaceAll('_', ' ') }}</dd>
                  </div>
                  <div>
                    <dt>Pontuação base</dt>
                    <dd>{{ r.pontuacaoBase }}</dd>
                  </div>
                  <div>
                    <dt>Versão</dt>
                    <dd>{{ r.versao }}</dd>
                  </div>
                </dl>
              </div>
            </article>
          }
        </div>
      } @else {
        <app-empty-state
          title="Nenhuma regra"
          message="O backend não retornou regras de detecção."
        />
      }
    </section>
  </div>`,
  styles: [
    `
      .rules {
        padding: 0 1rem;
      }
      .rules article {
        display: grid;
        grid-template-columns: 5rem 1fr;
        gap: 1rem;
        padding: 1.1rem 0;
        border-bottom: 1px solid var(--line);
      }
      .rules article:last-child {
        border: 0;
      }
      .code {
        font-family: var(--font-mono);
        font-weight: 800;
        color: var(--teal-700);
        padding-top: 0.15rem;
      }
      .rule-main {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.8rem 1rem;
      }
      .rule-main h2 {
        margin: 0 0 0.3rem;
      }
      .rule-main p {
        margin: 0;
        color: var(--muted);
        font-size: 0.8rem;
      }
      .rule-meta {
        display: flex;
        align-items: flex-start;
        gap: 0.5rem;
      }
      .rule-meta > span {
        font-size: 0.68rem;
        font-weight: 800;
        color: var(--muted);
        display: flex;
        align-items: center;
        gap: 0.35rem;
        padding-top: 0.35rem;
      }
      .rule-meta > span.active {
        color: var(--success);
      }
      dl {
        grid-column: 1/-1;
        display: flex;
        gap: 2rem;
        margin: 0.2rem 0 0;
      }
      dl div {
        display: grid;
        gap: 0.2rem;
      }
      dt {
        font-size: 0.66rem;
        font-weight: 700;
        color: var(--muted);
      }
      dd {
        margin: 0;
        font-size: 0.76rem;
      }
      @media (max-width: 700px) {
        .rules article {
          grid-template-columns: 1fr;
        }
        .rule-main {
          grid-template-columns: 1fr;
        }
        .rule-meta {
          grid-row: 1;
        }
        dl {
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class RulesPage {
  private service = inject(RuleService);
  rules = signal<DetectionRuleDto[]>([]);
  loading = signal(true);
  error = signal('');
  constructor() {
    this.load();
  }
  load() {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (r) => {
        this.rules.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.detail ?? 'Não foi possível carregar as regras.');
        this.loading.set(false);
      },
    });
  }
}
