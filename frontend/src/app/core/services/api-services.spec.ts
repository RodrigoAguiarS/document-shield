import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DocumentService } from './document.service';
import { AnalysisService } from './analysis.service';
describe('typed API services', () => {
  let http: HttpTestingController;
  let documents: DocumentService;
  let analyses: AnalysisService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    documents = TestBed.inject(DocumentService);
    analyses = TestBed.inject(AnalysisService);
  });
  afterEach(() => http.verify());
  it('uses backend pagination and status filter for documents', () => {
    documents.list({ page: 2, size: 15, sort: 'dataUpload,asc' }, 'PRONTO').subscribe();
    const req = http.expectOne((r) => r.url === '/api/v1/documentos');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('15');
    expect(req.request.params.get('status')).toBe('PRONTO');
    req.flush({
      content: [],
      page: 2,
      size: 15,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
    });
  });
  it('sends the PDF as multipart data', () => {
    const file = new File(['%PDF-1.7'], 'doc.pdf', { type: 'application/pdf' });
    documents.upload(file).subscribe();
    const req = http.expectOne('/api/v1/documentos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('arquivo')).toEqual(file);
    req.flush({
      documentoId: 'd',
      analiseId: 'a',
      statusDocumento: 'VALIDANDO',
      statusAnalise: 'PENDENTE',
      consultaAnalise: '/api/v1/analises/a',
    });
  });
  it('uses real analysis filters and reanalysis endpoint', () => {
    analyses.list({ page: 0, size: 20 }, 'CONCLUIDA', 'ALTO').subscribe();
    let req = http.expectOne((r) => r.url === '/api/v1/analises');
    expect(req.request.params.get('status')).toBe('CONCLUIDA');
    expect(req.request.params.get('nivelRisco')).toBe('ALTO');
    req.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });
    documents.reanalyze('doc-id').subscribe();
    req = http.expectOne('/api/v1/documentos/doc-id/reanalisar');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
