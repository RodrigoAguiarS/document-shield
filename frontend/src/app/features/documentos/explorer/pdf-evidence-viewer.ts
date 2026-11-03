import { NgStyle } from '@angular/common';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import {
  GlobalWorkerOptions,
  PDFDocumentLoadingTask,
  PDFDocumentProxy,
  PDFPageProxy,
  RenderTask,
  TextLayer,
  getDocument,
} from 'pdfjs-dist';
import { OccurrenceDto, UUID } from '../../../core/models/api.models';
import { evidenceRect } from './evidence-coordinate';

type FitMode = 'width' | 'page' | 'custom';

GlobalWorkerOptions.workerSrc = new URL('pdfjs/pdf.worker.min.mjs', document.baseURI).toString();

@Component({
  selector: 'app-pdf-evidence-viewer',
  standalone: true,
  imports: [NgStyle],
  templateUrl: './pdf-evidence-viewer.html',
  styleUrl: './pdf-evidence-viewer.css',
})
export class PdfEvidenceViewer implements AfterViewInit, OnChanges {
  @Input({ required: true }) file: Blob | null = null;
  @Input() documentName = 'documento.pdf';
  @Input() occurrences: OccurrenceDto[] = [];
  @Input() selectedId: UUID | null = null;
  @Output() occurrenceSelected = new EventEmitter<OccurrenceDto>();

  @ViewChild('viewportHost') private viewportHost?: ElementRef<HTMLElement>;
  @ViewChild('canvas') private canvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('textLayer') private textLayer?: ElementRef<HTMLElement>;

  readonly loading = signal(true);
  readonly error = signal('');
  readonly pageNumber = signal(1);
  readonly pageCount = signal(0);
  readonly zoomPercent = signal(100);
  readonly rotation = signal(0);
  readonly fitMode = signal<FitMode>('width');
  readonly viewportWidth = signal(0);
  readonly viewportHeight = signal(0);
  readonly liveMessage = signal('');

