import { Component, input } from '@angular/core';
import { AnalysisStatus, DocumentStatus } from '../../../core/models/api.models';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="status" [class]="'status ' + status().toLowerCase()"
    ><span class="status-dot" aria-hidden="true"></span>{{ label() }}</span
  >`,
  styles: [
    `
      .status {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.02em;
        white-space: nowrap;
      }
      .concluida,
      .pronto {
        color: var(--success);
      }
      .executando,
      .validando,
      .pendente,
      .recebido {
        color: #6fa4ff;
      }
      .falhou,
      .erro,
      .rejeitado {
        color: var(--danger);
      }
    `,
  ],
})
export class StatusBadge {
  status = input.required<AnalysisStatus | DocumentStatus>();
  label = () => this.status().replace('_', ' ');
}
