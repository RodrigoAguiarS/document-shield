import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  AnalysisDto,
  AnalysisStatus,
  PagedResponse,
  RiskLevel,
} from '../../core/models/api.models';
import { AnalysisService } from '../../core/services/analysis.service';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';
@Component({
  selector: 'app-analyses-page',
  standalone: true,
  imports: [DatePipe, RouterLink, RiskBadge, StatusBadge, EmptyState, ErrorState, LoadingState],
  template: `<div class="page">
    <div class="page-head">
      <div>
        <h1>Análises</h1>
        <p>Consulte execuções, resultados e histórico de avaliação.</p>
      </div>
    </div>
    <section class="panel">
      <div class="panel-head">
        <h2>Histórico global</h2>
        <div class="toolbar">
          <select
            class="control"
            aria-label="Status"
            [value]="status()"
            (change)="changeStatus($event)"
          >
            <option value="">Todos os status</option>
            @for (s of statuses; track s) {
              <option [value]="s">{{ s }}</option>
            }</select
          ><select
            class="control"
            aria-label="Nível de risco"
            [value]="risk()"
            (change)="changeRisk($event)"
          >
            <option value="">Todos os riscos</option>
            @for (r of risks; track r) {
              <option [value]="r">{{ r }}</option>
            }
          </select>
        </div>
      </div>
      @if (loading()) {
        <app-loading-state />
      } @else if (error()) {
        <app-error-state [message]="error()" (retry)="load()" />
      } @else if (data(); as d) {
        @if (d.content.length) {
          <div class="data-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Documento</th>
                  <th>Status</th>
                  <th>Risco</th>
                  <th>Pontuação</th>
                  <th>Ocorrências</th>
                  <th>Início</th>
                  <th>Duração</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (a of d.content; track a.id) {
                  <tr>
                    <td>
                      <a class="cell-title" [routerLink]="['/documentos', a.documentoId]"
                        >Documento {{ short(a.documentoId) }}</a
                      ><span class="cell-sub mono">{{ a.id }}</span>
                    </td>
                    <td><app-status-badge [status]="a.status" /></td>
                    <td><app-risk-badge [level]="a.nivelRisco" /></td>
                    <td>{{ a.pontuacaoRisco ?? '—' }}</td>
                    <td>{{ a.totalOcorrencias }}</td>
                    <td>{{ a.dataInicio ? (a.dataInicio | date: 'dd/MM/yyyy HH:mm') : '—' }}</td>
                    <td>{{ duration(a.duracaoMilissegundos) }}</td>
                    <td>
                      <a
                        class="btn btn-secondary btn-icon"
                        [routerLink]="['/analises', a.id]"
                        aria-label="Abrir"
                        ><i class="pi pi-chevron-right"></i
                      ></a>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <span>{{ d.totalElements }} análise(s)</span>
            <div class="pagination-actions">
              <button [disabled]="d.first" (click)="changePage(-1)">
                <i class="pi pi-chevron-left"></i></button
              ><button class="active">{{ d.page + 1 }}</button
              ><button [disabled]="d.last" (click)="changePage(1)">
                <i class="pi pi-chevron-right"></i>
              </button>
            </div>
          </div>
        } @else {
          <app-empty-state
            title="Nenhuma análise"
            message="Não existem análises para os filtros selecionados."
          />
        }
      }
    </section>
  </div>`,
})
export class AnalysesPage {
  private readonly service = inject(AnalysisService);

  readonly data = signal<PagedResponse<AnalysisDto> | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly page = signal(0);
  readonly status = signal<AnalysisStatus | ''>('');
  readonly risk = signal<RiskLevel | ''>('');
  readonly statuses: AnalysisStatus[] = ['PENDENTE', 'EXECUTANDO', 'CONCLUIDA', 'FALHOU'];
  readonly risks: RiskLevel[] = ['INFORMATIVO', 'BAIXO', 'MEDIO', 'ALTO', 'CRITICO'];

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service
      .list(
        { page: this.page(), size: 20, sort: 'dataInicio,desc' },
        this.status() || undefined,
        this.risk() || undefined,
      )
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(error.detail ?? 'Falha ao carregar análises.');
          this.loading.set(false);
        },
      });
  }

  changeStatus(event: Event): void {
    this.status.set((event.target as HTMLSelectElement).value as AnalysisStatus | '');
    this.resetPageAndLoad();
  }

  changeRisk(event: Event): void {
    this.risk.set((event.target as HTMLSelectElement).value as RiskLevel | '');
    this.resetPageAndLoad();
  }

  changePage(offset: -1 | 1): void {
    this.page.update((currentPage) => currentPage + offset);
    this.load();
  }

  short(id: string): string {
    return id.slice(0, 8).toUpperCase();
  }

  duration(ms: number | null): string {
    if (ms === null) return '—';
    return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;
  }

  private resetPageAndLoad(): void {
    this.page.set(0);
    this.load();
  }
}
