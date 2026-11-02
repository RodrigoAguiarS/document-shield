import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { DashboardDto } from '../models/api.models';
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  private base = inject(APP_CONFIG).apiBaseUrl;
  summary() {
    return this.http.get<DashboardDto>(`${this.base}/dashboard/resumo`);
  }
}
