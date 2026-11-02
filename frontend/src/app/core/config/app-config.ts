import { InjectionToken } from '@angular/core';

export interface DocumentShieldConfig {
  apiBaseUrl: string;
  maxUploadBytes: number;
  pollingIntervalMs: number;
  maxPollingErrors: number;
  monitoringPollingMs: number;
}
export const APP_CONFIG = new InjectionToken<DocumentShieldConfig>('DOCUMENT_SHIELD_CONFIG', {
  providedIn: 'root',
  factory: () => ({
    apiBaseUrl: '/api/v1',
    maxUploadBytes: 20 * 1024 * 1024,
    pollingIntervalMs: 2500,
    maxPollingErrors: 4,
    monitoringPollingMs: 30000,
  }),
});
