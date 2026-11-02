import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { EvidenceDto, OccurrenceDto, UUID } from '../models/api.models';
@Injectable({ providedIn: 'root' })
export class OccurrenceService {
  private http = inject(HttpClient);
  private base = inject(APP_CONFIG).apiBaseUrl;
  get(id: UUID) {
    return this.http.get<OccurrenceDto>(`${this.base}/ocorrencias/${id}`);
  }
  evidence(id: UUID) {
    return this.http.get<EvidenceDto[]>(`${this.base}/ocorrencias/${id}/evidencias`);
  }
}
