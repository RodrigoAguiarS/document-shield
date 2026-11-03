import { DatePipe } from '@angular/common';
import { Component, inject, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { finalize, forkJoin } from 'rxjs';
import {
  ApiKeyDto,
  ApiKeyStatus,
  CreateApiKeyRequest,
  IntegrationPolicyBindingDto,
  PagedResponse,
  SecurityPolicyDto,
} from '../../core/models/api.models';
import { ApiKeyService } from '../../core/services/api-key.service';
import { PolicyService } from '../../core/services/policy.service';
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from '../../shared/components/view-states/view-states';

type ApiKeyForm = CreateApiKeyRequest & { expiraEm: string | null };

@Component({
  selector: 'app-api-keys-page',
  standalone: true,
  imports: [DatePipe, FormsModule, DialogModule, EmptyState, ErrorState, LoadingState],
  template: `
    <div class="page api-keys-page">
      <div class="page-head">
        <div>
          <h1>API Keys</h1>
          <p>Credenciais das integrações autorizadas a analisar documentos pela API pública.</p>
        </div>
        <button class="btn btn-primary" type="button" (click)="openCreate()">
          <i class="pi pi-plus"></i>Nova API Key
        </button>
      </div>

      <section class="credential-note" aria-label="Informação de segurança">
        <i class="pi pi-lock" aria-hidden="true"></i>
        <div>
          <strong>Segredos não são recuperáveis.</strong
          ><span
            >O token completo aparece somente na criação ou rotação. Chaves antigas exibem apenas um
            prefixo identificável.</span
          >
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2>Integrações cadastradas</h2>
          <form class="toolbar filters" (ngSubmit)="applyFilters()">
            <label class="search-field" for="api-key-search"
              ><i class="pi pi-search"></i><span class="sr-only">Filtrar por nome</span
              ><input
                id="api-key-search"
                class="control"
                name="search"
                [(ngModel)]="search"
                placeholder="Filtrar por nome"
            /></label>
            <label
              ><span class="sr-only">Filtrar por status</span
              ><select class="control" name="status" [(ngModel)]="status">
                <option value="">Todos os status</option>
                <option value="ATIVA">Ativas</option>
                <option value="EXPIRADA">Expiradas</option>
                <option value="REVOGADA">Revogadas</option>
              </select></label
            >
            <button class="btn btn-secondary" type="submit">Aplicar</button>
            <button
              class="btn btn-secondary btn-icon"
              type="button"
              (click)="load()"
              aria-label="Atualizar listagem"
            >
              <i class="pi pi-refresh"></i>
            </button>
          </form>
        </div>
        @if (loading()) {
          <app-loading-state />
        } @else if (error()) {
          <app-error-state [message]="error()" (retry)="load()" />
        } @else if (data(); as result) {
          @if (result.content.length) {
            <div class="data-table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Integração</th>
                    <th>Identificador</th>
                    <th>Escopo</th>
                    <th>Status</th>
                    <th>Criada em</th>
                    <th>Expiração</th>
                    <th>Último uso</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  @for (key of result.content; track key.id) {
                    <tr>
                      <td>
                        <span class="cell-title">{{ key.nome }}</span
                        ><span class="cell-sub">{{ key.descricao || 'Sem descrição' }}</span>
                      </td>
                      <td>
                        <span class="mono key-prefix">{{ key.prefixo }}</span
                        ><span class="cell-sub mono">{{ key.identificador.slice(0, 12) }}…</span>
                      </td>
                      <td>
                        <span class="scope">{{ scopeLabel(key.escopo) }}</span>
                      </td>
                      <td>
                        <span class="key-status" [class]="'key-status ' + key.status.toLowerCase()"
                          ><i class="status-dot"></i>{{ key.status }}</span
                        >
                      </td>
                      <td>{{ key.criadaEm | date: 'dd/MM/yyyy HH:mm' }}</td>
                      <td>
                        {{
                          key.expiraEm ? (key.expiraEm | date: 'dd/MM/yyyy HH:mm') : 'Sem expiração'
                        }}
                      </td>
                      <td>
                        {{
                          key.ultimoUsoEm
                            ? (key.ultimoUsoEm | date: 'dd/MM/yyyy HH:mm')
                            : 'Nunca utilizada'
                        }}
                      </td>
                      <td>
                        <div class="row-actions">
                          <button
                            class="btn btn-secondary btn-icon"
                            type="button"
                            (click)="showDetails(key)"
                            aria-label="Ver detalhes de {{ key.nome }}"
                          >
                            <i class="pi pi-eye"></i>
                          </button>
                          <button
                            class="btn btn-secondary btn-icon"
                            type="button"
                            (click)="confirmRotate(key)"
                            [disabled]="key.status !== 'ATIVA'"
                            aria-label="Rotacionar {{ key.nome }}"
                          >
                            <i class="pi pi-sync"></i>
                          </button>
                          <button
                            class="btn btn-danger btn-icon"
                            type="button"
                            (click)="confirmRevoke(key)"
                            [disabled]="key.status !== 'ATIVA'"
                            aria-label="Revogar {{ key.nome }}"
                          >
                            <i class="pi pi-ban"></i>
                          </button>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="pagination">
              <span>{{ result.totalElements }} integração(ões)</span>
              <div class="pagination-actions">
                <button
                  type="button"
                  [disabled]="result.first"
                  (click)="previous()"
                  aria-label="Página anterior"
                >
                  <i class="pi pi-chevron-left"></i></button
                ><button class="active" type="button" aria-current="page">
                  {{ result.page + 1 }}</button
                ><button
                  type="button"
                  [disabled]="result.last"
                  (click)="next()"
                  aria-label="Próxima página"
                >
                  <i class="pi pi-chevron-right"></i>
                </button>
              </div>
            </div>
          } @else {
            <app-empty-state
              title="Nenhuma API Key"
              message="Crie a primeira credencial para conectar uma integração."
            />
          }
        }
      </section>
    </div>

    <p-dialog
      header="Nova API Key"
      [modal]="true"
      [visible]="createVisible()"
      (visibleChange)="createVisible.set($event)"
      (onHide)="resetForm()"
      [style]="{ width: 'min(34rem, calc(100vw - 2rem))' }"
    >
      <form class="dialog-form" (ngSubmit)="create()" novalidate>
        <div class="field">
          <label for="integration-name">Nome da integração</label
          ><input
            id="integration-name"
            class="control"
            name="name"
            [(ngModel)]="form.nome"
            maxlength="255"
            required
          /><small>Use um nome reconhecível pela equipe.</small>
        </div>
        <div class="field">
          <label for="integration-description">Descrição <span>opcional</span></label
          ><textarea
            id="integration-description"
            class="control textarea"
            name="description"
            [(ngModel)]="form.descricao"
            maxlength="500"
            rows="3"
          ></textarea>
        </div>
        <div class="field">
          <label for="integration-expiry">Expiração <span>opcional</span></label
          ><input
            id="integration-expiry"
            class="control"
            name="expiry"
            type="datetime-local"
            [(ngModel)]="form.expiraEm"
          /><small>Sem data, a credencial permanece válida até ser revogada.</small>
        </div>
        <div class="field">
          <label for="integration-scope">Escopo</label
          ><select id="integration-scope" class="control" name="scope" [(ngModel)]="form.escopo">
            <option value="DOCUMENT_ANALYZE">Analisar documentos</option>
          </select>
        </div>
        <div class="field">
          <label for="integration-limit">Requisições por minuto <span>opcional</span></label
          ><input
            id="integration-limit"
            class="control"
            name="limit"
            type="number"
            min="1"
            max="10000"
            [(ngModel)]="form.requisicoesPorMinuto"
          /><small>Vazio utiliza o limite padrão do servidor.</small>
        </div>
        @if (formError()) {
          <p class="form-error" role="alert">{{ formError() }}</p>
        }
        <div class="dialog-actions">
          <button class="btn btn-secondary" type="button" (click)="createVisible.set(false)">
            Cancelar</button
          ><button class="btn btn-primary" type="submit" [disabled]="saving() || !form.nome.trim()">
            @if (saving()) {
              <i class="pi pi-spin pi-spinner"></i>Criando…
            } @else {
              Criar API Key
            }
          </button>
        </div>
      </form>
    </p-dialog>

    <p-dialog
      header="Token gerado"
      [modal]="true"
      [closable]="true"
      [closeOnEscape]="false"
      [visible]="secretVisible()"
      (visibleChange)="onSecretVisibility($event)"
      [style]="{ width: 'min(38rem, calc(100vw - 2rem))' }"
    >
      <div class="secret-dialog">
        <div class="success-mark"><i class="pi pi-check"></i></div>
        <h2>API Key criada com sucesso</h2>
        <p>Guarde esta chave em um local seguro. Ela não poderá ser visualizada novamente.</p>
        <label for="generated-token">Token de {{ secretName() }}</label
        ><textarea
          id="generated-token"
          class="secret-value mono"
          readonly
          rows="4"
          [value]="secret()"
          spellcheck="false"
        ></textarea
        ><button class="btn btn-primary copy-button" type="button" (click)="copySecret()">
          <i [class]="copied() ? 'pi pi-check' : 'pi pi-copy'"></i
          >{{ copied() ? 'Token copiado' : 'Copiar token' }}</button
        ><button class="btn btn-secondary" type="button" (click)="clearSecret()">
          Já guardei a chave
        </button>
      </div>
    </p-dialog>

    <p-dialog
      header="Detalhes da credencial"
      [modal]="true"
      [visible]="detailsVisible()"
      (visibleChange)="detailsVisible.set($event)"
      [style]="{ width: 'min(35rem, calc(100vw - 2rem))' }"
    >
      @if (selected(); as key) {
        <dl class="details">
          <div>
            <dt>Integração</dt>
            <dd>{{ key.nome }}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{{ key.status }}</dd>
          </div>
          <div>
            <dt>Identificador</dt>
            <dd class="mono">{{ key.identificador }}</dd>
          </div>
          <div>
            <dt>Prefixo</dt>
            <dd class="mono">{{ key.prefixo }}</dd>
          </div>
          <div>
            <dt>Escopo</dt>
            <dd>{{ scopeLabel(key.escopo) }}</dd>
          </div>
          <div>
            <dt>Limite</dt>
            <dd>
              {{
                key.requisicoesPorMinuto ? key.requisicoesPorMinuto + '/min' : 'Padrão do servidor'
              }}
            </dd>
          </div>
          <div>
            <dt>Último uso</dt>
            <dd>
              {{
                key.ultimoUsoEm
                  ? (key.ultimoUsoEm | date: 'dd/MM/yyyy HH:mm:ss')
                  : 'Nunca utilizada'
              }}
            </dd>
          </div>
        </dl>
        <section class="integration-policy" aria-labelledby="credential-policy-title">
          <div>
            <strong id="credential-policy-title">Política de Segurança</strong>
            @if (policyBinding(); as binding) {
              <span
                >{{ binding.politicaNome }} · v{{ binding.versao }} ·
                {{ binding.utilizaPoliticaGlobal ? 'GLOBAL' : 'INTEGRAÇÃO' }}</span
              >
            } @else {
              <span>Carregando política aplicável…</span>
            }
          </div>
          <label for="credential-policy-select">Política aplicável</label>
          <select
            id="credential-policy-select"
            class="control"
            [(ngModel)]="selectedPolicyId"
            [disabled]="policySaving()"
          >
            <option value="">Usar política global padrão</option>
            @for (policy of availablePolicies(); track policy.id) {
              <option [value]="policy.id">{{ policy.nome }} · v{{ policy.versao }}</option>
            }
          </select>
          @if (policyError()) {
            <p role="alert">{{ policyError() }}</p>
          }
          <button
            class="btn btn-secondary"
            type="button"
            (click)="savePolicyBinding(key.id)"
            [disabled]="policySaving()"
          >
            <i [class]="policySaving() ? 'pi pi-spin pi-spinner' : 'pi pi-link'"></i>Atualizar
            política
          </button>
        </section>
      }
    </p-dialog>
  `,
  styles: [
    `
      .credential-note {
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        margin-bottom: 1rem;
        padding: 0.85rem 1rem;
        color: var(--muted);
        background: #101722;
        border: 1px solid #263c5d;
        border-radius: var(--radius);
        font-size: 0.8rem;
      }
      .credential-note > i {
        margin-top: 0.15rem;
        color: var(--ds-acid-lime);
      }
      .credential-note div {
        display: grid;
        gap: 0.15rem;
      }
      .credential-note strong {
        color: var(--text);
      }
      .filters .control {
        min-width: 9.5rem;
      }
      .search-field {
        position: relative;
      }
      .search-field i {
        position: absolute;
        left: 0.75rem;
        top: 50%;
        transform: translateY(-50%);
        color: var(--muted);
        pointer-events: none;
      }
      .search-field input {
        padding-left: 2.2rem;
        padding-right: 0.75rem;
      }
      .key-prefix {
        color: var(--ds-acid-lime);
        font-size: 0.78rem;
      }
      .scope {
        white-space: nowrap;
        color: #a9c6ff;
        font-size: 0.75rem;
        font-weight: 650;
      }
      .key-status {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.7rem;
        font-weight: 750;
      }
      .key-status.ativa {
        color: var(--success);
      }
      .key-status.expirada {
        color: var(--warning);
      }
      .key-status.revogada {
        color: var(--danger);
      }
      .row-actions {
        display: flex;
        gap: 0.35rem;
      }
      .data-table td {
        white-space: nowrap;
      }
      .data-table td:first-child {
        min-width: 12rem;
        white-space: normal;
      }
      .data-table td:last-child {
        position: sticky;
        right: 0;
        background: var(--surface);
        box-shadow: -8px 0 12px rgba(3, 4, 7, 0.45);
      }
      .dialog-form {
        display: grid;
        gap: 1rem;
        padding-top: 0.25rem;
      }
      .dialog-form .control {
        width: 100%;
        padding-right: 0.75rem;
        font-size: 1rem;
      }
      .textarea {
        height: auto;
        padding: 0.7rem;
        resize: vertical;
      }
      .field label span {
        font-weight: 400;
        color: var(--muted-2);
      }
      .field small {
        color: var(--muted-2);
        font-size: 0.72rem;
      }
      .dialog-actions {
        display: flex;
        justify-content: flex-end;
        gap: 0.5rem;
        padding-top: 0.5rem;
      }
      .form-error {
        margin: 0;
        padding: 0.65rem 0.75rem;
        color: #ff9aa4;
        background: #251318;
        border: 1px solid #59303a;
        border-radius: 5px;
        font-size: 0.8rem;
      }
      .secret-dialog {
        text-align: center;
      }
      .success-mark {
        width: 2.75rem;
        height: 2.75rem;
        display: grid;
        place-items: center;
        margin: 0 auto 0.75rem;
        color: #071108;
        background: var(--ds-acid-lime);
        border-radius: 50%;
      }
      .secret-dialog h2 {
        margin-bottom: 0.35rem;
      }
      .secret-dialog p {
        max-width: 52ch;
        margin: 0 auto 1rem;
        color: var(--muted);
        font-size: 0.84rem;
      }
      .secret-dialog label {
        display: block;
        margin-bottom: 0.4rem;
        color: var(--muted);
        font-size: 0.75rem;
        text-align: left;
      }
      .secret-value {
        width: 100%;
        padding: 0.75rem;
        color: var(--text);
        background: #090d12;
        border: 1px solid var(--line);
        border-radius: 5px;
        resize: none;
        overflow-wrap: anywhere;
        font-size: 0.78rem;
      }
      .copy-button {
        width: 100%;
        margin: 0.65rem 0 0.5rem;
      }
      .secret-dialog > .btn-secondary {
        width: 100%;
      }
      .details {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
        margin: 0;
      }
      .details div {
        min-width: 0;
      }
      .details dt {
        margin-bottom: 0.25rem;
        color: var(--muted);
        font-size: 0.72rem;
      }
      .details dd {
        margin: 0;
        overflow-wrap: anywhere;
        font-size: 0.84rem;
      }
      .integration-policy {
        display: grid;
        gap: 0.65rem;
        margin-top: 1rem;
        padding: 0.85rem;
        background: var(--surface-subtle);
        border: 1px solid var(--line);
        border-radius: var(--radius-sm);
      }
      .integration-policy > div {
        display: grid;
        gap: 0.2rem;
      }
      .integration-policy > div span,
      .integration-policy label {
        color: var(--muted);
        font-size: 0.72rem;
      }
      .integration-policy .control {
        width: 100%;
      }
      .integration-policy p {
        margin: 0;
        color: var(--danger);
        font-size: 0.72rem;
      }
      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }
      @media (max-width: 900px) {
        .panel-head {
          align-items: flex-start;
          flex-direction: column;
        }
        .filters {
          width: 100%;
        }
        .filters label,
        .filters .control {
          flex: 1;
          min-width: 0;
        }
        .data-table td:last-child {
          position: static;
          box-shadow: none;
        }
      }
      @media (max-width: 600px) {
        .page-head {
          align-items: stretch;
          flex-direction: column;
        }
        .page-head .btn {
          width: 100%;
        }
        .filters {
          display: grid;
          grid-template-columns: 1fr 1fr;
        }
        .filters label {
          grid-column: 1/-1;
        }
        .details {
          grid-template-columns: 1fr;
        }
        .dialog-actions {
          flex-direction: column-reverse;
        }
        .dialog-actions .btn {
          width: 100%;
        }
      }
    `,
  ],
})
export class ApiKeysPage implements OnDestroy {
  private readonly service = inject(ApiKeyService);
  private readonly policyService = inject(PolicyService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly messages = inject(MessageService);

  readonly data = signal<PagedResponse<ApiKeyDto> | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly saving = signal(false);
  readonly formError = signal('');
  readonly page = signal(0);
  search = '';
  status: ApiKeyStatus | '' = '';
  readonly createVisible = signal(false);
  readonly secretVisible = signal(false);
  readonly detailsVisible = signal(false);
  readonly secret = signal('');
  readonly secretName = signal('');
  readonly copied = signal(false);
  readonly selected = signal<ApiKeyDto | null>(null);
  readonly policyBinding = signal<IntegrationPolicyBindingDto | null>(null);
  readonly availablePolicies = signal<SecurityPolicyDto[]>([]);
  readonly policySaving = signal(false);
  readonly policyError = signal('');
  selectedPolicyId = '';
  form: ApiKeyForm = this.blankForm();

  constructor() {
    this.load();
  }
  ngOnDestroy(): void {
    this.clearSecret();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.service
      .list({ page: this.page(), size: 20, nome: this.search, status: this.status })
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(error.detail ?? 'Não foi possível carregar as API Keys.');
          this.loading.set(false);
        },
      });
  }
  applyFilters(): void {
    this.page.set(0);
    this.load();
  }
  previous(): void {
    if (this.page() > 0) {
      this.page.update((value) => value - 1);
      this.load();
    }
  }
  next(): void {
    if (!this.data()?.last) {
      this.page.update((value) => value + 1);
      this.load();
    }
  }
  scopeLabel(scope: string): string {
    return scope === 'DOCUMENT_ANALYZE' || scope === 'PUBLIC_ANALYSIS'
      ? 'Analisar documentos'
      : scope;
  }
  openCreate(): void {
    this.resetForm();
    this.createVisible.set(true);
  }
  resetForm(): void {
    if (!this.saving()) {
      this.form = this.blankForm();
      this.formError.set('');
    }
  }
  create(): void {
    if (!this.form.nome.trim() || this.saving()) return;
    const expires = this.form.expiraEm ? new Date(this.form.expiraEm) : null;
    if (expires && (Number.isNaN(expires.getTime()) || expires <= new Date())) {
      this.formError.set('A expiração deve ser uma data futura.');
      return;
    }
    this.saving.set(true);
    this.formError.set('');
    const request = this.buildCreateRequest(expires);
    this.service
      .create(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (created) => {
          this.createVisible.set(false);
          this.presentSecret(created.apiKey.nome, created.token);
          this.load();
        },
        error: (error) => this.formError.set(error.detail ?? 'Não foi possível criar a API Key.'),
      });
  }
  showDetails(key: ApiKeyDto): void {
    this.selected.set(key);
    this.policyBinding.set(null);
    this.policyError.set('');
    this.selectedPolicyId = '';
    this.detailsVisible.set(true);
    forkJoin({
      key: this.service.get(key.id),
      binding: this.policyService.binding(key.id),
      policies: this.policyService.list(),
    }).subscribe({
      next: ({ key: value, binding, policies }) => {
        this.selected.set(value);
        this.policyBinding.set(binding);
        this.availablePolicies.set(
          policies.filter((policy) => policy.tipo === 'INTEGRACAO' && policy.status === 'ATIVA'),
        );
        this.selectedPolicyId = binding.utilizaPoliticaGlobal ? '' : binding.politicaId;
      },
      error: (error) =>
        this.messages.add({
          severity: 'error',
          summary: 'Falha ao consultar credencial',
          detail: error.detail,
        }),
    });
  }
  savePolicyBinding(integrationId: string): void {
    if (this.policySaving()) return;
    this.policySaving.set(true);
    this.policyError.set('');
    const operation = this.selectedPolicyId
      ? this.policyService.bind(integrationId, this.selectedPolicyId)
      : this.policyService.unbind(integrationId);
    operation.pipe(finalize(() => this.policySaving.set(false))).subscribe({
      next: (binding) => {
        this.policyBinding.set(binding);
        this.selectedPolicyId = binding.utilizaPoliticaGlobal ? '' : binding.politicaId;
      },
      error: (error) => this.policyError.set(error.detail ?? error.message),
    });
  }
  confirmRevoke(key: ApiKeyDto): void {
    this.confirmation.confirm({
      header: 'Revogar API Key',
      message: `Revogar “${key.nome}”? A integração perderá acesso imediatamente. Esta ação não pode ser desfeita.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Revogar acesso',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () =>
        this.service.revoke(key.id).subscribe({
          next: () => {
            this.messages.add({ severity: 'success', summary: 'API Key revogada' });
            this.load();
          },
          error: (error) =>
            this.messages.add({
              severity: 'error',
              summary: 'Não foi possível revogar',
              detail: error.detail,
            }),
        }),
    });
  }
  confirmRotate(key: ApiKeyDto): void {
    this.confirmation.confirm({
      header: 'Rotacionar API Key',
      message: `Gerar um novo token para “${key.nome}”? O token atual será revogado imediatamente.`,
      icon: 'pi pi-sync',
      acceptLabel: 'Rotacionar token',
      rejectLabel: 'Cancelar',
      accept: () =>
        this.service.rotate(key.id).subscribe({
          next: (created) => {
            this.presentSecret(created.apiKey.nome, created.token);
            this.load();
          },
          error: (error) =>
            this.messages.add({
              severity: 'error',
              summary: 'Não foi possível rotacionar',
              detail: error.detail,
            }),
        }),
    });
  }
  presentSecret(name: string, token: string): void {
    this.secretName.set(name);
    this.secret.set(token);
    this.copied.set(false);
    this.secretVisible.set(true);
  }
  async copySecret(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.secret());
      this.copied.set(true);
    } catch {
      this.messages.add({
        severity: 'warn',
        summary: 'Cópia bloqueada',
        detail: 'Selecione o token e copie manualmente.',
      });
    }
  }
  onSecretVisibility(visible: boolean): void {
    if (!visible) this.clearSecret();
    else this.secretVisible.set(true);
  }
  clearSecret(): void {
    this.secretVisible.set(false);
    this.secret.set('');
    this.secretName.set('');
    this.copied.set(false);
  }

  private blankForm(): ApiKeyForm {
    return {
      nome: '',
      descricao: '',
      expiraEm: null,
      escopo: 'DOCUMENT_ANALYZE',
      requisicoesPorMinuto: null,
    };
  }

  private buildCreateRequest(expiration: Date | null): CreateApiKeyRequest {
    return {
      nome: this.form.nome.trim(),
      descricao: this.form.descricao?.trim() || null,
      expiraEm: expiration?.toISOString() ?? null,
      escopo: 'DOCUMENT_ANALYZE',
      requisicoesPorMinuto: this.form.requisicoesPorMinuto || null,
    };
  }
}
