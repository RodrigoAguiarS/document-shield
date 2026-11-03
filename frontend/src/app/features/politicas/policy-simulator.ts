import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { PolicyEvaluationDto, SecurityPolicyDto } from '../../core/models/api.models';
import { PolicyService } from '../../core/services/policy.service';
import { PolicyDecisionBadge } from '../../shared/components/policy-decision-badge/policy-decision-badge';

@Component({
  selector: 'app-policy-simulator',
  standalone: true,
  imports: [FormsModule, PolicyDecisionBadge],
  templateUrl: './policy-simulator.html',
  styleUrl: './policy-simulator.css',
})
export class PolicySimulator {
  private readonly service = inject(PolicyService);
  readonly policies = input.required<SecurityPolicyDto[]>();
  readonly result = signal<PolicyEvaluationDto | null>(null);
  readonly error = signal('');
  readonly loading = signal(false);
  policyId = '';
  score = 45;

  simulate(): void {
    if (!this.policyId || this.score < 0 || this.score > 100 || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.result.set(null);
    this.service
      .simulate(this.policyId, this.score)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => this.result.set(result),
        error: (failure: Error) => this.error.set(failure.message),
      });
  }
}
