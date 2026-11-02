import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import {
  AnalysisDto,
  DocumentDto,
  DocumentStatus,
  PageRequest,
  PagedResponse,
  UploadResponse,
  UUID,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private base = inject(APP_CONFIG).apiBaseUrl;
  list(req: PageRequest = {}, status?: DocumentStatus) {
    let p = new HttpParams()
      .set('page', req.page ?? 0)
      .set('size', req.size ?? 20)
      .set('sort', req.sort ?? 'dataUpload,desc');
    if (status) p = p.set('status', status);
    return this.http.get<PagedResponse<DocumentDto>>(`${this.base}/documentos`, { params: p });
  }
  get(id: UUID) {
    return this.http.get<DocumentDto>(`${this.base}/documentos/${id}`);
  }
  upload(file: File): Observable<HttpEvent<UploadResponse>> {
    const data = new FormData();
    data.append('arquivo', file, file.name);
    return this.http.post<UploadResponse>(`${this.base}/documentos`, data, {
      observe: 'events',
      reportProgress: true,
    });
  }
  history(id: UUID, req: PageRequest = {}) {
    const p = new HttpParams()
      .set('page', req.page ?? 0)
      .set('size', req.size ?? 20)
      .set('sort', req.sort ?? 'dataInicio,desc');
    return this.http.get<PagedResponse<AnalysisDto>>(`${this.base}/documentos/${id}/analises`, {
      params: p,
    });
  }
  reanalyze(id: UUID) {
    return this.http.post<UploadResponse>(`${this.base}/documentos/${id}/reanalisar`, {});
  }
  delete(id: UUID) {
    return this.http.delete<void>(`${this.base}/documentos/${id}`);
  }
  download(id: UUID) {
    return this.http.get(`${this.base}/documentos/${id}/arquivo`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
