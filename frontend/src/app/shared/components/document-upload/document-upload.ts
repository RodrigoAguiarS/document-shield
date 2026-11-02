import { HttpEventType } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ProgressBarModule } from 'primeng/progressbar';
import { finalize } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app-config';
import { ApiProblem } from '../../../core/interceptors/problem.interceptor';
import { DocumentService } from '../../../core/services/document.service';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [ProgressBarModule],
  template: `
    <section
      id="upload"
      class="upload-zone"
      [class.dragging]="dragging()"
      [class.busy]="uploading()"
      (dragover)="over($event)"
      (dragleave)="dragging.set(false)"
      (drop)="drop($event)"
      aria-label="Envio de documento PDF"
    >
      <input
        #picker
        type="file"
        accept="application/pdf,.pdf"
        hidden
        (change)="selected($event)"
        [disabled]="uploading()"
      />
      <div class="upload-icon" aria-hidden="true"><i class="pi pi-file-arrow-up"></i></div>
      <div class="upload-copy">
        <strong>{{ uploading() ? 'Enviando documento…' : 'Arraste e solte um PDF aqui' }}</strong>
        <span>{{ fileName() || 'ou selecione um arquivo no dispositivo' }}</span>
      </div>
      <button
        class="btn btn-secondary"
        type="button"
        (click)="picker.click()"
        [disabled]="uploading()"
      >
        Selecionar PDF
      </button>
      <div class="upload-limit">Máximo {{ maxMb }} MB<br />Apenas PDF</div>
      @if (uploading()) {
        <p-progressBar
          class="progress"
          [value]="progress()"
          [showValue]="true"
          aria-label="Progresso do envio"
        />
      }
      @if (error()) {
        <p class="upload-error" role="alert">
          <i class="pi pi-exclamation-circle" aria-hidden="true"></i>{{ error() }}
        </p>
      }
    </section>
  `,
  styles: [
    `
      .upload-zone {
        position: relative;
        display: grid;
        grid-template-columns: auto 1fr auto auto;
        align-items: center;
        gap: 1rem;
        background: #0d1219;
        border: 1px dashed #45607d;
        border-radius: var(--radius);
        padding: 1.05rem 1.25rem;
        margin-bottom: 1.25rem;
        transition:
          background 0.15s,
          border-color 0.15s;
      }
      .upload-zone.dragging {
        background: #102653;
        border-color: var(--ds-cobalt-hover);
      }
      .upload-icon {
        width: 2.8rem;
        height: 2.8rem;
        border-radius: 6px;
        display: grid;
        place-items: center;
        background: #1a2330;
        color: var(--ds-acid-lime);
        border: 1px solid var(--line);
        font-size: 1.2rem;
      }
      .upload-copy {
        display: grid;
        gap: 0.2rem;
        min-width: 0;
      }
      .upload-copy strong {
        font-size: 0.92rem;
      }
      .upload-copy span,
      .upload-limit {
        font-size: 0.76rem;
        color: var(--muted);
      }
      .upload-copy span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .upload-limit {
        text-align: right;
        line-height: 1.5;
      }
      .progress {
        grid-column: 1/-1;
      }
      .upload-error {
        grid-column: 1/-1;
        margin: 0;
        color: var(--danger);
        font-size: 0.8rem;
        display: flex;
        gap: 0.45rem;
      }
      .busy {
        pointer-events: none;
        opacity: 0.82;
      }
      @media (max-width: 720px) {
        .upload-zone {
          grid-template-columns: auto 1fr;
          padding: 1rem;
        }
        .upload-limit {
          display: none;
        }
        .upload-zone .btn {
          grid-column: 1/-1;
        }
        .upload-icon {
          align-self: start;
        }
        .upload-copy span {
          font-size: 0.8rem;
        }
      }
    `,
  ],
})
export class DocumentUpload {
  private readonly service = inject(DocumentService);
  private readonly router = inject(Router);
  private readonly messages = inject(MessageService);
  private readonly config = inject(APP_CONFIG);

  readonly maxMb = this.config.maxUploadBytes / 1024 / 1024;
  readonly dragging = signal(false);
  readonly uploading = signal(false);
  readonly progress = signal(0);
  readonly error = signal('');
  readonly fileName = signal('');

  over(event: DragEvent): void {
    event.preventDefault();
    if (!this.uploading()) {
      this.dragging.set(true);
    }
  }

  drop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.send(file);
    }
  }

  selected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.send(file);
    }
    input.value = '';
  }

  private send(file: File): void {
    this.error.set('');

    const validationError = this.validateFile(file);
    if (validationError) {
      this.error.set(validationError);
      return;
    }

    this.fileName.set(file.name);
    this.uploading.set(true);
    this.progress.set(0);
    this.service
      .upload(file)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (event) => {
          if (event.type === HttpEventType.UploadProgress && event.total)
            this.progress.set(Math.round((100 * event.loaded) / event.total));
          if (event.type === HttpEventType.Response && event.body) {
            this.messages.add({
              severity: 'success',
              summary: 'Documento recebido',
              detail: 'A análise foi enfileirada.',
            });
            void this.router.navigate(['/analises', event.body.analiseId]);
          }
        },
        error: (error) =>
          this.error.set(
            error instanceof ApiProblem ? error.detail : 'Não foi possível enviar o documento.',
          ),
      });
  }

  private validateFile(file: File): string | null {
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      return 'Selecione um arquivo PDF válido.';
    }
    if (file.size > this.config.maxUploadBytes) {
      return `O arquivo excede o limite de ${this.maxMb} MB.`;
    }
    if (file.size === 0) {
      return 'O arquivo está vazio.';
    }
    return null;
  }
}
