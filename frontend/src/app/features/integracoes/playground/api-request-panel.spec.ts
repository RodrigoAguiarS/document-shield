import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApiRequestPanel } from './api-request-panel';

describe('ApiRequestPanel', () => {
  async function create(state = 'AGUARDANDO' as const) {
    await TestBed.configureTestingModule({
      imports: [ApiRequestPanel],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(ApiRequestPanel);
    fixture.componentRef.setInput('endpoint', '/api/v1/public/analises');
    fixture.componentRef.setInput('state', state);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('emits only a valid PDF and a trimmed API key', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;
    const file = new File(['%PDF-1.7'], 'documento.pdf', { type: 'application/pdf' });
    const requests = jasmine.createSpy('executeRequest');
    component.executeRequest.subscribe(requests);
    component.apiKey = '  dsk_secret  ';
    component.selectedFile = file;

    component.submit();

    expect(requests).toHaveBeenCalledOnceWith({ apiKey: 'dsk_secret', file });
  });

  it('rejects non-PDF and oversized files before executing', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;
    const requests = jasmine.createSpy('executeRequest');
    component.executeRequest.subscribe(requests);
    component.apiKey = 'dsk_secret';

    component.selectedFile = new File(['text'], 'readme.txt', { type: 'text/plain' });
    component.submit();
    expect(component.validationError()).toContain('somente arquivos PDF');

    component.selectedFile = new File([new ArrayBuffer(20 * 1024 * 1024 + 1)], 'large.pdf', {
      type: 'application/pdf',
    });
    component.submit();
    expect(component.validationError()).toContain('20 MB');
    expect(requests).not.toHaveBeenCalled();
  });

  it('clears the API key when the execution finishes', async () => {
    const fixture = await create();
    const component = fixture.componentInstance;
    component.apiKey = 'dsk_secret';
    component.showApiKey.set(true);

    fixture.componentRef.setInput('clearSecretVersion', 1);
    fixture.detectChanges();

    expect(component.apiKey).toBe('');
    expect(component.showApiKey()).toBeFalse();
  });
});
