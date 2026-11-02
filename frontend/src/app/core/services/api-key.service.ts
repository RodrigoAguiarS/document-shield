import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import {
  ApiKeyDto,
  ApiKeyStatus,
  CreateApiKeyRequest,
  CreatedApiKeyResponse,
  PagedResponse,
  UUID,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiKeyService {
  private http = inject(HttpClient);
  private base = `${inject(APP_CONFIG).apiBaseUrl}/admin/api-keys`;

  list(options: { page?: number; size?: number; nome?: string; status?: ApiKeyStatus | '' } = {}) {
    let params = new HttpParams().set('page', options.page ?? 0).set('size', options.size ?? 20);
    if (options.nome?.trim()) params = params.set('nome', options.nome.trim());
    if (options.status) params = params.set('status', options.status);
    return this.http.get<PagedResponse<ApiKeyDto>>(this.base, { params, withCredentials: true });
  }

  get(id: UUID) {
    return this.http.get<ApiKeyDto>(`${this.base}/${id}`, { withCredentials: true });
  }
  create(request: CreateApiKeyRequest) {
    return this.http.post<CreatedApiKeyResponse>(this.base, request, { withCredentials: true });
  }
  revoke(id: UUID) {
    return this.http.post<ApiKeyDto>(`${this.base}/${id}/revogar`, null, { withCredentials: true });
  }
  rotate(id: UUID) {
    return this.http.post<CreatedApiKeyResponse>(`${this.base}/${id}/rotacionar`, null, {
      withCredentials: true,
    });
  }
}
