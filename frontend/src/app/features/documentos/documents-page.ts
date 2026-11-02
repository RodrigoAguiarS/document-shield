import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DocumentDto, DocumentStatus, PagedResponse } from '../../core/models/api.models';
import { DocumentService } from '../../core/services/document.service';
import { DocumentUpload } from '../../shared/components/document-upload/document-upload';
import { StatusBadge } from '../../shared/components/status-badge/status-badge';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';
@Component({
  selector: 'app-documents-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    DocumentUpload,
    StatusBadge,
    EmptyState,
    ErrorState,
    LoadingState,
  ],
  template: `<div class="page">
    <div class="page-head">
      <div>
        <h1>Documentos</h1>
        <p>Envie PDFs, acompanhe o processamento e consulte o histórico.</p>
      </div>
    </div>
    <app-document-upload />
    <section class="panel">
      <div class="panel-head">
        <h2>Documentos enviados</h2>
        <div class="toolbar">
          <select
            class="control"
            aria-label="Filtrar por status"
            [value]="status()"
            (change)="changeStatus($event)"
          >
            <option value="">Todos os status</option>
            @for (s of statuses; track s) {
              <option [value]="s">{{ s }}</option>
            }</select
          ><button
            class="btn btn-secondary btn-icon"
            type="button"
            (click)="load()"
            aria-label="Atualizar"
          >
            <i class="pi pi-refresh"></i>
          </button>
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
                  <th>Upload</th>
                  <th>Páginas</th>
                  <th>Tamanho</th>
                  <th>Status</th>
                  <th>Risco mais recente</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (doc of d.content; track doc.id) {
                  <tr>
                    <td>
                      <span class="cell-title">{{ doc.nomeOriginal }}</span
                      ><span class="cell-sub mono">{{ doc.hashSha256.slice(0, 12) }}…</span>
                    </td>
                    <td>{{ doc.dataUpload | date: 'dd/MM/yyyy HH:mm' }}</td>
                    <td>{{ doc.quantidadePaginas ?? '—' }}</td>
                    <td>{{ doc.tamanhoBytes / 1048576 | number: '1.1-1' }} MB</td>
                    <td><app-status-badge [status]="doc.status" /></td>
                    <td><span class="unavailable">Consultar histórico</span></td>
                    <td>
                      <div class="row-actions">
                        <a
                          class="btn btn-secondary btn-icon"
                          [routerLink]="['/documentos', doc.id]"
                          aria-label="Ver detalhes"
                          ><i class="pi pi-eye"></i></a
                        ><button
                          class="btn btn-secondary btn-icon"
                          type="button"
                          (click)="reanalyze(doc)"
                          [disabled]="doc.status === 'VALIDANDO'"
                          aria-label="Reanalisar"
                        >
                          <i class="pi pi-refresh"></i></button
                        ><button
                          class="btn btn-secondary btn-icon"
                          type="button"
                          (click)="download(doc)"
                          aria-label="Baixar"
                        >
                          <i class="pi pi-download"></i></button
                        ><button
                          class="btn btn-danger btn-icon"
                          type="button"
                          (click)="confirmDelete(doc)"
                          aria-label="Excluir"
                        >
                          <i class="pi pi-trash"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <span>{{ d.totalElements }} documento(s)</span>
            <div class="pagination-actions">
              <button [disabled]="d.first" (click)="page.set(page() - 1); load()">
                <i class="pi pi-chevron-left"></i></button
              ><button class="active">{{ d.page + 1 }}</button
              ><button [disabled]="d.last" (click)="page.set(page() + 1); load()">
                <i class="pi pi-chevron-right"></i>
              </button>
            </div>
          </div>
        } @else {
          <app-empty-state title="Nenhum documento" message="Envie um PDF para começar." />
        }
      }
    </section>
  </div>`,
  styles: [
    `
      .row-actions {
        display: flex;
        gap: 0.35rem;
      }
      .unavailable {
        font-size: 0.75rem;
        color: var(--muted);
        font-style: italic;
      }
      @media (max-width: 900px) {
        .panel-head {
          align-items: flex-start;
          flex-direction: column;
        }
        .control {
          width: 100%;
        }
      }
    `,
  ],
})
export class DocumentsPage {
  private service = inject(DocumentService);
  private confirm = inject(ConfirmationService);
  private messages = inject(MessageService);
  data = signal<PagedResponse<DocumentDto> | null>(null);
  loading = signal(true);
  error = signal('');
  page = signal(0);
  status = signal<DocumentStatus | ''>('');
  statuses: DocumentStatus[] = ['RECEBIDO', 'VALIDANDO', 'PRONTO', 'REJEITADO', 'ERRO'];
  constructor() {
    this.load();
  }
  load() {
    this.loading.set(true);
    this.error.set('');
    this.service
      .list({ page: this.page(), size: 20, sort: 'dataUpload,desc' }, this.status() || undefined)
      .subscribe({
        next: (d) => {
          this.data.set(d);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(e.detail ?? 'Não foi possível carregar os documentos.');
          this.loading.set(false);
        },
      });
  }
  changeStatus(e: Event) {
    this.status.set((e.target as HTMLSelectElement).value as DocumentStatus | '');
    this.page.set(0);
    this.load();
  }
  reanalyze(d: DocumentDto) {
    this.service.reanalyze(d.id).subscribe({
      next: (r) => {
        this.messages.add({
          severity: 'success',
          summary: 'Reanálise solicitada',
          detail: 'A nova análise foi enfileirada.',
        });
        location.href = `/analises/${r.analiseId}`;
      },
      error: (e) =>
        this.messages.add({
          severity: 'error',
          summary: 'Não foi possível reanalisar',
          detail: e.detail,
        }),
    });
  }
  download(d: DocumentDto) {
    this.service.download(d.id).subscribe({
      next: (r) => {
        const url = URL.createObjectURL(r.body!);
        const a = document.createElement('a');
        a.href = url;
        a.download = d.nomeOriginal;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) =>
        this.messages.add({ severity: 'error', summary: 'Falha no download', detail: e.detail }),
    });
  }
  confirmDelete(d: DocumentDto) {
    this.confirm.confirm({
      header: 'Excluir documento',
      message: `Excluir “${d.nomeOriginal}” e todo o histórico relacionado?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Excluir',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.service.delete(d.id).subscribe({
          next: () => {
            this.messages.add({ severity: 'success', summary: 'Documento excluído' });
            this.load();
          },
          error: (e) =>
            this.messages.add({
              severity: 'error',
              summary: 'Não foi possível excluir',
              detail: e.detail,
            }),
        }),
    });
  }
}
