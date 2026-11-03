import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import {
  IntegrationPolicyBindingDto,
  PolicyAuditDto,
  PolicyEvaluationDto,
  SavePolicyRequest,
  SecurityPolicyDto,
  UUID,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PolicyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiBaseUrl}/admin/politicas`;
  private readonly integrationsBase = `${inject(APP_CONFIG).apiBaseUrl}/admin/integracoes`;

  list() {
    return this.http.get<SecurityPolicyDto[]>(this.base, { withCredentials: true });
  }
  get(id: UUID) {
    return this.http.get<SecurityPolicyDto>(`${this.base}/${id}`, { withCredentials: true });
  }
  create(request: SavePolicyRequest) {
    return this.http.post<SecurityPolicyDto>(this.base, request, { withCredentials: true });
  }
  update(id: UUID, request: SavePolicyRequest) {
    return this.http.put<SecurityPolicyDto>(`${this.base}/${id}`, request, {
      withCredentials: true,
    });
  }
  activate(id: UUID) {
    return this.http.post<SecurityPolicyDto>(`${this.base}/${id}/ativar`, null, {
      withCredentials: true,
    });
  }
  archive(id: UUID) {
    return this.http.post<SecurityPolicyDto>(`${this.base}/${id}/arquivar`, null, {
      withCredentials: true,
    });
  }
  versions(id: UUID) {
    return this.http.get<SecurityPolicyDto[]>(`${this.base}/${id}/versoes`, {
      withCredentials: true,
    });
  }
  simulate(id: UUID, score: number) {
    return this.http.post<PolicyEvaluationDto>(
      `${this.base}/${id}/simular`,
      { pontuacao: score },
      { withCredentials: true },
    );
  }
  audits() {
    return this.http.get<PolicyAuditDto[]>(`${this.base}/auditoria`, { withCredentials: true });
  }
  binding(integrationId: UUID) {
    return this.http.get<IntegrationPolicyBindingDto>(
      `${this.integrationsBase}/${integrationId}/politica`,
      { withCredentials: true },
    );
  }
  bind(integrationId: UUID, policyId: UUID) {
    return this.http.put<IntegrationPolicyBindingDto>(
      `${this.integrationsBase}/${integrationId}/politica`,
      { politicaId: policyId },
      { withCredentials: true },
    );
  }
  unbind(integrationId: UUID) {
    return this.http.delete<IntegrationPolicyBindingDto>(
      `${this.integrationsBase}/${integrationId}/politica`,
      { withCredentials: true },
    );
  }
}
