import { OccurrenceDto } from '../../../core/models/api.models';
import { evidenceRect } from './evidence-coordinate';

describe('evidenceRect', () => {
  const occurrence = {
    localizacaoConfiavel: true,
    espacoCoordenadas: 'PDFBOX_ROTATED_CROP_TOP_LEFT',
    posicaoX: 10,
    posicaoY: 20,
    largura: 30,
    altura: 40,
    larguraPagina: 100,
    alturaPagina: 200,
  } as OccurrenceDto;

  it('escala o retângulo sem conversão fixa', () => {
    expect(evidenceRect(occurrence, 200, 400, 0)).toEqual({
      left: 20,
      top: 40,
      width: 60,
      height: 80,
      outsidePage: false,
    });
  });

  it('aplica rotação adicional de 90 graus', () => {
    expect(evidenceRect(occurrence, 400, 200, 90)).toEqual({
      left: 280,
      top: 20,
      width: 80,
      height: 60,
      outsidePage: false,
    });
  });

  it('aplica rotações adicionais de 180 e 270 graus', () => {
    expect(evidenceRect(occurrence, 200, 400, 180)).toEqual({
      left: 120,
      top: 280,
      width: 60,
      height: 80,
      outsidePage: false,
    });
    expect(evidenceRect(occurrence, 400, 200, 270)).toEqual({
      left: 40,
      top: 120,
      width: 80,
      height: 60,
      outsidePage: false,
    });
  });

  it('não inventa localização quando o contrato é incompleto', () => {
    expect(evidenceRect({ ...occurrence, posicaoX: null }, 200, 400, 0)).toBeNull();
    expect(evidenceRect({ ...occurrence, localizacaoConfiavel: false }, 200, 400, 0)).toBeNull();
  });

  it('preserva a informação de caixa fora da página', () => {
    expect(evidenceRect({ ...occurrence, posicaoX: 110 }, 200, 400, 0)?.outsidePage).toBeTrue();
  });
});
