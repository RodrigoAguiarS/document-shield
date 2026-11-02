export type UUID = string;
export type DocumentStatus = 'RECEBIDO' | 'VALIDANDO' | 'PRONTO' | 'REJEITADO' | 'ERRO';
export type AnalysisStatus = 'PENDENTE' | 'EXECUTANDO' | 'CONCLUIDA' | 'FALHOU';
export type RiskLevel = 'INFORMATIVO' | 'BAIXO' | 'MEDIO' | 'ALTO' | 'CRITICO';
export type OccurrenceType =
  | 'TEXTO_INVISIVEL'
  | 'FONTE_MINUSCULA'
  | 'TRANSPARENCIA_SUSPEITA'
  | 'TEXTO_SOBREPOSTO'
  | 'TEXTO_FORA_DA_PAGINA'
  | 'CAMADA_OCULTA'
  | 'UNICODE_SUSPEITO'
  | 'METADADO_SUSPEITO'
  | 'INSTRUCAO_IA'
  | 'PROMPT_INJECTION'
  | 'TEXTO_OFUSCADO';
export type EvidenceType =
  | 'PROPRIEDADE_VISUAL'
  | 'TRECHO_TEXTO'
  | 'METADADO'
  | 'CARACTERE_UNICODE'
  | 'PADRAO_DETECTADO'
  | 'POSICIONAMENTO'
  | 'PROPRIEDADE_PDF';
export type RuleExecutionState = 'NAO_EXECUTADA' | 'EXECUTADA_COM_SUCESSO' | 'FALHOU';

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}
export interface DocumentDto {
  id: UUID;
  nomeOriginal: string;
  tipoMime: string;
  extensao: string;
  tamanhoBytes: number;
  hashSha256: string;
  quantidadePaginas: number | null;
  status: DocumentStatus;
  dataUpload: string;
  dataAtualizacao: string;
  mensagemErro: string | null;
}
export interface UploadResponse {
  documentoId: UUID;
  analiseId: UUID;
  statusDocumento: DocumentStatus;
  statusAnalise: AnalysisStatus;
  consultaAnalise: string;
}
export interface AnalysisDto {
  id: UUID;
  documentoId: UUID;
  status: AnalysisStatus;
  nivelRisco: RiskLevel | null;
  pontuacaoRisco: number | null;
  totalOcorrencias: number;
  totalOcorrenciasCriticas: number;
  totalOcorrenciasAltas: number;
  totalOcorrenciasMedias: number;
  totalOcorrenciasBaixas: number;
  totalRegrasExecutadas: number;
  versaoMotor: string;
  dataInicio: string | null;
  dataFim: string | null;
  duracaoMilissegundos: number | null;
  resumo: string | null;
  mensagemErro: string | null;
}
export interface OccurrenceDto {
  id: UUID;
  analiseId: UUID;
  regraId: UUID | null;
  tipo: OccurrenceType;
  nivelRisco: RiskLevel;
  titulo: string;
  descricao: string;
  conteudoSuspeito: string | null;
  conteudoNormalizado: string | null;
  numeroPagina: number | null;
  posicaoX: number | null;
  posicaoY: number | null;
  largura: number | null;
  altura: number | null;
  fonte: string | null;
  tamanhoFonte: number | null;
  corTexto: string | null;
  opacidade: number | null;
  pontuacao: number;
  dataIdentificacao: string;
}
export interface EvidenceDto {
  id: UUID;
  ocorrenciaId: UUID;
  tipo: EvidenceType;
  descricao: string;
  valorEncontrado: string | null;
  valorEsperado: string | null;
  detalhesTecnicos: string | null;
  numeroPagina: number | null;
  dataRegistro: string;
}
export interface DetectionRuleDto {
  id: UUID;
  codigo: string;
  nome: string;
  descricao: string;
  tipoOcorrencia: OccurrenceType;
  nivelRiscoPadrao: RiskLevel;
  pontuacaoBase: number;
  parametrosJson: string | null;
  ativa: boolean;
  versao: string;
  dataCriacao: string;
  dataAtualizacao: string;
}
export interface RuleExecutionDto {
  id: UUID;
  analiseId: UUID;
  regraId: UUID;
  estado: RuleExecutionState;
  executada: boolean;
  quantidadeOcorrencias: number;
  sucesso: boolean;
  duracaoMilissegundos: number;
  mensagemErro: string | null;
  dataExecucao: string;
}
export interface DashboardDto {
  totalDocumentos: number;
  totalAnalises: number;
  analisesEmAndamento: number;
  analisesConcluidas: number;
  analisesComFalha: number;
  totalOcorrencias: number;
  documentosComRiscoAlto: number;
  documentosComRiscoCritico: number;
}
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  timestamp?: string;
}
export type ApiKeyStatus = 'ATIVA' | 'EXPIRADA' | 'REVOGADA';
export type ApiKeyScope = 'DOCUMENT_ANALYZE';
export interface AdminSession {
  username: string;
  authenticated: boolean;
  roles?: string[];
}
export interface ApiKeyDto {
  id: UUID;
  nome: string;
  descricao: string | null;
  identificador: string;
  prefixo: string;
  escopo: string;
  status: ApiKeyStatus;
  criadaEm: string;
  expiraEm: string | null;
  ultimoUsoEm: string | null;
  revogadaEm: string | null;
  requisicoesPorMinuto: number | null;
}
export interface CreateApiKeyRequest {
  nome: string;
  descricao?: string | null;
  expiraEm?: string | null;
  escopo: ApiKeyScope;
  requisicoesPorMinuto?: number | null;
}
export interface CreatedApiKeyResponse {
  apiKey: ApiKeyDto;
  token: string;
}
export type MonitoringPeriod = '24H' | '7D' | '30D' | 'CUSTOM';
export interface MonitoringSummary {
  inicio: string;
  fim: string;
  granularidade: 'HOUR' | 'DAY';
  totalRequisicoes: number;
  totalSucesso: number;
  totalFalhas: number;
  taxaSucesso: number;
  duracaoMediaMs: number;
  p95Ms: number;
  integracoesComUso: number;
}
export interface MonitoringPoint {
  inicio: string;
  totalRequisicoes: number;
  totalSucesso: number;
  totalFalhas: number;
  duracaoMediaMs: number;
}
export interface MonitoringSeries {
  inicio: string;
  fim: string;
  granularidade: 'HOUR' | 'DAY';
  pontos: MonitoringPoint[];
}
export interface MonitoringIntegration {
  id: UUID;
  nome: string;
  status: string;
  totalRequisicoes: number;
  totalSucesso: number;
  totalFalhas: number;
  taxaSucesso: number;
  duracaoMediaMs: number;
  p95Ms: number;
  limitePorMinuto: number | null;
}
export interface MonitoringIntegrationList {
  inicio: string;
  fim: string;
  integracoes: MonitoringIntegration[];
}
export interface MonitoringErrorItem {
  tipo: string;
  quantidade: number;
  percentual: number;
}
export interface MonitoringErrors {
  inicio: string;
  fim: string;
  totalFalhas: number;
  erros: MonitoringErrorItem[];
}
export interface MonitoringCapacity {
  aplicacao: string;
  bancoDeDados: string;
  concorrenciaConfigurada: number;
  execucoesAtivas: number;
  workersDisponiveis: number;
  filaEmUso: number;
  capacidadeFila: number;
  capacidadeFilaDisponivel: number;
  verificadoEm: string;
}
