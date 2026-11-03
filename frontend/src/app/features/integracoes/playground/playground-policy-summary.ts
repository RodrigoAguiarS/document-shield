import { Component, input } from '@angular/core';
import { PublicPolicyEvaluationDto } from '../../../core/models/api.models';
import { PolicyDecisionBadge } from '../../../shared/components/policy-decision-badge/policy-decision-badge';

@Component({
  selector: 'app-playground-policy-summary',
  standalone: true,
  imports: [PolicyDecisionBadge],
  templateUrl: './playground-policy-summary.html',
  styleUrl: './playground-policy-summary.css',
})
export class PlaygroundPolicySummary {
  readonly evaluation = input.required<PublicPolicyEvaluationDto>();
}
