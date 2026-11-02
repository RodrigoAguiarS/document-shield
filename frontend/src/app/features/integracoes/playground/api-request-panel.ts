import { Component, computed, effect, input, OnDestroy, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { APP_CONFIG } from '../../../core/config/app-config';
import { inject } from '@angular/core';
import { PlaygroundExecutionState, PlaygroundRequest } from './playground.models';

@Component({
  selector: 'app-api-request-panel',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './api-request-panel.html',
  styleUrl: './api-request-panel.css',
})
export class ApiRequestPanel implements OnDestroy {
  private readonly config = inject(APP_CONFIG);

  readonly endpoint = input.required<string>();
  readonly state = input.required<PlaygroundExecutionState>();
  readonly uploadProgress = input(0);
  readonly clearSecretVersion = input(0);
  readonly executeRequest = output<PlaygroundRequest>();
  readonly cancelRequest = output<void>();

  readonly showApiKey = signal(false);
  readonly dragging = signal(false);
  readonly validationError = signal('');
  readonly running = computed(() => this.state() === 'ENVIANDO' || this.state() === 'PROCESSANDO');
  readonly maxUploadMb = this.config.maxUploadBytes / 1024 / 1024;

  apiKey = '';
  selectedFile: File | null = null;

  constructor() {
    effect(() => {
      this.clearSecretVersion();
      this.clearApiKey();
    });
  }

  ngOnDestroy(): void {
    this.clearApiKey();
  }

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.setFile(file);
    }
    input.value = '';
  }

  dragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.running()) {
      this.dragging.set(true);
    }
  }

  dragLeave(event: DragEvent): void {
    if (
      !event.currentTarget ||
      !(event.currentTarget as Node).contains(event.relatedTarget as Node)
    ) {
      this.dragging.set(false);
    }
  }

  dropFile(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    if (this.running()) {
      return;
    }
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.setFile(file);
    }
  }

  removeFile(): void {
    if (!this.running()) {
      this.selectedFile = null;
      this.validationError.set('');
    }
  }

  submit(): void {
    this.validationError.set('');
    if (!this.apiKey.trim()) {
      this.validationError.set('Informe uma API Key para executar a análise.');
      return;
    }
    if (!this.selectedFile) {
      this.validationError.set('Selecione um documento PDF.');
      return;
    }

    const fileError = this.validateFile(this.selectedFile);
    if (fileError) {
      this.validationError.set(fileError);
      return;
    }

    this.executeRequest.emit({ apiKey: this.apiKey.trim(), file: this.selectedFile });
  }

  cancel(): void {
    if (this.running()) {
      this.cancelRequest.emit();
    }
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024 * 1024) {
      return `${Math.max(1, Math.round(bytes / 1024))} KB`;
    }
    return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
  }

  private setFile(file: File): void {
    const fileError = this.validateFile(file);
    if (fileError) {
      this.selectedFile = null;
      this.validationError.set(fileError);
      return;
    }
    this.selectedFile = file;
    this.validationError.set('');
  }

  private validateFile(file: File): string | null {
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      return 'O Playground aceita somente arquivos PDF.';
    }
    if (file.size === 0) {
      return 'O arquivo selecionado está vazio.';
    }
    if (file.size > this.config.maxUploadBytes) {
      return `O arquivo excede o limite de ${this.maxUploadMb} MB.`;
    }
    return null;
  }

  private clearApiKey(): void {
    this.apiKey = '';
    this.showApiKey.set(false);
  }
}
