import { TestBed } from '@angular/core/testing';
import { EvidencePanel } from './evidence-panel';
describe('EvidencePanel', () => {
  it('renders hostile content as text and never as HTML', async () => {
    await TestBed.configureTestingModule({ imports: [EvidencePanel] }).compileComponents();
    const f = TestBed.createComponent(EvidencePanel);
    f.componentRef.setInput('items', [
      {
        id: '1',
        ocorrenciaId: '2',
        tipo: 'TRECHO_TEXTO',
        descricao: 'Trecho',
        valorEncontrado: '<img src=x onerror=alert(1)>',
        valorEsperado: null,
        detalhesTecnicos: '<script>alert(1)</script>',
        numeroPagina: 1,
        dataRegistro: '2026-01-01T00:00:00Z',
      },
    ]);
    f.detectChanges();
    expect(f.nativeElement.querySelector('img')).toBeNull();
    expect(f.nativeElement.querySelector('script')).toBeNull();
    expect(f.nativeElement.textContent).toContain('<img src=x');
  });
});
