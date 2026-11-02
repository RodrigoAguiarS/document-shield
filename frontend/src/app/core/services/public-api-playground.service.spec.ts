import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PublicApiPlaygroundService } from './public-api-playground.service';

describe('PublicApiPlaygroundService', () => {
  let service: PublicApiPlaygroundService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PublicApiPlaygroundService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the PDF as multipart with the API key only in the Authorization header', () => {
    const file = new File(['%PDF-1.7'], 'contrato.pdf', { type: 'application/pdf' });

    service.analyze(file, 'dsk_test_secret').subscribe();

    const request = http.expectOne('/api/v1/public/analises');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Authorization')).toBe('Bearer dsk_test_secret');
    expect(request.request.headers.has('Content-Type')).toBeFalse();
    expect(request.request.body instanceof FormData).toBeTrue();
    const uploaded = (request.request.body as FormData).get('arquivo') as File;
    expect(uploaded.name).toBe(file.name);
    expect(uploaded.type).toBe(file.type);
    expect(uploaded.size).toBe(file.size);
    request.flush({});
  });
});
