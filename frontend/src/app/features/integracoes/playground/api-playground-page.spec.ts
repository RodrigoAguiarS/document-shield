import { HttpEvent, HttpEventType } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { PublicAnalysisResponseDto } from '../../../core/models/api.models';
import { PublicApiPlaygroundService } from '../../../core/services/public-api-playground.service';
import { ApiPlaygroundPage } from './api-playground-page';

describe('ApiPlaygroundPage', () => {
  it('distinguishes upload from processing and cancels the active request', async () => {
    const events = new Subject<HttpEvent<PublicAnalysisResponseDto>>();
    const service = {
      endpoint: '/api/v1/public/analises',
      analyze: jasmine.createSpy('analyze').and.returnValue(events),
    };
    await TestBed.configureTestingModule({
      imports: [ApiPlaygroundPage],
      providers: [provideRouter([]), { provide: PublicApiPlaygroundService, useValue: service }],
    }).compileComponents();
    const fixture = TestBed.createComponent(ApiPlaygroundPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const file = new File(['%PDF-1.7'], 'documento.pdf', { type: 'application/pdf' });

    component.execute({ file, apiKey: 'dsk_secret' });
    expect(component.state()).toBe('ENVIANDO');
    expect(events.observed).toBeTrue();

    events.next({ type: HttpEventType.UploadProgress, loaded: file.size, total: file.size });
    expect(component.uploadProgress()).toBe(100);
    expect(component.state()).toBe('PROCESSANDO');

    component.cancel();
    expect(events.observed).toBeFalse();
    expect(component.state()).toBe('AGUARDANDO');
    expect(component.clearSecretVersion()).toBe(1);
    expect(component.cancellationMessage()).toContain('cancelada no navegador');
  });
});
