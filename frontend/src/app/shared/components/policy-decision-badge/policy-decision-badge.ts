import { Component, input } from '@angular/core';
import { PolicyDecision } from '../../../core/models/api.models';

@Component({
  selector: 'app-policy-decision-badge',
  standalone: true,
  template: `<span class="decision" [class]="'decision ' + decision().toLowerCase()">
    <i aria-hidden="true"></i>{{ decision() }}
  </span>`,
  styles: `
    .decision {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      min-height: 1.7rem;
      padding: 0 0.55rem;
      border: 1px solid;
      border-radius: 4px;
      font-size: 0.7rem;
      font-weight: 750;
      letter-spacing: 0.04em;
    }
    .decision i {
      width: 0.42rem;
      height: 0.42rem;
      border-radius: 50%;
      background: currentColor;
    }
    .allow {
      color: #53d383;
      background: #10251a;
      border-color: #285d3c;
    }
    .review {
      color: #ffc247;
      background: #2b2312;
      border-color: #66511f;
    }
    .block {
      color: #ff6675;
      background: #2a1419;
      border-color: #68313b;
    }
  `,
})
export class PolicyDecisionBadge {
  readonly decision = input.required<PolicyDecision>();
}
