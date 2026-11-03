import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PolicyService } from './policy.service';

describe('PolicyService', () => {
  let service: PolicyService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PolicyService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('uses protected administrative endpoints for lifecycle operations', () => {
    service.list().subscribe();
    let request = http.expectOne('/api/v1/admin/politicas');
    expect(request.request.method).toBe('GET');
    request.flush([]);

    service.simulate('policy-1', 45).subscribe();
    request = http.expectOne('/api/v1/admin/politicas/policy-1/simular');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ pontuacao: 45 });
    request.flush({});

    service.bind('integration-1', 'policy-1').subscribe();
    request = http.expectOne('/api/v1/admin/integracoes/integration-1/politica');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ politicaId: 'policy-1' });
    request.flush({});
  });
});
