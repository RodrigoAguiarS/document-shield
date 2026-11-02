import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import {
  AnalysisDto,
  AnalysisStatus,
  OccurrenceDto,
  PageRequest,
  PagedResponse,
  RiskLevel,
  RuleExecutionDto,
  UUID,
} from '../models/api.models';
@Injectable({ providedIn: 'root' })
export class AnalysisService {
  private http = inject(HttpClient);
  private base = inject(APP_CONFIG).apiBaseUrl;
  list(req: PageRequest = {}, status?: AnalysisStatus, risk?: RiskLevel) {
    let p = new HttpParams()
      .set('page', req.page ?? 0)
      .set('size', req.size ?? 20)
      .set('sort', req.sort ?? 'dataInicio,desc');
    if (status) p = p.set('status', status);
    if (risk) p = p.set('nivelRisco', risk);
    return this.http.get<PagedResponse<AnalysisDto>>(`${this.base}/analises`, { params: p });
  }
  get(id: UUID) {
    return this.http.get<AnalysisDto>(`${this.base}/analises/${id}`);
  }
  occurrences(id: UUID, req: PageRequest = {}) {
    const p = new HttpParams()
      .set('page', req.page ?? 0)
      .set('size', req.size ?? 20)
      .set('sort', req.sort ?? 'dataIdentificacao,asc');
    return this.http.get<PagedResponse<OccurrenceDto>>(`${this.base}/analises/${id}/ocorrencias`, {
      params: p,
    });
  }
  executions(id: UUID) {
    return this.http.get<RuleExecutionDto[]>(`${this.base}/analises/${id}/execucoes`);
  }
}
