import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import { AnalysisDto, DocumentDto, PagedResponse } from '../../core/models/api.models';
import { DocumentService } from '../../core/services/document.service';
import { RiskBadge } from '../../shared/components/risk-badge/risk-badge';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import { ErrorState, LoadingState } from '../../shared/components/view-states/view-states';
@Component({
  selector: 'app-document-detail-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink, RiskBadge, StatusBadge, ErrorState, LoadingState],
  template: `<div class="page">
    <div class="page-head">
      <div>
        <a class="back" routerLink="/documentos"><i class="pi pi-arrow-left"></i> Documentos</a>
        <h1>{{ document()?.nomeOriginal || 'Detalhes do documento' }}</h1>
        <p>Metadados do arquivo e histórico imutável de análises.</p>
      </div>
      @if (document(); as d) {
        <div class="toolbar">
          <button class="btn btn-secondary" (click)="download(d)">
            <i class="pi pi-download"></i>Baixar</button
          ><button class="btn btn-primary" (click)="reanalyze(d)">
            <i class="pi pi-refresh"></i>Reanalisar
          </button>
        </div>
      }
    </div>
    @if (loading()) {
      <app-loading-state />
    } @else if (error()) {
      <app-error-state [message]="error()" (retry)="load()" />
    } @else if (document(); as d) {
      <section class="document-band panel">
        <div><span>STATUS</span><app-status-badge [status]="d.status" /></div>
        <div>
          <span>UPLOAD</span><strong>{{ d.dataUpload | date: 'dd/MM/yyyy HH:mm' }}</strong>
        </div>
        <div>
          <span>PÁGINAS</span><strong>{{ d.quantidadePaginas ?? 'Não determinado' }}</strong>
        </div>
        <div>
          <span>TAMANHO</span><strong>{{ d.tamanhoBytes / 1048576 | number: '1.1-2' }} MB</strong>
        </div>
        <div>
          <span>SHA-256</span><code>{{ d.hashSha256 }}</code>
        </div>
      </section>
      <section class="panel history">
        <div class="panel-head">
          <h2>Histórico de análises</h2>
          <span class="muted">{{ history()?.totalElements ?? 0 }} registro(s)</span>
        </div>
        @if (history(); as h) {
          @if (h.content.length) {
            <div class="timeline">
              @for (a of h.content; track a.id) {
                <a class="timeline-row" [routerLink]="['/analises', a.id]"
                  ><span class="timeline-marker" [class.failed]="a.status === 'FALHOU'"></span>
                  <div>
                    <strong>{{
                      a.dataInicio ? (a.dataInicio | date: 'dd/MM/yyyy HH:mm') : 'Aguardando início'
                    }}</strong
                    ><span>{{ a.versaoMotor }} · {{ duration(a.duracaoMilissegundos) }}</span>
                  </div>
                  <app-status-badge [status]="a.status" /><app-risk-badge
                    [level]="a.nivelRisco" /><span>{{ a.totalOcorrencias }} ocorrência(s)</span
                  ><i class="pi pi-chevron-right"></i
                ></a>
              }
            </div>
          } @else {
            <div class="empty-block">Nenhuma análise registrada.</div>
          }
        }
      </section>
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
      .document-band {
        display: grid;
        grid-template-columns: 0.8fr 1fr 0.65fr 0.75fr 2.5fr;
        margin-bottom: 1.25rem;
      }
      .document-band > div {
        padding: 1rem;
        border-right: 1px solid var(--line);
        display: grid;
        gap: 0.45rem;
        min-width: 0;
      }
      .document-band > div:last-child {
        border: 0;
      }
      .document-band span {
        font-size: 0.68rem;
        color: var(--muted);
        font-weight: 800;
      }
      .document-band strong {
        font-size: 0.86rem;
      }
      .document-band code {
        font-size: 0.68rem;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .timeline {
        padding: 0.35rem 1rem;
      }
      .timeline-row {
        display: grid;
        grid-template-columns: auto 1.8fr 1fr 1fr 1fr auto;
        align-items: center;
        gap: 1rem;
        padding: 1rem 0;
        border-bottom: 1px solid var(--line);
        font-size: 0.8rem;
      }
      .timeline-row:last-child {
        border: 0;
      }
      .timeline-row > div {
        display: grid;
        gap: 0.25rem;
      }
      .timeline-row > div span {
        color: var(--muted);
        font-size: 0.74rem;
      }
      .timeline-marker {
        width: 0.65rem;
        height: 0.65rem;
        border: 2px solid var(--teal-600);
        border-radius: 50%;
      }
      .timeline-marker.failed {
        border-color: var(--danger);
      }
      @media (max-width: 900px) {
        .document-band {
          grid-template-columns: 1fr 1fr;
        }
        .document-band > div {
          border-bottom: 1px solid var(--line);
        }
        .document-band > div:last-child {
          grid-column: 1/-1;
        }
        .timeline-row {
          grid-template-columns: auto 1fr auto;
        }
        .timeline-row app-status-badge,
        .timeline-row app-risk-badge,
        .timeline-row > span {
          display: none;
        }
      }
    `,
  ],
})
export class DocumentDetailPage {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(DocumentService);
  private messages = inject(MessageService);
  id = this.route.snapshot.paramMap.get('id')!;
  document = signal<DocumentDto | null>(null);
  history = signal<PagedResponse<AnalysisDto> | null>(null);
  loading = signal(true);
  error = signal('');
  constructor() {
    this.load();
  }
  load() {
    this.loading.set(true);
    forkJoin({
      document: this.service.get(this.id),
      history: this.service.history(this.id, { page: 0, size: 50, sort: 'dataInicio,desc' }),
    }).subscribe({
      next: (r) => {
        this.document.set(r.document);
        this.history.set(r.history);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e.detail ?? 'Não foi possível carregar o documento.');
        this.loading.set(false);
      },
    });
  }
  duration(ms: number | null) {
    if (ms === null) return 'duração indisponível';
    if (ms < 1000) return `${ms} ms`;
    return `${(ms / 1000).toFixed(1)} s`;
  }
  reanalyze(d: DocumentDto) {
    this.service.reanalyze(d.id).subscribe({
      next: (r) => void this.router.navigate(['/analises', r.analiseId]),
      error: (e) =>
        this.messages.add({ severity: 'error', summary: 'Falha na reanálise', detail: e.detail }),
    });
  }
  download(d: DocumentDto) {
    this.service.download(d.id).subscribe((r) => {
      const u = URL.createObjectURL(r.body!);
      const a = document.createElement('a');
      a.href = u;
      a.download = d.nomeOriginal;
      a.click();
      URL.revokeObjectURL(u);
    });
  }
}
