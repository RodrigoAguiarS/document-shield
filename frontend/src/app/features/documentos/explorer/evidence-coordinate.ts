import { OccurrenceDto } from '../../../core/models/api.models';

export interface ViewportEvidenceRect {
  left: number;
  top: number;
  width: number;
  height: number;
  outsidePage: boolean;
}

export function evidenceRect(
  occurrence: OccurrenceDto,
  viewportWidth: number,
  viewportHeight: number,
  additionalRotation: number,
): ViewportEvidenceRect | null {
  if (
    occurrence.localizacaoConfiavel !== true ||
    occurrence.espacoCoordenadas !== 'PDFBOX_ROTATED_CROP_TOP_LEFT' ||
    occurrence.posicaoX === null ||
    occurrence.posicaoY === null ||
    occurrence.largura === null ||
    occurrence.altura === null ||
    occurrence.larguraPagina === null ||
    occurrence.alturaPagina === null ||
    occurrence.larguraPagina <= 0 ||
    occurrence.alturaPagina <= 0
  ) {
    return null;
  }

  const pageWidth = occurrence.larguraPagina;
  const pageHeight = occurrence.alturaPagina;
  const x = occurrence.posicaoX;
  const y = occurrence.posicaoY;
  const width = occurrence.largura;
  const height = occurrence.altura;
  const rotation = ((additionalRotation % 360) + 360) % 360;

  let rotatedX = x;
  let rotatedY = y;
  let rotatedWidth = width;
  let rotatedHeight = height;
  let rotatedPageWidth = pageWidth;
  let rotatedPageHeight = pageHeight;

  if (rotation === 90) {
    rotatedX = pageHeight - y - height;
    rotatedY = x;
    rotatedWidth = height;
    rotatedHeight = width;
    rotatedPageWidth = pageHeight;
    rotatedPageHeight = pageWidth;
  } else if (rotation === 180) {
    rotatedX = pageWidth - x - width;
    rotatedY = pageHeight - y - height;
  } else if (rotation === 270) {
    rotatedX = y;
    rotatedY = pageWidth - x - width;
    rotatedWidth = height;
    rotatedHeight = width;
    rotatedPageWidth = pageHeight;
    rotatedPageHeight = pageWidth;
  } else if (rotation !== 0) {
    return null;
  }

  const outsidePage =
    rotatedX < 0 ||
    rotatedY < 0 ||
    rotatedX + rotatedWidth > rotatedPageWidth ||
    rotatedY + rotatedHeight > rotatedPageHeight;

  return {
    left: (rotatedX / rotatedPageWidth) * viewportWidth,
    top: (rotatedY / rotatedPageHeight) * viewportHeight,
    width: (rotatedWidth / rotatedPageWidth) * viewportWidth,
    height: (rotatedHeight / rotatedPageHeight) * viewportHeight,
    outsidePage,
  };
}
