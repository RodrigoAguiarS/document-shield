import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService } from 'primeng/api';
import { finalize, forkJoin } from 'rxjs';
import {
  ApiKeyDto,
  PolicyAuditDto,
  SavePolicyRequest,
  SecurityPolicyDto,
} from '../../core/models/api.models';
import { ApiKeyService } from '../../core/services/api-key.service';
import { PolicyService } from '../../core/services/policy.service';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';
import { IntegrationPolicyPanel } from './integration-policy-panel';
import { PolicyEditor } from './policy-editor';
import { PolicySimulator } from './policy-simulator';

@Component({
  selector: 'app-policies-page',
  standalone: true,
  imports: [
    DatePipe,
    DialogModule,
    EmptyState,
    ErrorState,
    LoadingState,
    PolicyEditor,
    PolicySimulator,
    IntegrationPolicyPanel,
  ],
  templateUrl: './policies-page.html',
  styleUrl: './policies-page.css',
})
export class PoliciesPage {
  private readonly policiesApi = inject(PolicyService);
  private readonly apiKeys = inject(ApiKeyService);
  private readonly confirmations = inject(ConfirmationService);

  readonly policies = signal<SecurityPolicyDto[]>([]);
  readonly integrations = signal<ApiKeyDto[]>([]);
  readonly audits = signal<PolicyAuditDto[]>([]);
  readonly versions = signal<SecurityPolicyDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly operationMessage = signal('');
  readonly editorVisible = signal(false);
  readonly versionsVisible = signal(false);
  readonly editing = signal<SecurityPolicyDto | null>(null);
  readonly saving = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      policies: this.policiesApi.list(),
      integrations: this.apiKeys.list({ page: 0, size: 100 }),
      audits: this.policiesApi.audits(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => {
          this.policies.set(data.policies);
          this.integrations.set(data.integrations.content);
          this.audits.set(data.audits);
        },
        error: (failure: Error) => this.error.set(failure.message),
      });
  }

  openCreate(): void {
    this.editing.set(null);
    this.editorVisible.set(true);
  }
  openEdit(policy: SecurityPolicyDto): void {
    this.editing.set(policy);
    this.editorVisible.set(true);
  }

  save(request: SavePolicyRequest): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.operationMessage.set('');
    const current = this.editing();
    const operation = current
      ? this.policiesApi.update(current.id, request)
      : this.policiesApi.create(request);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => {
        this.editorVisible.set(false);
        this.operationMessage.set(
          current?.status === 'ATIVA'
            ? `Versão ${saved.versao} criada em rascunho.`
            : 'Política salva em rascunho.',
        );
        this.load();
      },
      error: (failure: Error) => this.operationMessage.set(failure.message),
    });
  }

  activate(policy: SecurityPolicyDto): void {
    this.confirmations.confirm({
      header: 'Ativar política',
      message: `Ativar ${policy.nome}, versão ${policy.versao}? A versão ativa anterior será arquivada.`,
      icon: 'pi pi-shield',
      acceptLabel: 'Ativar',
      rejectLabel: 'Cancelar',
      accept: () => {
        this.policiesApi.activate(policy.id).subscribe({
          next: () => {
            this.operationMessage.set('Política ativada.');
            this.load();
          },
          error: (failure: Error) => this.operationMessage.set(failure.message),
        });
      },
    });
  }

  showVersions(policy: SecurityPolicyDto): void {
    this.policiesApi.versions(policy.id).subscribe({
      next: (versions) => {
        this.versions.set(versions);
        this.versionsVisible.set(true);
      },
      error: (failure: Error) => this.operationMessage.set(failure.message),
    });
  }

  statusLabel(status: string): string {
    return status.replaceAll('_', ' ');
  }
  operationLabel(operation: string): string {
    return operation.replaceAll('_', ' ');
  }
}
