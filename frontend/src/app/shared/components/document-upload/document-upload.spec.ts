import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentUpload } from './document-upload';
describe('DocumentUpload', () => {
  let component: DocumentUpload;
  let uploadDocument: (file: File) => void;
  const service = { upload: jasmine.createSpy('upload').and.returnValue(of()) };
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentUpload],
      providers: [
        { provide: DocumentService, useValue: service },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
        MessageService,
      ],
    }).compileComponents();
    component = TestBed.createComponent(DocumentUpload).componentInstance;
    uploadDocument = (component as unknown as { send: (file: File) => void }).send.bind(component);
    service.upload.calls.reset();
  });
  it('rejects non-PDF files before any request', () => {
    uploadDocument(new File(['x'], 'note.txt', { type: 'text/plain' }));
    expect(component.error()).toContain('PDF');
    expect(service.upload).not.toHaveBeenCalled();
  });
  it('rejects empty files before any request', () => {
    uploadDocument(new File([], 'empty.pdf', { type: 'application/pdf' }));
    expect(component.error()).toContain('vazio');
    expect(service.upload).not.toHaveBeenCalled();
  });
  it('accepts a valid PDF and starts upload progress', () => {
    uploadDocument(new File(['%PDF-1.7'], 'safe.pdf', { type: 'application/pdf' }));
    expect(service.upload).toHaveBeenCalled();
  });
});
