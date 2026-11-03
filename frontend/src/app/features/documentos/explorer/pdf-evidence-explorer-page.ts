import { DatePipe } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import {
  AnalysisDto,
  DocumentDto,
  EvidenceDto,
  OccurrenceDto,
  PagedResponse,
  RiskLevel,
  UUID,
} from '../../../core/models/api.models';
import { AnalysisService } from '../../../core/services/analysis.service';
import { DocumentService } from '../../../core/services/document.service';
import { OccurrenceService } from '../../../core/services/occurrence.service';
import { EvidencePanel } from '../../../shared/components/evidence-panel/evidence-panel';
import { RiskBadge } from '../../../shared/components/risk-badge/risk-badge';
import { StatusBadge } from '../../../shared/components/status-badge/status-badge';
import { ErrorState, LoadingState } from '../../../shared/components/view-states/view-states';
import { PdfEvidenceViewer } from './pdf-evidence-viewer';

type SortMode = 'risk' | 'page' | 'score';
type LocationFilter = 'all' | 'located' | 'unlocated';

@Component({
  selector: 'app-pdf-evidence-explorer-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    EvidencePanel,
    RiskBadge,
    StatusBadge,
    ErrorState,
    LoadingState,
    PdfEvidenceViewer,
  ],
  templateUrl: './pdf-evidence-explorer-page.html',
  styleUrl: './pdf-evidence-explorer-page.css',
})
export class PdfEvidenceExplorerPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly documents = inject(DocumentService);
  private readonly analyses = inject(AnalysisService);
  private readonly occurrenceService = inject(OccurrenceService);

  readonly documentId = this.route.snapshot.paramMap.get('id')!;
  readonly document = signal<DocumentDto | null>(null);
  readonly history = signal<PagedResponse<AnalysisDto> | null>(null);
  readonly analysis = signal<AnalysisDto | null>(null);
  readonly file = signal<Blob | null>(null);
  readonly occurrences = signal<OccurrenceDto[]>([]);
  readonly selected = signal<OccurrenceDto | null>(null);
  readonly evidence = signal<EvidenceDto[]>([]);
  readonly loading = signal(true);
  readonly evidenceLoading = signal(false);
  readonly error = signal('');

  query = '';
  risk = 'all';
  type = 'all';
  page = 'all';
  location: LocationFilter = 'all';
  sort: SortMode = 'risk';
  private loadVersion = 0;

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    const version = ++this.loadVersion;
    try {
      const result = await firstValueFrom(
        forkJoin({
          document: this.documents.get(this.documentId),
          history: this.documents.history(this.documentId, {
            page: 0,
            size: 100,
            sort: 'dataInicio,desc',
          }),
          file: this.documents.download(this.documentId),
        }),
      );
      if (version !== this.loadVersion) return;
      this.document.set(result.document);
      this.history.set(result.history);
      this.file.set(result.file.body);
      const requestedId = this.route.snapshot.queryParamMap.get('analise');
      const chosen =
        result.history.content.find((item) => item.id === requestedId) ??
        result.history.content.find((item) => item.status === 'CONCLUIDA') ??
        result.history.content[0] ??
        null;
      if (chosen) await this.selectAnalysis(chosen.id, false);
      this.loading.set(false);
    } catch (error: unknown) {
      this.error.set(this.errorDetail(error, 'Não foi possível abrir o explorador de evidências.'));
      this.loading.set(false);
    }
  }

  async analysisChanged(event: Event): Promise<void> {
    await this.selectAnalysis((event.target as HTMLSelectElement).value, true);
  }

  async selectAnalysis(id: UUID, updateUrl: boolean): Promise<void> {
    const selectedAnalysis = this.history()?.content.find((item) => item.id === id) ?? null;
    if (!selectedAnalysis) return;
    this.analysis.set(selectedAnalysis);
    this.occurrences.set([]);
    this.selected.set(null);
    this.evidence.set([]);
    if (updateUrl) {
      await this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { analise: id, ocorrencia: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
    if (selectedAnalysis.status !== 'CONCLUIDA') return;
    try {
      const all = await this.loadAllOccurrences(id);
      if (this.analysis()?.id !== id) return;
      this.occurrences.set(all);
      const requestedOccurrence = this.route.snapshot.queryParamMap.get('ocorrencia');
      const initial = all.find((item) => item.id === requestedOccurrence) ?? all[0] ?? null;
      if (initial) this.selectOccurrence(initial, false);
    } catch (error: unknown) {
      this.error.set(this.errorDetail(error, 'Não foi possível carregar as ocorrências.'));
    }
  }

  filteredOccurrences(): OccurrenceDto[] {
    const query = this.query.trim().toLocaleLowerCase('pt-BR');
    const filtered = this.occurrences().filter((item) => {
      const hasLocation = item.localizacaoConfiavel === true;
      return (
        (this.risk === 'all' || item.nivelRisco === this.risk) &&
        (this.type === 'all' || item.tipo === this.type) &&
        (this.page === 'all' || item.numeroPagina === Number(this.page)) &&
        (this.location === 'all' || (this.location === 'located' ? hasLocation : !hasLocation)) &&
        (!query ||
          item.titulo.toLocaleLowerCase('pt-BR').includes(query) ||
          item.descricao.toLocaleLowerCase('pt-BR').includes(query) ||
          item.conteudoSuspeito?.toLocaleLowerCase('pt-BR').includes(query))
      );
    });
    return filtered.sort((a, b) => {
      if (this.sort === 'page') return (a.numeroPagina ?? 999999) - (b.numeroPagina ?? 999999);
      if (this.sort === 'score') return b.pontuacao - a.pontuacao;
      return (
        this.riskWeight(b.nivelRisco) - this.riskWeight(a.nivelRisco) || b.pontuacao - a.pontuacao
      );
    });
  }

  occurrenceTypes(): string[] {
    return [...new Set(this.occurrences().map((item) => item.tipo))].sort();
  }

  occurrencePages(): number[] {
    return [
      ...new Set(
        this.occurrences()
          .map((item) => item.numeroPagina)
          .filter((page): page is number => page !== null),
      ),
    ].sort((a, b) => a - b);
  }

  selectOccurrence(item: OccurrenceDto, updateUrl = true): void {
    this.selected.set(item);
    this.evidence.set([]);
    this.evidenceLoading.set(true);
    if (updateUrl) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { analise: this.analysis()?.id, ocorrencia: item.id },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
    this.occurrenceService.evidence(item.id).subscribe({
      next: (evidence) => {
        if (this.selected()?.id === item.id) this.evidence.set(evidence);
        this.evidenceLoading.set(false);
      },
      error: () => this.evidenceLoading.set(false),
    });
  }

  locationLabel(item: OccurrenceDto): string {
    if (item.localizacaoConfiavel !== true) return 'Sem localização visual confiável';
    const outside =
      item.posicaoX !== null &&
      item.posicaoY !== null &&
      item.largura !== null &&
      item.altura !== null &&
      item.larguraPagina !== null &&
      item.alturaPagina !== null &&
      (item.posicaoX < 0 ||
        item.posicaoY < 0 ||
        item.posicaoX + item.largura > item.larguraPagina ||
        item.posicaoY + item.altura > item.alturaPagina);
    return outside ? 'Fora da área visível' : `Página ${item.numeroPagina}`;
  }

  label(value: string): string {
    return value.replaceAll('_', ' ');
  }

  coords(item: OccurrenceDto): string {
    if (item.localizacaoConfiavel !== true || item.posicaoX === null || item.posicaoY === null) {
      return 'Não determinadas pelo extrator';
    }
    return `x ${item.posicaoX.toFixed(2)}, y ${item.posicaoY.toFixed(2)}, ${item.largura?.toFixed(2)} × ${item.altura?.toFixed(2)}`;
  }

  @HostListener('keydown', ['$event'])
  listKeyboard(event: KeyboardEvent): void {
    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
    const list = this.filteredOccurrences();
    if (!list.length) return;
    const index = Math.max(
      0,
      list.findIndex((item) => item.id === this.selected()?.id),
    );
    const next =
      event.key === 'ArrowDown' ? Math.min(list.length - 1, index + 1) : Math.max(0, index - 1);
    this.selectOccurrence(list[next]);
    event.preventDefault();
  }

  private async loadAllOccurrences(analysisId: UUID): Promise<OccurrenceDto[]> {
    const first = await firstValueFrom(
      this.analyses.occurrences(analysisId, { page: 0, size: 100, sort: 'dataIdentificacao,asc' }),
    );
    const all = [...first.content];
    for (let page = 1; page < first.totalPages; page++) {
      const response = await firstValueFrom(
        this.analyses.occurrences(analysisId, { page, size: 100, sort: 'dataIdentificacao,asc' }),
      );
      all.push(...response.content);
    }
    return all;
  }

  private riskWeight(level: RiskLevel): number {
    return { INFORMATIVO: 0, BAIXO: 1, MEDIO: 2, ALTO: 3, CRITICO: 4 }[level];
  }

  private errorDetail(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error && 'detail' in error) return String(error['detail']);
    return fallback;
  }
}
