import { DatePipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  ApiKeyDto,
  IntegrationPolicyBindingDto,
  SecurityPolicyDto,
} from '../../core/models/api.models';
import { PolicyService } from '../../core/services/policy.service';

@Component({
  selector: 'app-integration-policy-panel',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './integration-policy-panel.html',
  styleUrl: './integration-policy-panel.css',
})
export class IntegrationPolicyPanel {
  private readonly service = inject(PolicyService);
  readonly integrations = input.required<ApiKeyDto[]>();
  readonly policies = input.required<SecurityPolicyDto[]>();
  readonly binding = signal<IntegrationPolicyBindingDto | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  selectedIntegrationId = '';
  selectedPolicyId = '';

  integrationChanged(): void {
    this.binding.set(null);
    this.selectedPolicyId = '';
    this.error.set('');
    if (!this.selectedIntegrationId) return;
    this.loading.set(true);
    this.service
      .binding(this.selectedIntegrationId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (binding) => {
          this.binding.set(binding);
          this.selectedPolicyId = binding.utilizaPoliticaGlobal ? '' : binding.politicaId;
        },
        error: (failure: Error) => this.error.set(failure.message),
      });
  }

  save(): void {
    if (!this.selectedIntegrationId || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    const request = this.selectedPolicyId
      ? this.service.bind(this.selectedIntegrationId, this.selectedPolicyId)
      : this.service.unbind(this.selectedIntegrationId);
    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (binding) => this.binding.set(binding),
      error: (failure: Error) => this.error.set(failure.message),
    });
  }

  specificPolicies(): SecurityPolicyDto[] {
    return this.policies().filter(
      (policy) => policy.tipo === 'INTEGRACAO' && policy.status === 'ATIVA',
    );
  }
}
