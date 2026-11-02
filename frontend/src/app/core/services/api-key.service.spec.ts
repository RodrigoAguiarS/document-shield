import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiKeyService } from './api-key.service';

describe('ApiKeyService', () => {
  let service: ApiKeyService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiKeyService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lists with real pagination and filters', () => {
    service.list({ page: 2, size: 10, nome: 'Financeiro', status: 'ATIVA' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/admin/api-keys');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('nome')).toBe('Financeiro');
    expect(req.request.params.get('status')).toBe('ATIVA');
    req.flush({
      content: [],
      page: 2,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
    });
  });

  it('uses protected mutation endpoints without sending a client-generated secret', () => {
    service.create({ nome: 'ERP', escopo: 'DOCUMENT_ANALYZE' }).subscribe();
    let req = http.expectOne('/api/v1/admin/api-keys');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.token).toBeUndefined();
    req.flush({ apiKey: { id: '1' }, token: 'dsk_secret' });
    service.rotate('1').subscribe();
    req = http.expectOne('/api/v1/admin/api-keys/1/rotacionar');
    expect(req.request.method).toBe('POST');
    req.flush({});
    service.revoke('1').subscribe();
    req = http.expectOne('/api/v1/admin/api-keys/1/revogar');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
