import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, exhaustMap, finalize, forkJoin, merge, timer } from 'rxjs';
import { APP_CONFIG } from '../../core/config/app-config';
import {
  MonitoringCapacity,
  MonitoringErrors,
  MonitoringIntegrationList,
  MonitoringPeriod,
  MonitoringSeries,
  MonitoringSummary,
} from '../../core/models/api.models';
import { MonitoringQuery, MonitoringService } from '../../core/services/monitoring.service';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';

@Component({
  selector: 'app-monitoring-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, EmptyState, ErrorState, LoadingState],
  template: `
    <div class="page monitoring-page">
      <div class="page-head">
        <div>
          <h1>Monitoramento da API</h1>
          <p>Uso agregado, latência e capacidade operacional da API pública. Horários em UTC.</p>
        </div>
        <div class="freshness">
          <span [class.online]="capacity()?.aplicacao === 'UP'"
            ><i></i>{{ capacity()?.aplicacao === 'UP' ? 'Operacional' : 'Verificando' }}</span
          ><small
            >Atualizado
            {{ lastUpdated() ? (lastUpdated() | date: 'HH:mm:ss' : 'UTC') : '—' }} UTC</small
          >
        </div>
      </div>

      <section class="filter-bar" aria-label="Filtros de monitoramento">
        <label
          >Período<select class="control" [(ngModel)]="period">
            <option value="24H">Últimas 24 horas</option>
            <option value="7D">Últimos 7 dias</option>
            <option value="30D">Últimos 30 dias</option>
            <option value="CUSTOM">Personalizado</option>
          </select></label
        >
        @if (period === 'CUSTOM') {
          <label
            >Início<input class="control" type="datetime-local" [(ngModel)]="customStart"
          /></label>
          <label>Fim<input class="control" type="datetime-local" [(ngModel)]="customEnd" /></label>
        }
        <button
          class="btn btn-primary"
          type="button"
          (click)="apply()"
          [disabled]="period === 'CUSTOM' && (!customStart || !customEnd)"
        >
          Aplicar
        </button>
        <button
          class="btn btn-secondary btn-icon"
          type="button"
          (click)="refresh()"
          aria-label="Atualizar métricas"
        >
          <i class="pi pi-refresh" [class.pi-spin]="loading()"></i>
        </button>
        <span class="poll-note">Atualização automática a cada 30 s</span>
      </section>

      @if (loading() && !summary()) {
        <app-loading-state />
      } @else if (error() && !summary()) {
        <app-error-state [message]="error()" (retry)="refresh()" />
      } @else if (summary(); as value) {
        @if (error()) {
          <div class="stale-warning" role="status">
            <i class="pi pi-exclamation-triangle"></i>{{ error() }} Exibindo a última leitura
            válida.
          </div>
        }
        <section class="metric-strip metrics-six" aria-label="Indicadores principais">
          <article class="metric">
            <span class="metric-label"><i class="pi pi-send"></i>Requisições</span
            ><strong class="metric-value">{{ value.totalRequisicoes | number }}</strong
            ><span class="metric-hint">no período selecionado</span>
          </article>
          <article class="metric">
            <span class="metric-label"><i class="pi pi-check-circle"></i>Sucesso</span
            ><strong class="metric-value">{{ value.taxaSucesso | number: '1.1-1' }}%</strong
            ><span class="metric-hint">exclui autenticação e limite</span>
          </article>
          <article class="metric">
            <span class="metric-label"><i class="pi pi-times-circle"></i>Falhas</span
            ><strong class="metric-value">{{ value.totalFalhas | number }}</strong
            ><span class="metric-hint">respostas não 2xx</span>
          </article>
          <article class="metric">
            <span class="metric-label"><i class="pi pi-clock"></i>Latência média</span
            ><strong class="metric-value">{{ formatMs(value.duracaoMediaMs) }}</strong
            ><span class="metric-hint">tempo total da requisição</span>
          </article>
          <article class="metric">
            <span class="metric-label"><i class="pi pi-chart-line"></i>P95</span
            ><strong class="metric-value">{{ formatMs(value.p95Ms) }}</strong
            ><span class="metric-hint">estimativa por histograma</span>
          </article>
          <article class="metric">
            <span class="metric-label"><i class="pi pi-link"></i>Integrações</span
            ><strong class="metric-value">{{ value.integracoesComUso }}</strong
            ><span class="metric-hint">com uso no período</span>
          </article>
        </section>

        <div class="monitor-grid">
          <section class="panel volume-panel">
            <div class="panel-head">
              <div>
                <h2>Volume temporal</h2>
                <small>{{
                  series()?.granularidade === 'HOUR' ? 'Agregado por hora' : 'Agregado por dia'
                }}</small>
              </div>
              <span class="legend"><i></i>Requisições</span>
            </div>
            @if (series()?.pontos?.length) {
              <div
                class="bar-chart"
                role="img"
                aria-label="Volume de requisições ao longo do período"
              >
                @for (point of series()!.pontos; track point.inicio; let index = $index) {
                  <div class="bar-slot">
                    <div
                      class="bar"
                      [style.height.%]="barHeight(point.totalRequisicoes)"
                      [attr.title]="point.totalRequisicoes + ' requisições em ' + point.inicio"
                    ></div>
                    @if (showChartLabel(index)) {
                      <span>{{
                        point.inicio
                          | date: (series()!.granularidade === 'HOUR' ? 'HH:mm' : 'dd/MM') : 'UTC'
                      }}</span>
                    }
                  </div>
                }
              </div>
            } @else {
              <app-empty-state
                title="Sem tráfego"
                message="Nenhuma requisição foi agregada neste período."
              />
            }
          </section>

          <section class="panel capacity-panel">
            <div class="panel-head">
              <h2>Capacidade agora</h2>
              <span class="health" [class.up]="capacity()?.aplicacao === 'UP'"
                ><i></i>{{ capacity()?.aplicacao || '—' }}</span
              >
            </div>
            @if (capacity(); as cap) {
              <dl class="capacity-list">
                <div>
                  <dt>Banco de dados</dt>
                  <dd>{{ cap.bancoDeDados }}</dd>
                </div>
                <div>
                  <dt>Execuções ativas</dt>
                  <dd>{{ cap.execucoesAtivas }} / {{ cap.concorrenciaConfigurada }}</dd>
                </div>
                <div>
                  <dt>Workers disponíveis</dt>
                  <dd>{{ cap.workersDisponiveis }}</dd>
                </div>
                <div>
                  <dt>Fila</dt>
                  <dd>{{ cap.filaEmUso }} / {{ cap.capacidadeFila }}</dd>
                </div>
              </dl>
              <div class="capacity-track"><span [style.width.%]="queuePercent(cap)"></span></div>
              <small>{{ cap.capacidadeFilaDisponivel }} posição(ões) disponíveis na fila</small>
            }
          </section>

          <section class="panel integrations-panel">
            <div class="panel-head">
              <h2>Uso por integração</h2>
              <span>{{ integrations()?.integracoes?.length || 0 }} com tráfego</span>
            </div>
            @if (integrations()?.integracoes?.length) {
              <div class="data-table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Integração</th>
                      <th>Status</th>
                      <th>Requisições</th>
                      <th>Sucesso</th>
                      <th>Falhas</th>
                      <th>Média</th>
                      <th>P95</th>
                      <th>Limite</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of integrations()!.integracoes; track item.id) {
                      <tr>
                        <td>
                          <span class="cell-title">{{ item.nome }}</span>
                        </td>
                        <td>
                          <span class="integration-status" [class.active]="item.status === 'ATIVA'"
                            ><i></i>{{ item.status }}</span
                          >
                        </td>
                        <td>{{ item.totalRequisicoes | number }}</td>
                        <td>{{ item.taxaSucesso | number: '1.1-1' }}%</td>
                        <td>{{ item.totalFalhas | number }}</td>
                        <td>{{ formatMs(item.duracaoMediaMs) }}</td>
                        <td>{{ formatMs(item.p95Ms) }}</td>
                        <td>
                          {{ item.limitePorMinuto ? item.limitePorMinuto + '/min' : 'Padrão' }}
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            } @else {
              <app-empty-state
                title="Sem uso por integração"
                message="As integrações aparecerão após a primeira chamada autenticada."
              />
            }
          </section>

          <section class="panel errors-panel">
            <div class="panel-head">
              <h2>Distribuição de falhas</h2>
              <span>{{ errors()?.totalFalhas || 0 }} no total</span>
            </div>
            @if (errors()?.erros?.length) {
              <div class="error-list">
                @for (item of errors()!.erros; track item.tipo) {
                  <div class="error-row">
                    <div>
                      <span>{{ errorLabel(item.tipo) }}</span
                      ><strong>{{ item.quantidade | number }}</strong>
                    </div>
                    <div class="error-track"><span [style.width.%]="item.percentual"></span></div>
                    <small>{{ item.percentual | number: '1.1-1' }}% das falhas</small>
                  </div>
                }
              </div>
            } @else {
              <div class="all-clear">
                <i class="pi pi-check-circle"></i>
                <div>
                  <strong>Nenhuma falha registrada</strong
                  ><span>O período selecionado não contém respostas com erro.</span>
                </div>
              </div>
            }
          </section>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .freshness {
        display: grid;
        justify-items: end;
        gap: 0.25rem;
        color: var(--muted);
        font-size: 0.75rem;
      }
      .freshness > span,
      .health,
      .integration-status {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        font-weight: 700;
      }
      .freshness i,
      .health i,
      .integration-status i {
        width: 0.5rem;
        height: 0.5rem;
        border-radius: 50%;
        background: var(--warning);
      }
      .freshness .online i,
      .health.up i,
      .integration-status.active i {
        background: var(--success);
      }
      .filter-bar {
        display: flex;
        align-items: flex-end;
        gap: 0.65rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
        padding: 0.8rem 1rem;
        background: var(--surface);
        border: 1px solid var(--line);
        border-radius: var(--radius);
      }
      .filter-bar label {
        display: grid;
        gap: 0.3rem;
        color: var(--muted);
        font-size: 0.7rem;
        font-weight: 700;
      }
      .filter-bar .control {
        height: 2.4rem;
      }
      .poll-note {
        margin-left: auto;
        align-self: center;
        color: var(--muted-2);
        font-size: 0.72rem;
      }
      .stale-warning {
        display: flex;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
        padding: 0.65rem 0.8rem;
        color: var(--warning);
        background: #241e11;
        border: 1px solid #55451f;
        border-radius: 5px;
        font-size: 0.78rem;
      }
      .metrics-six {
        grid-template-columns: repeat(6, minmax(0, 1fr));
      }
      .metrics-six .metric:nth-child(4n) {
        border-right: 1px solid var(--line);
      }
      .metrics-six .metric:last-child {
        border-right: 0;
      }
      .metric-label {
        display: flex;
        align-items: center;
        gap: 0.45rem;
      }
      .metric-label i {
        color: var(--ds-acid-lime);
        font-size: 0.85rem;
      }
      .metric-value {
        display: block;
      }
      .monitor-grid {
        display: grid;
        grid-template-columns: minmax(0, 2fr) minmax(18rem, 1fr);
        gap: 1rem;
      }
      .panel-head small,
      .panel-head > span {
        color: var(--muted);
        font-size: 0.72rem;
      }
      .legend {
        display: flex;
        align-items: center;
        gap: 0.4rem;
      }
      .legend i {
        width: 0.55rem;
        height: 0.55rem;
        background: var(--ds-cobalt-hover);
        border-radius: 2px;
      }
      .bar-chart {
        height: 17rem;
        display: flex;
        align-items: stretch;
        gap: clamp(2px, 0.35vw, 6px);
        padding: 1.2rem 1rem 0.7rem;
        background: repeating-linear-gradient(
          to bottom,
          transparent 0,
          transparent calc(25% - 1px),
          var(--line) 25%
        );
      }
      .bar-slot {
        flex: 1;
        min-width: 2px;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;
        align-items: center;
        gap: 0.35rem;
      }
      .bar {
        width: 100%;
        min-height: 2px;
        background: var(--ds-cobalt-blue);
        border-radius: 2px 2px 0 0;
      }
      .bar-slot span {
        height: 0.9rem;
        color: var(--muted-2);
        font-size: 0.58rem;
        white-space: nowrap;
      }
      .capacity-list {
        margin: 0;
        padding: 1rem;
        display: grid;
        gap: 0.8rem;
      }
      .capacity-list div {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
      }
      .capacity-list dt {
        color: var(--muted);
        font-size: 0.78rem;
      }
      .capacity-list dd {
        margin: 0;
        font-weight: 700;
      }
      .capacity-track,
      .error-track {
        height: 0.4rem;
        overflow: hidden;
        background: var(--surface-subtle);
        border-radius: 4px;
      }
      .capacity-track {
        margin: 0 1rem;
      }
      .capacity-track span {
        display: block;
        height: 100%;
        background: var(--ds-cobalt-blue);
      }
      .capacity-panel > small {
        display: block;
        padding: 0.55rem 1rem 1rem;
        color: var(--muted-2);
      }
      .integrations-panel {
        grid-column: 1/-1;
      }
      .integration-status {
        font-size: 0.68rem;
      }
      .integrations-panel .data-table {
        min-width: 850px;
      }
      .errors-panel {
        grid-column: 1/-1;
      }
      .error-list {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem 1.5rem;
        padding: 1rem;
      }
      .error-row > div:first-child {
        display: flex;
        justify-content: space-between;
        margin-bottom: 0.4rem;
        font-size: 0.78rem;
      }
      .error-track span {
        display: block;
        height: 100%;
        background: var(--danger);
      }
      .error-row small {
        display: block;
        margin-top: 0.3rem;
        color: var(--muted-2);
        font-size: 0.68rem;
      }
      .all-clear {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.8rem;
        padding: 2rem;
        color: var(--success);
      }
      .all-clear i {
        font-size: 1.5rem;
      }
      .all-clear div {
        display: grid;
      }
      .all-clear span {
        color: var(--muted);
        font-size: 0.75rem;
        font-weight: 400;
      }
      @media (max-width: 1200px) {
        .metrics-six {
          grid-template-columns: repeat(3, minmax(0, 1fr));
        }
        .metrics-six .metric:nth-child(3n) {
          border-right: 0;
        }
        .monitor-grid {
          grid-template-columns: 1fr;
        }
      }
      @media (max-width: 700px) {
        .page-head {
          display: grid;
        }
        .freshness {
          justify-items: start;
        }
        .filter-bar > * {
          width: 100%;
        }
        .filter-bar .btn-icon {
          width: 2.75rem;
        }
        .poll-note {
          margin-left: 0;
        }
        .metrics-six {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
        .metrics-six .metric:nth-child(3n) {
          border-right: 1px solid var(--line);
        }
        .metrics-six .metric:nth-child(2n) {
          border-right: 0;
        }
        .bar-chart {
          height: 13rem;
          overflow: hidden;
        }
        .error-list {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class MonitoringPage {
  private service = inject(MonitoringService);
  private config = inject(APP_CONFIG);
  private destroyRef = inject(DestroyRef);
  private reload = new Subject<void>();
  period: MonitoringPeriod = '24H';
  customStart = '';
  customEnd = '';
  loading = signal(false);
  error = signal('');
  lastUpdated = signal<Date | null>(null);
  summary = signal<MonitoringSummary | null>(null);
  series = signal<MonitoringSeries | null>(null);
  integrations = signal<MonitoringIntegrationList | null>(null);
  errors = signal<MonitoringErrors | null>(null);
  capacity = signal<MonitoringCapacity | null>(null);

  constructor() {
    merge(timer(0, this.config.monitoringPollingMs), this.reload)
      .pipe(
        exhaustMap(() => this.fetch()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.summary.set(result.summary);
        this.series.set(result.series);
        this.integrations.set(result.integrations);
        this.errors.set(result.errors);
        this.capacity.set(result.capacity);
        this.lastUpdated.set(new Date());
        this.error.set('');
      });
  }

  apply() {
    this.refresh();
  }
  refresh() {
    this.reload.next();
  }
  formatMs(value: number) {
    return value >= 1000
      ? `${(value / 1000).toFixed(value >= 10000 ? 0 : 1)} s`
      : `${Math.round(value)} ms`;
  }
  barHeight(value: number) {
    const max = Math.max(
      1,
      ...(this.series()?.pontos.map((point) => point.totalRequisicoes) || [1]),
    );
    return Math.max(value ? 3 : 0, (value / max) * 100);
  }
  showChartLabel(index: number) {
    const size = this.series()?.pontos.length || 1;
    return index % Math.max(1, Math.ceil(size / 8)) === 0;
  }
  queuePercent(cap: MonitoringCapacity) {
    return cap.capacidadeFila ? (cap.filaEmUso / cap.capacidadeFila) * 100 : 0;
  }
  errorLabel(type: string) {
    return (
      (
        {
          AUTENTICACAO: 'Autenticação',
          LIMITE_DE_TAXA: 'Limite de taxa',
          VALIDACAO: 'Validação',
          TIMEOUT: 'Timeout',
          PROCESSAMENTO: 'Processamento',
          OUTROS_4XX: 'Outros erros 4xx',
          OUTROS_5XX: 'Outros erros 5xx',
        } as Record<string, string>
      )[type] || type
    );
  }

  private fetch() {
    this.loading.set(true);
    const query: MonitoringQuery = { periodo: this.period };
    if (this.period === 'CUSTOM') {
      query.dataInicio = new Date(this.customStart).toISOString();
      query.dataFim = new Date(this.customEnd).toISOString();
    }
    return forkJoin({
      summary: this.service.summary(query),
      series: this.service.timeSeries(query),
      integrations: this.service.integrations(query),
      errors: this.service.errors(query),
      capacity: this.service.capacity(),
    }).pipe(
      catchError((error) => {
        this.error.set(error.detail || 'Não foi possível atualizar o monitoramento.');
        return EMPTY;
      }),
      finalize(() => this.loading.set(false)),
    );
  }
}