  private readonly destroyRef = inject(DestroyRef);
  private pdf: PDFDocumentProxy | null = null;
  private loadingTask: PDFDocumentLoadingTask | null = null;
  private renderTask: RenderTask | null = null;
  private resizeObserver?: ResizeObserver;
  private loadVersion = 0;
  private viewReady = false;

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.resizeObserver?.disconnect();
      this.renderTask?.cancel();
      void this.loadingTask?.destroy();
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.resizeObserver = new ResizeObserver(() => {
      if (this.fitMode() !== 'custom') void this.renderPage();
    });
    this.resizeObserver.observe(this.viewportHost!.nativeElement);
    if (this.file) void this.loadDocument();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['file'] && this.viewReady) void this.loadDocument();
    if (changes['selectedId'] && this.selectedId) {
      const occurrence = this.occurrences.find((item) => item.id === this.selectedId);
      if (occurrence?.numeroPagina && occurrence.numeroPagina !== this.pageNumber()) {
        void this.goToPage(occurrence.numeroPagina);
      }
    }
  }

  pageOccurrences(): OccurrenceDto[] {
    return this.occurrences.filter((item) => item.numeroPagina === this.pageNumber());
  }

  markerStyle(occurrence: OccurrenceDto): Record<string, string> | null {
    const rect = evidenceRect(
      occurrence,
      this.viewportWidth(),
      this.viewportHeight(),
      this.rotation(),
    );
    if (!rect) return null;
    const left = rect.outsidePage
      ? Math.max(0, Math.min(rect.left, this.viewportWidth() - 12))
      : rect.left;
    const top = rect.outsidePage
      ? Math.max(0, Math.min(rect.top, this.viewportHeight() - 12))
      : rect.top;
    return {
      left: `${left}px`,
      top: `${top}px`,
      width: `${rect.outsidePage ? Math.max(12, Math.min(rect.width, 28)) : Math.max(4, rect.width)}px`,
      height: `${rect.outsidePage ? Math.max(12, Math.min(rect.height, 28)) : Math.max(4, rect.height)}px`,
    };
  }

  isOutside(occurrence: OccurrenceDto): boolean {
    return (
      evidenceRect(occurrence, this.viewportWidth(), this.viewportHeight(), this.rotation())
        ?.outsidePage ?? false
    );
  }

  async previous(): Promise<void> {
    await this.goToPage(this.pageNumber() - 1);
  }

  async next(): Promise<void> {
    await this.goToPage(this.pageNumber() + 1);
  }

  async pageInput(event: Event): Promise<void> {
    const value = Number((event.target as HTMLInputElement).value);
    await this.goToPage(value);
  }

  async zoom(delta: number): Promise<void> {
    this.fitMode.set('custom');
    this.zoomPercent.update((value) => Math.max(25, Math.min(400, value + delta)));
    await this.renderPage();
  }

  async fit(mode: Exclude<FitMode, 'custom'>): Promise<void> {
    this.fitMode.set(mode);
    await this.renderPage();
  }

  async rotate(): Promise<void> {
    this.rotation.update((value) => (value + 90) % 360);
    await this.renderPage();
  }

  download(): void {
    if (!this.file) return;
    const url = URL.createObjectURL(this.file);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = this.documentName;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url));
  }

  private async loadDocument(): Promise<void> {
    const currentVersion = ++this.loadVersion;
    this.loading.set(true);
    this.error.set('');
    this.renderTask?.cancel();
    await this.loadingTask?.destroy();
    this.loadingTask = null;
    this.pdf = null;
    if (!this.file) return;
    try {
      const data = await this.file.arrayBuffer();
      const task = getDocument({
        data,
        disableRange: true,
        disableStream: true,
        enableXfa: false,
        useWorkerFetch: false,
        useWasm: false,
      });
      this.loadingTask = task;
      const pdf = await task.promise;
      if (currentVersion !== this.loadVersion) {
        await task.destroy();
        return;
      }
      this.pdf = pdf;
      this.pageCount.set(pdf.numPages);
      this.pageNumber.set(Math.min(this.pageNumber(), pdf.numPages));
      await this.renderPage();
    } catch {
      this.error.set('Não foi possível renderizar este PDF com segurança.');
      this.loading.set(false);
    }
  }

  private async goToPage(page: number): Promise<void> {
    if (!this.pdf || !Number.isFinite(page)) return;
    const nextPage = Math.max(1, Math.min(Math.trunc(page), this.pageCount()));
    if (nextPage === this.pageNumber() && !this.loading()) return;
    this.pageNumber.set(nextPage);
    this.liveMessage.set(`Página ${nextPage} de ${this.pageCount()}`);
    await this.renderPage();
  }

  private async renderPage(): Promise<void> {
    if (!this.pdf || !this.canvas || !this.textLayer || !this.viewportHost) return;
    this.loading.set(true);
    this.renderTask?.cancel();
    let page: PDFPageProxy;
    try {
      page = await this.pdf.getPage(this.pageNumber());
      const totalRotation = (page.rotate + this.rotation()) % 360;
      const base = page.getViewport({ scale: 1, rotation: totalRotation });
      const host = this.viewportHost.nativeElement;
      const availableWidth = Math.max(240, host.clientWidth - 48);
      const availableHeight = Math.max(320, host.clientHeight - 48);
      let scale = this.zoomPercent() / 100;
      if (this.fitMode() === 'width') scale = availableWidth / base.width;
      if (this.fitMode() === 'page') {
        scale = Math.min(availableWidth / base.width, availableHeight / base.height);
      }
      scale = Math.max(0.25, Math.min(4, scale));
      this.zoomPercent.set(Math.round(scale * 100));
      const viewport = page.getViewport({ scale, rotation: totalRotation });
      this.viewportWidth.set(viewport.width);
      this.viewportHeight.set(viewport.height);

      const canvas = this.canvas.nativeElement;
      const context = canvas.getContext('2d', { alpha: false });
      if (!context) throw new Error('Canvas indisponível');
      const outputScale = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.floor(viewport.width * outputScale);
      canvas.height = Math.floor(viewport.height * outputScale);
      canvas.style.width = `${viewport.width}px`;
      canvas.style.height = `${viewport.height}px`;
      const transform = outputScale === 1 ? undefined : [outputScale, 0, 0, outputScale, 0, 0];
      this.renderTask = page.render({ canvas, canvasContext: context, transform, viewport });
      await this.renderTask.promise;

      const layerContainer = this.textLayer.nativeElement;
      layerContainer.replaceChildren();
      layerContainer.style.width = `${viewport.width}px`;
      layerContainer.style.height = `${viewport.height}px`;
      const textLayer = new TextLayer({
        textContentSource: await page.getTextContent(),
        container: layerContainer,
        viewport,
      });
      await textLayer.render();
      this.loading.set(false);
    } catch (error) {
      if (error instanceof Error && error.name === 'RenderingCancelledException') return;
      this.error.set('A página selecionada não pôde ser renderizada.');
      this.loading.set(false);
    }
  }

  @HostListener('document:keydown', ['$event'])
  async keyboard(event: KeyboardEvent): Promise<void> {
    const target = event.target as HTMLElement | null;
    if (target?.matches('input, textarea, select')) return;
    if (event.key === 'PageUp') await this.previous();
    else if (event.key === 'PageDown') await this.next();
    else if (event.key === '+' || event.key === '=') await this.zoom(10);
    else if (event.key === '-') await this.zoom(-10);
    else if (event.key.toLowerCase() === 'r') await this.rotate();
    else return;
    event.preventDefault();
  }
}
