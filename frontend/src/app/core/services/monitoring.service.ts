import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import {
  MonitoringCapacity,
  MonitoringErrors,
  MonitoringIntegration,
  MonitoringIntegrationList,
  MonitoringPeriod,
  MonitoringSeries,
  MonitoringSummary,
  UUID,
} from '../models/api.models';

export interface MonitoringQuery {
  periodo: MonitoringPeriod;
  dataInicio?: string;
  dataFim?: string;
  integracaoId?: UUID;
}

@Injectable({ providedIn: 'root' })
export class MonitoringService {
  private http = inject(HttpClient);
  private base = `${inject(APP_CONFIG).apiBaseUrl}/admin/monitoramento`;

  summary(query: MonitoringQuery) {
    return this.http.get<MonitoringSummary>(`${this.base}/resumo`, {
      params: this.params(query),
      withCredentials: true,
    });
  }
  timeSeries(query: MonitoringQuery) {
    return this.http.get<MonitoringSeries>(`${this.base}/serie-temporal`, {
      params: this.params(query),
      withCredentials: true,
    });
  }
  integrations(query: MonitoringQuery) {
    return this.http.get<MonitoringIntegrationList>(`${this.base}/integracoes`, {
      params: this.params(query),
      withCredentials: true,
    });
  }
  integration(id: UUID, query: MonitoringQuery) {
    return this.http.get<MonitoringIntegration>(`${this.base}/integracoes/${id}`, {
      params: this.params(query),
      withCredentials: true,
    });
  }
  errors(query: MonitoringQuery) {
    return this.http.get<MonitoringErrors>(`${this.base}/erros`, {
      params: this.params(query),
      withCredentials: true,
    });
  }
  capacity() {
    return this.http.get<MonitoringCapacity>(`${this.base}/capacidade`, { withCredentials: true });
  }

  private params(query: MonitoringQuery) {
    let params = new HttpParams().set('periodo', query.periodo);
    if (query.dataInicio) params = params.set('dataInicio', query.dataInicio);
    if (query.dataFim) params = params.set('dataFim', query.dataFim);
    if (query.integracaoId) params = params.set('integracaoId', query.integracaoId);
    return params;
  }
}
