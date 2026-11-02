import { Component, input } from '@angular/core';
import { RiskLevel } from '../../../core/models/api.models';

@Component({
  selector: 'app-risk-badge',
  standalone: true,
  template: `<span class="risk" [class]="'risk ' + (level() || 'AUSENTE').toLowerCase()"
    ><i class="pi" [class]="icon()" aria-hidden="true"></i>{{ label() }}</span
  >`,
  styles: [
    `
      .risk {
        display: inline-flex;
        align-items: center;
        gap: 0.35rem;
        padding: 0.3rem 0.5rem;
        border: 1px solid currentColor;
        border-radius: 5px;
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.025em;
        white-space: nowrap;
      }
      .risk i {
        font-size: 0.68rem;
      }
      .informativo {
        color: var(--risk-info);
        background: #1c2632;
      }
      .baixo {
        color: var(--risk-low);
        background: #12291f;
      }
      .medio {
        color: var(--risk-medium);
        background: #2b2312;
      }
      .alto {
        color: var(--risk-high);
        background: #2d1c14;
      }
      .critico {
        color: var(--risk-critical);
        background: #30171d;
      }
      .ausente {
        color: var(--muted);
        background: #1b222c;
      }
    `,
  ],
})
export class RiskBadge {
  level = input<RiskLevel | null>();
  label = () => this.level()?.replace('_', ' ') ?? 'NÃO AVALIADO';
  icon = () =>
    this.level() === 'CRITICO' || this.level() === 'ALTO' ? 'pi-exclamation-triangle' : 'pi-shield';
}
