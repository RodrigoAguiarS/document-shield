import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { DetectionRuleDto, UUID } from '../models/api.models';
@Injectable({ providedIn: 'root' })
export class RuleService {
  private http = inject(HttpClient);
  private base = inject(APP_CONFIG).apiBaseUrl;
  list() {
    return this.http.get<DetectionRuleDto[]>(`${this.base}/regras`);
  }
  get(id: UUID) {
    return this.http.get<DetectionRuleDto>(`${this.base}/regras/${id}`);
  }
}
