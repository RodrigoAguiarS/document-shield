import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EvidenceDto, OccurrenceDto } from '../../core/models/api.models';
import { OccurrenceService } from '../../core/services/occurrence.service';
import { EvidencePanel } from '../../shared/components/evidence-panel/evidence-panel';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import { ErrorState, LoadingState } from '../../shared/components/view-states/view-states';
import { forkJoin } from 'rxjs';
@Component({
  selector: 'app-occurrence-page',
  standalone: true,
  imports: [RouterLink, EvidencePanel, RiskBadge, ErrorState, LoadingState],
  template: `<div class="page narrow">
    <div class="page-head">
      <div>
        <a
          class="back"
          [routerLink]="occurrence() ? ['/analises', occurrence()!.analiseId] : ['/analises']"
          ><i class="pi pi-arrow-left"></i> Voltar à análise</a
        >
        <h1>Detalhes da ocorrência</h1>
        <p>Conteúdo extraído é exibido apenas como texto não confiável.</p>
      </div>
    </div>
    @if (loading()) {
      <app-loading-state />
    } @else if (error()) {
      <app-error-state [message]="error()" (retry)="load()" />
    } @else if (occurrence(); as o) {
      <article class="panel occurrence">
        <header>
          <div>
            <span>{{ o.tipo.replaceAll('_', ' ') }}</span>
            <h2>{{ o.titulo }}</h2>
            <p>Página {{ o.numeroPagina ?? 'não determinada' }} · Pontuação {{ o.pontuacao }}</p>
          </div>
          <app-risk-badge [level]="o.nivelRisco" />
        </header>
        <section>
          <h3>Descrição</h3>
          <p>{{ o.descricao }}</p>
        </section>
        <div class="content-grid">
          <section>
            <h3>Conteúdo suspeito</h3>
            <pre>{{ o.conteudoSuspeito || 'Não registrado' }}</pre>
          </section>
          <section>
            <h3>Conteúdo normalizado</h3>
            <pre>{{ o.conteudoNormalizado || 'Não registrado' }}</pre>
          </section>
        </div>
        <section>
          <h3>Propriedades técnicas</h3>
          <dl>
            <div>
              <dt>Coordenadas</dt>
              <dd>{{ coords(o) }}</dd>
            </div>
            <div>
              <dt>Fonte</dt>
              <dd>{{ o.fonte ?? 'Não determinada' }}</dd>
            </div>
            <div>
              <dt>Tamanho da fonte</dt>
              <dd>{{ o.tamanhoFonte !== null ? o.tamanhoFonte + ' pt' : 'Não determinado' }}</dd>
            </div>
            <div>
              <dt>Cor</dt>
              <dd>{{ o.corTexto ?? 'Não determinada' }}</dd>
            </div>
            <div>
              <dt>Opacidade</dt>
              <dd>
                {{
                  o.opacidade !== null ? (o.opacidade * 100).toFixed(0) + '%' : 'Não determinada'
                }}
              </dd>
            </div>
            <div>
              <dt>Regra associada</dt>
              <dd>{{ o.regraId ?? 'Não associada' }}</dd>
            </div>
          </dl>
        </section>
        <app-evidence-panel [items]="evidence()" />
      </article>
    }
  </div>`,
  styles: [
    `
      .narrow {
        max-width: 1100px;
      }
      .back {
        display: inline-flex;
        gap: 0.45rem;
        color: var(--muted);
        font-size: 0.8rem;
        margin-bottom: 0.65rem;
      }
      .occurrence {
        padding: 1.2rem;
        display: grid;
        gap: 1.25rem;
      }
      .occurrence header {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        border-bottom: 1px solid var(--line);
        padding-bottom: 1rem;
      }
      .occurrence header span {
        font-size: 0.7rem;
        color: var(--muted);
        font-weight: 800;
      }
      .occurrence header h2 {
        font-size: 1.25rem;
        margin: 0.3rem 0;
      }
      .occurrence header p {
        margin: 0;
        color: var(--muted);
        font-size: 0.78rem;
      }
      .occurrence section h3 {
        margin-bottom: 0.45rem;
      }
      .occurrence section > p {
        font-size: 0.86rem;
      }
      .content-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
      }
      pre {
        min-height: 5rem;
        margin: 0;
        background: var(--surface-subtle);
        padding: 0.85rem;
        border-radius: 6px;
        font-family: var(--font-mono);
        font-size: 0.78rem;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
      }
      dl {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 1rem;
        margin: 0;
      }
      dl div {
        display: grid;
        gap: 0.25rem;
      }
      dt {
        font-size: 0.7rem;
        color: var(--muted);
        font-weight: 700;
      }
      dd {
        margin: 0;
        font-size: 0.78rem;
        overflow-wrap: anywhere;
      }
      @media (max-width: 700px) {
        .content-grid,
        dl {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class OccurrencePage {
  private route = inject(ActivatedRoute);
  private service = inject(OccurrenceService);
  id = this.route.snapshot.paramMap.get('id')!;
  occurrence = signal<OccurrenceDto | null>(null);
  evidence = signal<EvidenceDto[]>([]);
  loading = signal(true);
  error = signal('');
  constructor() {
    this.load();
  }
  load() {
    this.loading.set(true);
    forkJoin({
      occurrence: this.service.get(this.id),
      evidence: this.service.evidence(this.id),
    }).subscribe({
      next: (r) => {
        this.occurrence.set(r.occurrence);
        this.evidence.set(r.evidence);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.detail ?? 'Não foi possível carregar a ocorrência.');
        this.loading.set(false);
      },
    });
  }
  coords(o: OccurrenceDto) {
    return o.posicaoX === null || o.posicaoY === null
      ? 'Não determinadas pelo extrator'
      : `X ${o.posicaoX}, Y ${o.posicaoY}, largura ${o.largura ?? '—'}, altura ${o.altura ?? '—'}`;
  }
}
