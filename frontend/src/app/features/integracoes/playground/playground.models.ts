import { PublicAnalysisResponseDto } from '../../../core/models/api.models';

export type PlaygroundExecutionState =
  'AGUARDANDO' | 'ENVIANDO' | 'PROCESSANDO' | 'CONCLUIDO' | 'ERRO';

export interface PlaygroundRequest {
  apiKey: string;
  file: File;
}

export interface PlaygroundError {
  status: number;
  title: string;
  detail: string;
  type?: string;
  correlationId?: string;
  retryAfter?: string;
}

export interface PlaygroundResult {
  status: number;
  clientDurationMs: number;
  response: PublicAnalysisResponseDto;
}
