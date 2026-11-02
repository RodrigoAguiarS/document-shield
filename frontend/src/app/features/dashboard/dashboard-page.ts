import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AnalysisDto, DashboardDto } from '../../core/models/api.models';
import { AnalysisService } from '../../core/services/analysis.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { DocumentUpload } from '../../shared/components/document-upload/document-upload';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    DocumentUpload,
    RiskBadge,
    StatusBadge,
    EmptyState,
    ErrorState,
    LoadingState,
  ],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h1>Visão geral</h1>
          <p>Acompanhe documentos, análises e sinais que exigem atenção.</p>
        </div>
      </div>
      @if (loading()) {
        <app-loading-state />
      } @else if (error()) {
        <app-error-state [message]="error()" (retry)="load()" />
      } @else if (summary(); as s) {
        <section class="metric-strip" aria-label="Indicadores">
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-file" aria-hidden="true"></i>Documentos</span
            >
            <div class="metric-value">{{ s.totalDocumentos }}</div>
            <span class="metric-hint">Total no sistema</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-search" aria-hidden="true"></i>Análises</span
            >
            <div class="metric-value">{{ s.totalAnalises }}</div>
            <span class="metric-hint">{{ s.analisesConcluidas }} concluídas</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-clock" aria-hidden="true"></i>Em andamento</span
            >
            <div class="metric-value">{{ s.analisesEmAndamento }}</div>
            <span class="metric-hint">Processamento assíncrono</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-flag" aria-hidden="true"></i>Ocorrências</span
            >
            <div class="metric-value">{{ s.totalOcorrencias }}</div>
            <span class="metric-hint">Evidências identificadas</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-check-circle" aria-hidden="true"></i>Concluídas</span
            >
            <div class="metric-value">{{ s.analisesConcluidas }}</div>
            <span class="metric-hint">Processamento finalizado</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-times-circle" aria-hidden="true"></i>Com falha</span
            >
            <div class="metric-value">{{ s.analisesComFalha }}</div>
            <span class="metric-hint">Requerem verificação</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-exclamation-triangle" aria-hidden="true"></i>Risco alto</span
            >
            <div class="metric-value">{{ s.documentosComRiscoAlto }}</div>
            <span class="metric-hint">Documentos</span>
          </div>
          <div class="metric">
            <span class="metric-label"
              ><i class="pi pi-bolt" aria-hidden="true"></i>Risco crítico</span
            >
            <div class="metric-value">{{ s.documentosComRiscoCritico }}</div>
            <span class="metric-hint">Documentos</span>
          </div>
        </section>
        <app-document-upload />
        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-head">
              <h2><i class="pi pi-list-check"></i> Análises recentes</h2>
              <a class="btn btn-secondary" routerLink="/analises"
                >Ver todas <i class="pi pi-chevron-right"></i
              ></a>
            </div>
            @if (recent().length) {
              <div class="data-table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Documento</th>
                      <th>Status</th>
                      <th>Risco</th>
                      <th>Ocorrências</th>
                      <th>Início</th>
                      <th><span class="sr-only">Ação</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (a of recent(); track a.id) {
                      <tr>
                        <td>
                          <span class="cell-title">Documento {{ short(a.documentoId) }}</span
                          ><span class="cell-sub">Nome indisponível neste contrato</span>
                        </td>
                        <td><app-status-badge [status]="a.status" /></td>
                        <td><app-risk-badge [level]="a.nivelRisco" /></td>
                        <td>{{ a.totalOcorrencias }}</td>
                        <td>
                          {{ a.dataInicio ? (a.dataInicio | date: 'dd/MM/yyyy HH:mm') : '—' }}
                        </td>
                        <td>
                          <a
                            class="btn btn-secondary btn-icon"
                            [routerLink]="['/analises', a.id]"
                            aria-label="Abrir análise"
                            ><i class="pi pi-chevron-right"></i
                          ></a>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            } @else {
              <app-empty-state
                title="Nenhuma análise"
                message="Envie um PDF para iniciar a primeira análise."
              />
            }
          </section>
          <aside class="risk-rail panel">
            <div class="panel-head">
              <h2><i class="pi pi-chart-bar"></i> Resumo de risco</h2>
            </div>
            <div class="risk-lines">
              <div>
                <span><b class="dot critical"></b>Risco crítico</span
                ><strong>{{ s.documentosComRiscoCritico }}</strong>
              </div>
              <div>
                <span><b class="dot high"></b>Risco alto</span
                ><strong>{{ s.documentosComRiscoAlto }}</strong>
              </div>
            </div>
            <div class="rail-empty">
              <i class="pi pi-chart-pie"></i><strong>Distribuição completa indisponível</strong
              ><span
                >A API atual não fornece agregações por todos os níveis ou tipos de
                ocorrência.</span
              >
            </div>
          </aside>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .metric-strip .metric:nth-child(-n + 4) {
        border-bottom: 1px solid var(--line);
      }
      .metric-label {
        display: inline-flex;
        align-items: center;
        gap: 0.42rem;
      }
      .metric-label i {
        color: #6fa4ff;
        font-size: 0.82rem;
      }
      .dashboard-grid {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 20rem;
        gap: 1rem;
      }
      .panel-head i {
        margin-right: 0.35rem;
        color: var(--ink-700);
      }
      .risk-lines {
        padding: 1rem;
        display: grid;
        gap: 0.9rem;
        border-bottom: 1px solid var(--line);
      }
      .risk-lines div {
        display: flex;
        justify-content: space-between;
      }
      .risk-lines span {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.82rem;
      }
      .dot {
        width: 0.55rem;
        height: 0.55rem;
        border-radius: 50%;
      }
      .critical {
        background: var(--risk-critical);
      }
      .high {
        background: var(--risk-high);
      }
      .rail-empty {
        text-align: center;
        padding: 2.2rem 1rem;
        color: var(--muted);
        display: grid;
        gap: 0.55rem;
      }
      .rail-empty i {
        font-size: 1.5rem;
      }
      .rail-empty strong {
        color: var(--text);
        font-size: 0.85rem;
      }
      .rail-empty span {
        font-size: 0.76rem;
        line-height: 1.5;
      }
      .sr-only {
        position: absolute;
        clip: rect(0, 0, 0, 0);
      }
      @media (max-width: 1100px) {
        .metric-strip .metric:nth-child(-n + 4) {
          border-bottom: 1px solid var(--line);
        }
        .dashboard-grid {
          grid-template-columns: 1fr;
        }
        .risk-rail {
          display: none;
        }
      }
    `,
  ],
})
export class DashboardPage {
  private dashboard = inject(DashboardService);
  private analyses = inject(AnalysisService);
  private destroy = inject(DestroyRef);
  summary = signal<DashboardDto | null>(null);
  recent = signal<AnalysisDto[]>([]);
  loading = signal(true);
  error = signal('');

  constructor() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      summary: this.dashboard.summary(),
      recent: this.analyses.list({ page: 0, size: 10, sort: 'dataInicio,desc' }),
    })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (result) => {
          this.summary.set(result.summary);
          this.recent.set(result.recent.content);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(error?.detail ?? 'Não foi possível carregar o dashboard.');
          this.loading.set(false);
        },
      });
  }

  short(id: string) {
    return id.slice(0, 8).toUpperCase();
  }
}
