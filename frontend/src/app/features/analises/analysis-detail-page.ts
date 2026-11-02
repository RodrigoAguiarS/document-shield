import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { APP_CONFIG } from '../../core/config/app-config';
import {
  AnalysisDto,
  DocumentDto,
  EvidenceDto,
  OccurrenceDto,
  PagedResponse,
  RuleExecutionDto,
} from '../../core/models/api.models';
import { AnalysisService } from '../../core/services/analysis.service';
import { DocumentService } from '../../core/services/document.service';
import { OccurrenceService } from '../../core/services/occurrence.service';
import { EvidencePanel } from '../../shared/components/evidence-panel/evidence-panel';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';
@Component({
  selector: 'app-analysis-detail-page',
  standalone: true,
  imports: [
    RouterLink,
    EvidencePanel,
    RiskBadge,
    StatusBadge,
    EmptyState,
    ErrorState,
    LoadingState,
  ],
  template: `<div class="page">
    <div class="page-head">
      <div>
        <a class="back" routerLink="/analises"><i class="pi pi-arrow-left"></i> Análises</a>
        <h1>Análise do documento</h1>
        <p>
          @if (document(); as d) {
            <a [routerLink]="['/documentos', d.id]">{{ d.nomeOriginal }}</a>
          } @else {
            Avaliação {{ id.slice(0, 8).toUpperCase() }}
          }
        </p>
      </div>
      @if (document(); as d) {
        <button class="btn btn-secondary" (click)="download(d)">
          <i class="pi pi-download"></i>Baixar documento
        </button>
      }
    </div>
    @if (loading()) {
      <app-loading-state message="Carregando análise…" />
    } @else if (error() && !analysis()) {
      <app-error-state [message]="error()" (retry)="refresh()" />
    } @else if (analysis(); as a) {
      <section class="analysis-band panel">
        <div><span>STATUS</span><app-status-badge [status]="a.status" /></div>
        <div><span>NÍVEL DE RISCO</span><app-risk-badge [level]="a.nivelRisco" /></div>
        <div>
          <span>PONTUAÇÃO</span
          ><strong class="score">{{ a.pontuacaoRisco ?? '—' }}<small>/100</small></strong>
        </div>
        <div>
          <span>OCORRÊNCIAS</span><strong>{{ a.totalOcorrencias }}</strong
          ><small>{{ a.totalOcorrenciasAltas }} alta · {{ a.totalOcorrenciasMedias }} média</small>
        </div>
        <div>
          <span>VERSÃO DO MOTOR</span><strong>{{ a.versaoMotor }}</strong>
        </div>
        <div>
          <span>DURAÇÃO</span><strong>{{ duration(a.duracaoMilissegundos) }}</strong>
        </div>
        <p class="heuristic">
          <i class="pi pi-info-circle"></i>A pontuação é uma avaliação heurística baseada em
          evidências técnicas; não constitui prova definitiva de ataque.
        </p>
      </section>
      @if (a.status === 'PENDENTE' || a.status === 'EXECUTANDO') {
        <section class="processing panel">
          <i class="pi pi-spin pi-spinner"></i>
          <div>
            <strong>{{
              a.status === 'PENDENTE' ? 'Aguardando processamento' : 'Análise em execução'
            }}</strong
            ><span
              >Atualização automática a cada {{ cfg.pollingIntervalMs / 1000 }} segundos. Não há
              percentual disponível no contrato.</span
            >
          </div>
          @if (pollStopped()) {
            <button class="btn btn-secondary" (click)="resumePolling()">
              <i class="pi pi-refresh"></i>Tentar novamente
            </button>
          }
        </section>
      } @else if (a.status === 'FALHOU') {
        <section class="processing failed">
          <i class="pi pi-exclamation-circle"></i>
          <div>
            <strong>A análise falhou</strong
            ><span>{{ a.mensagemErro || 'O backend não forneceu detalhes adicionais.' }}</span>
          </div>
        </section>
      } @else {
        <div class="detail-grid">
          <section class="panel occurrences">
            <div class="panel-head">
              <h2>Ocorrências encontradas ({{ a.totalOcorrencias }})</h2>
            </div>
            @if (occurrences(); as o) {
              @if (o.content.length) {
                <div class="data-table-wrap">
                  <table class="data-table">
                    <thead>
                      <tr>
                        <th>Tipo</th>
                        <th>Título</th>
                        <th>Página</th>
                        <th>Severidade</th>
                        <th>Pontuação</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (item of o.content; track item.id) {
                        <tr
                          [class.selected]="selected()?.id === item.id"
                          (click)="select(item)"
                          tabindex="0"
                          (keydown.enter)="select(item)"
                        >
                          <td>{{ label(item.tipo) }}</td>
                          <td class="cell-title">{{ item.titulo }}</td>
                          <td>{{ item.numeroPagina ?? 'Não determinada' }}</td>
                          <td><app-risk-badge [level]="item.nivelRisco" /></td>
                          <td>{{ item.pontuacao }}</td>
                          <td>
                            <a
                              [routerLink]="['/ocorrencias', item.id]"
                              aria-label="Abrir ocorrência"
                              ><i class="pi pi-chevron-right"></i
                            ></a>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
                <div class="pagination">
                  <span>{{ o.totalElements }} ocorrência(s)</span>
                  <div class="pagination-actions">
                    <button
                      [disabled]="o.first"
                      (click)="occPage.set(occPage() - 1); loadOccurrences()"
                    >
                      <i class="pi pi-chevron-left"></i></button
                    ><button class="active">{{ o.page + 1 }}</button
                    ><button
                      [disabled]="o.last"
                      (click)="occPage.set(occPage() + 1); loadOccurrences()"
                    >
                      <i class="pi pi-chevron-right"></i>
                    </button>
                  </div>
                </div>
              } @else {
                <app-empty-state
                  title="Nenhuma ocorrência"
                  message="As regras ativas não identificaram evidências neste documento."
                  icon="pi pi-shield"
                />
              }
            } @else {
              <app-loading-state />
            }
          </section>
          <aside class="inspector panel">
            @if (selected(); as s) {
              <div class="panel-head">
                <h2>Detalhes da ocorrência</h2>
                <a [routerLink]="['/ocorrencias', s.id]" aria-label="Abrir página completa"
                  ><i class="pi pi-external-link"></i
                ></a>
              </div>
              <div class="inspector-body">
                <div class="finding-title">
                  <div>
                    <strong>{{ s.titulo }}</strong
                    ><span
                      >{{ label(s.tipo) }} · Página {{ s.numeroPagina ?? 'não determinada' }}</span
                    >
                  </div>
                  <app-risk-badge [level]="s.nivelRisco" />
                </div>
                <section>
                  <h3>Descrição</h3>
                  <p>{{ s.descricao }}</p>
                </section>
                @if (s.conteudoSuspeito) {
                  <section>
                    <h3>Conteúdo suspeito</h3>
                    <pre>{{ s.conteudoSuspeito }}</pre>
                  </section>
                }
                @if (s.conteudoNormalizado) {
                  <section>
                    <h3>Conteúdo normalizado</h3>
                    <pre>{{ s.conteudoNormalizado }}</pre>
                  </section>
                }
                <section>
                  <h3>Propriedades extraídas</h3>
                  <dl>
                    <div>
                      <dt>Coordenadas</dt>
                      <dd>{{ coords(s) }}</dd>
                    </div>
                    <div>
                      <dt>Fonte</dt>
                      <dd>{{ s.fonte ?? 'Não determinada' }}</dd>
                    </div>
                    <div>
                      <dt>Tamanho</dt>
                      <dd>
                        {{ s.tamanhoFonte !== null ? s.tamanhoFonte + ' pt' : 'Não determinado' }}
                      </dd>
                    </div>
                    <div>
                      <dt>Cor</dt>
                      <dd>{{ s.corTexto ?? 'Não determinada' }}</dd>
                    </div>
                    <div>
                      <dt>Opacidade</dt>
                      <dd>
                        {{
                          s.opacidade !== null
                            ? (s.opacidade * 100).toFixed(0) + '%'
                            : 'Não determinada'
                        }}
                      </dd>
                    </div>
                  </dl>
                </section>
                <app-evidence-panel [items]="evidence()" />
              </div>
            } @else {
              <app-empty-state
                title="Selecione uma ocorrência"
                message="Os detalhes e as evidências aparecerão aqui."
              />
            }
          </aside>
        </div>
        <section class="panel audit">
          <div class="panel-head">
            <h2>Auditoria das regras</h2>
            <span class="muted">{{ executions().length }} registro(s)</span>
          </div>
          <div class="execution-grid">
            @for (x of executions(); track x.id) {
              <div>
                <i
                  class="pi"
                  [class.pi-check-circle]="x.sucesso"
                  [class.pi-minus-circle]="!x.executada"
                  [class.pi-times-circle]="x.executada && !x.sucesso"
                ></i
                ><span>{{ x.estado.replaceAll('_', ' ') }}</span
                ><strong>{{ x.quantidadeOcorrencias }} ocorrência(s)</strong>
              </div>
            }
          </div>
        </section>
      }
    }
  </div>`,
  styles: [
    `
      .back {
        display: inline-flex;
        gap: 0.45rem;
        align-items: center;
        color: var(--muted);
        font-size: 0.8rem;
        margin-bottom: 0.65rem;
      }
      .analysis-band {
        display: grid;
        grid-template-columns: repeat(6, 1fr);
        margin-bottom: 1rem;
      }
      .analysis-band > div {
        padding: 1rem;
        border-right: 1px solid var(--line);
        display: grid;
        gap: 0.4rem;
      }
      .analysis-band > div:nth-child(6) {
        border: 0;
      }
      .analysis-band span {
        font-size: 0.68rem;
        font-weight: 800;
        color: var(--muted);
      }
      .analysis-band strong {
        font-size: 0.95rem;
      }
      .analysis-band small {
        font-size: 0.7rem;
        color: var(--muted);
      }
      .score {
        font-size: 1.55rem !important;
        color: var(--risk-high);
      }
      .score small {
        margin-left: 0.25rem;
      }
      .heuristic {
        grid-column: 1/-1;
        margin: 0 0.65rem 0.65rem;
        background: #edf7fb;
        color: var(--ink-700);
        padding: 0.65rem 0.75rem;
        border-radius: 5px;
        font-size: 0.76rem;
      }
      .heuristic i {
        margin-right: 0.45rem;
        color: #168ec2;
      }
      .processing {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1.2rem;
        margin-bottom: 1rem;
      }
      .processing > i {
        font-size: 1.4rem;
        color: #1474b8;
      }
      .processing div {
        display: grid;
        gap: 0.25rem;
        flex: 1;
      }
      .processing span {
        font-size: 0.78rem;
        color: var(--muted);
      }
      .processing.failed {
        border: 1px solid #efc9ce;
        background: #fff7f8;
      }
      .processing.failed > i {
        color: var(--danger);
      }
      .detail-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.55fr) minmax(19rem, 0.8fr);
        gap: 1rem;
      }
      .occurrences tr {
        cursor: pointer;
      }
      .inspector {
        min-width: 0;
      }
      .inspector-body {
        padding: 1rem;
        display: grid;
        gap: 1rem;
      }
      .finding-title {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
      }
      .finding-title > div {
        display: grid;
        gap: 0.25rem;
      }
      .finding-title span {
        font-size: 0.75rem;
        color: var(--muted);
      }
      .inspector section h3 {
        margin: 0 0 0.35rem;
      }
      .inspector section p {
        font-size: 0.78rem;
        margin: 0;
      }
      .inspector pre {
        margin: 0;
        padding: 0.75rem;
        border-radius: 5px;
        background: var(--surface-subtle);
        font: inherit;
        font-family: var(--font-mono);
        font-size: 0.74rem;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
        max-height: 10rem;
        overflow: auto;
      }
      .inspector dl {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.7rem;
        margin: 0;
      }
      .inspector dl div {
        display: grid;
        gap: 0.2rem;
      }
      .inspector dt {
        font-size: 0.68rem;
        color: var(--muted);
        font-weight: 700;
      }
      .inspector dd {
        font-size: 0.76rem;
        margin: 0;
      }
      .audit {
        margin-top: 1rem;
      }
      .execution-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        padding: 0.5rem 1rem;
      }
      .execution-grid div {
        padding: 0.7rem;
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.25rem 0.5rem;
        border-bottom: 1px solid var(--line);
        font-size: 0.7rem;
      }
      .execution-grid strong {
        grid-column: 2;
      }
      .execution-grid .pi-check-circle {
        color: var(--success);
      }
      .execution-grid .pi-times-circle {
        color: var(--danger);
      }
      @media (max-width: 1180px) {
        .analysis-band {
          grid-template-columns: repeat(3, 1fr);
        }
        .analysis-band > div {
          border-bottom: 1px solid var(--line);
        }
        .detail-grid {
          grid-template-columns: 1fr;
        }
        .inspector {
          max-height: none;
        }
        .execution-grid {
          grid-template-columns: 1fr 1fr;
        }
      }
      @media (max-width: 700px) {
        .analysis-band {
          grid-template-columns: 1fr 1fr;
        }
        .execution-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class AnalysisDetailPage {
  private route = inject(ActivatedRoute);
  private analyses = inject(AnalysisService);
  private documents = inject(DocumentService);
  private occurrencesService = inject(OccurrenceService);
  private destroy = inject(DestroyRef);
  cfg = inject(APP_CONFIG);
  id = this.route.snapshot.paramMap.get('id')!;
  analysis = signal<AnalysisDto | null>(null);
  document = signal<DocumentDto | null>(null);
  occurrences = signal<PagedResponse<OccurrenceDto> | null>(null);
  selected = signal<OccurrenceDto | null>(null);
  evidence = signal<EvidenceDto[]>([]);
  executions = signal<RuleExecutionDto[]>([]);
  occPage = signal(0);
  loading = signal(true);
  error = signal('');
  pollStopped = signal(false);
  private timer?: ReturnType<typeof setTimeout>;
  private pollErrors = 0;
  constructor() {
    this.destroy.onDestroy(() => clearTimeout(this.timer));
    this.refresh();
  }
  refresh() {
    clearTimeout(this.timer);
    this.loading.set(!this.analysis());
    this.error.set('');
    this.analyses.get(this.id).subscribe({
      next: (a) => {
        this.analysis.set(a);
        this.loading.set(false);
        this.error.set('');
        this.pollErrors = 0;
        if (!this.document())
          this.documents.get(a.documentoId).subscribe({ next: (d) => this.document.set(d) });
        if (a.status === 'PENDENTE' || a.status === 'EXECUTANDO') this.schedule();
        else if (a.status === 'CONCLUIDA' && !this.occurrences()) this.loadResults();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e.detail ?? 'Não foi possível consultar a análise.');
        this.pollErrors++;
        if (this.analysis() && this.pollErrors < this.cfg.maxPollingErrors) this.schedule();
        else this.pollStopped.set(true);
      },
    });
  }
  private schedule() {
    this.pollStopped.set(false);
    this.timer = setTimeout(() => this.refresh(), this.cfg.pollingIntervalMs);
  }
  resumePolling() {
    this.pollErrors = 0;
    this.pollStopped.set(false);
    this.refresh();
  }
  private loadResults() {
    forkJoin({
      occurrences: this.analyses.occurrences(this.id, {
        page: 0,
        size: 20,
        sort: 'dataIdentificacao,asc',
      }),
      executions: this.analyses.executions(this.id),
    }).subscribe((r) => {
      this.occurrences.set(r.occurrences);
      this.executions.set(r.executions);
      if (r.occurrences.content[0]) this.select(r.occurrences.content[0]);
    });
  }
  loadOccurrences() {
    this.analyses
      .occurrences(this.id, { page: this.occPage(), size: 20, sort: 'dataIdentificacao,asc' })
      .subscribe((o) => {
        this.occurrences.set(o);
        if (o.content[0]) this.select(o.content[0]);
      });
  }
  select(o: OccurrenceDto) {
    this.selected.set(o);
    this.evidence.set([]);
    this.occurrencesService.evidence(o.id).subscribe((e) => this.evidence.set(e));
  }
  label(v: string) {
    return v.replaceAll('_', ' ');
  }
  coords(o: OccurrenceDto) {
    if (o.posicaoX === null || o.posicaoY === null) return 'Não determinadas pelo extrator';
    return `X ${o.posicaoX}, Y ${o.posicaoY}${o.largura !== null ? `, ${o.largura} × ${o.altura}` : ''}`;
  }
  duration(ms: number | null) {
    if (ms === null) return 'Não disponível';
    return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;
  }
  download(d: DocumentDto) {
    this.documents.download(d.id).subscribe((r) => {
      const u = URL.createObjectURL(r.body!);
      const a = document.createElement('a');
      a.href = u;
      a.download = d.nomeOriginal;
      a.click();
      URL.revokeObjectURL(u);
    });
  }
}
