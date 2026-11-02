import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MonitoringService } from './monitoring.service';

describe('MonitoringService', () => {
  let service: MonitoringService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MonitoringService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('queries aggregate endpoints with the selected period', () => {
    service.summary({ periodo: '7D' }).subscribe();
    const summary = http.expectOne((r) => r.url === '/api/v1/admin/monitoramento/resumo');
    expect(summary.request.params.get('periodo')).toBe('7D');
    expect(summary.request.withCredentials).toBeTrue();
    summary.flush({});
    service
      .errors({
        periodo: 'CUSTOM',
        dataInicio: '2026-01-01T00:00:00Z',
        dataFim: '2026-01-02T00:00:00Z',
      })
      .subscribe();
    const errors = http.expectOne((r) => r.url === '/api/v1/admin/monitoramento/erros');
    expect(errors.request.params.get('dataInicio')).toBe('2026-01-01T00:00:00Z');
    expect(errors.request.params.get('dataFim')).toBe('2026-01-02T00:00:00Z');
    errors.flush({});
  });

  it('reads capacity without adding a public API credential', () => {
    service.capacity().subscribe();
    const request = http.expectOne('/api/v1/admin/monitoramento/capacidade');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({ aplicacao: 'UP' });
  });
});
