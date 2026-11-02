import { HttpClient, HttpEvent, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { PublicAnalysisResponseDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PublicApiPlaygroundService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  readonly endpoint = `${this.config.apiBaseUrl}/public/analises`;

  analyze(file: File, apiKey: string): Observable<HttpEvent<PublicAnalysisResponseDto>> {
    const formData = new FormData();
    formData.append('arquivo', file, file.name);

    return this.http.post<PublicAnalysisResponseDto>(this.endpoint, formData, {
      headers: new HttpHeaders({ Authorization: `Bearer ${apiKey}` }),
      observe: 'events',
      reportProgress: true,
    });
  }
}
