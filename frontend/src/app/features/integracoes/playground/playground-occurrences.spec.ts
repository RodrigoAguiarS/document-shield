import { TestBed } from '@angular/core/testing';
import { PublicOccurrenceDto } from '../../../core/models/api.models';
import { PlaygroundOccurrences } from './playground-occurrences';

describe('PlaygroundOccurrences', () => {
  it('renders hostile PDF-derived values as inert text', async () => {
    await TestBed.configureTestingModule({ imports: [PlaygroundOccurrences] }).compileComponents();
    const fixture = TestBed.createComponent(PlaygroundOccurrences);
    const hostile = '<img src=x onerror=alert(1)><script>alert(2)</script>';
    const occurrence: PublicOccurrenceDto = {
      id: '00000000-0000-0000-0000-000000000001',
      codigoRegra: 'PDF-TEXT-001',
      tipo: 'TEXTO_INVISIVEL',
      nivelRisco: 'ALTO',
      titulo: 'Conteúdo suspeito',
      descricao: hostile,
      conteudoSuspeito: hostile,
      conteudoNormalizado: null,
      numeroPagina: 1,
      posicaoX: null,
      posicaoY: null,
      largura: null,
      altura: null,
      fonte: null,
      tamanhoFonte: null,
      corTexto: null,
      opacidade: null,
      pontuacao: 75,
      evidencias: [
        {
          tipo: 'TRECHO_TEXTO',
          descricao: hostile,
          valorEncontrado: hostile,
          valorEsperado: null,
          detalhesTecnicos: hostile,
          numeroPagina: 1,
        },
      ],
    };
    fixture.componentRef.setInput('occurrences', [occurrence]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(hostile);
    expect(fixture.nativeElement.querySelector('script')).toBeNull();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
  });
});
