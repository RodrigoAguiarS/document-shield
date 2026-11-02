import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Confirmation, ConfirmationService, MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { ApiKeyDto } from '../../core/models/api.models';
import { ApiKeyService } from '../../core/services/api-key.service';
import { ApiKeysPage } from './api-keys-page';

describe('ApiKeysPage', () => {
  let fixture: ComponentFixture<ApiKeysPage>;
  let page: ApiKeysPage;
  let service: jasmine.SpyObj<ApiKeyService>;
  const key: ApiKeyDto = {
    id: 'key-1',
    nome: 'ERP',
    descricao: 'Financeiro',
    identificador: 'a'.repeat(32),
    prefixo: 'dsk_aaaaaaaa…',
    escopo: 'DOCUMENT_ANALYZE',
    status: 'ATIVA',
    criadaEm: '2026-11-02T12:00:00Z',
    expiraEm: null,
    ultimoUsoEm: null,
    revogadaEm: null,
    requisicoesPorMinuto: 30,
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<ApiKeyService>('ApiKeyService', [
      'list',
      'get',
      'create',
      'revoke',
      'rotate',
    ]);
    service.list.and.returnValue(
      of({
        content: [key],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      }),
    );
    service.get.and.returnValue(of(key));
    service.revoke.and.returnValue(of({ ...key, status: 'REVOGADA' }));
    service.create.and.returnValue(of({ apiKey: key, token: 'dsk_token_criado' }));
    service.rotate.and.returnValue(
      of({ apiKey: { ...key, id: 'key-2' }, token: 'dsk_token_rotacionado' }),
    );
    await TestBed.configureTestingModule({
      imports: [ApiKeysPage],
      providers: [
        provideNoopAnimations(),
        MessageService,
        { provide: ApiKeyService, useValue: service },
        {
          provide: ConfirmationService,
          useValue: { confirm: (options: Confirmation) => options.accept?.() },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ApiKeysPage);
    page = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the real listing and applies filters', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ERP');
    page.search = 'ERP';
    page.status = 'ATIVA';
    page.applyFilters();
    expect(service.list).toHaveBeenCalledWith({ page: 0, size: 20, nome: 'ERP', status: 'ATIVA' });
  });

  it('validates expiration and presents the created token only until dismissal', () => {
    page.openCreate();
    page.form.nome = 'ERP novo';
    page.form.expiraEm = '2020-01-01T00:00';
    page.create();
    expect(page.formError()).toContain('futura');
    expect(service.create).not.toHaveBeenCalled();
    page.form.expiraEm = null;
    page.create();
    expect(page.secret()).toBe('dsk_token_criado');
    expect(page.secretVisible()).toBeTrue();
    page.clearSecret();
    expect(page.secret()).toBe('');
    expect(page.secretVisible()).toBeFalse();
  });

  it('revokes and rotates after confirmation', () => {
    page.confirmRevoke(key);
    expect(service.revoke).toHaveBeenCalledWith('key-1');
    page.confirmRotate(key);
    expect(service.rotate).toHaveBeenCalledWith('key-1');
    expect(page.secret()).toBe('dsk_token_rotacionado');
  });

  it('shows API loading errors', () => {
    service.list.and.returnValue(throwError(() => ({ detail: 'Sessão expirada' })));
    page.load();
    expect(page.error()).toBe('Sessão expirada');
    expect(page.loading()).toBeFalse();
  });
});
