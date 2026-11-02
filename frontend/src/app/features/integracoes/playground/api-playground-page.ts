import { HttpEventType } from '@angular/common/http';
import { Component, computed, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiProblem } from '../../../core/interceptors/problem.interceptor';
import { PublicApiPlaygroundService } from '../../../core/services/public-api-playground.service';
import { inject } from '@angular/core';
import { ApiRequestPanel } from './api-request-panel';
import { ApiResponsePanel } from './api-response-panel';
import { IntegrationCodeExamples } from './integration-code-examples';
import {
  PlaygroundError,
  PlaygroundExecutionState,
  PlaygroundRequest,
  PlaygroundResult,
} from './playground.models';

@Component({
  selector: 'app-api-playground-page',
  standalone: true,
  imports: [RouterLink, ApiRequestPanel, ApiResponsePanel, IntegrationCodeExamples],
  templateUrl: './api-playground-page.html',
  styleUrl: './api-playground-page.css',
})
export class ApiPlaygroundPage implements OnDestroy {
  private readonly playground = inject(PublicApiPlaygroundService);
  private execution?: Subscription;

  readonly endpoint = this.playground.endpoint;
  readonly state = signal<PlaygroundExecutionState>('AGUARDANDO');
  readonly uploadProgress = signal(0);
  readonly result = signal<PlaygroundResult | null>(null);
  readonly error = signal<PlaygroundError | null>(null);
  readonly cancellationMessage = signal('');
  readonly clearSecretVersion = signal(0);
  readonly running = computed(() => this.state() === 'ENVIANDO' || this.state() === 'PROCESSANDO');

  execute(request: PlaygroundRequest): void {
    if (this.running()) {
      return;
    }

    this.result.set(null);
    this.error.set(null);
    this.cancellationMessage.set('');
    this.uploadProgress.set(0);
    this.state.set('ENVIANDO');
    const startedAt = performance.now();

    this.execution = this.playground.analyze(request.file, request.apiKey).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress) {
          const total = event.total ?? request.file.size;
          const progress = total > 0 ? Math.round((event.loaded / total) * 100) : 0;
          this.uploadProgress.set(Math.min(100, progress));
          if (event.loaded >= total) {
            this.state.set('PROCESSANDO');
          }
        }

        if (event.type === HttpEventType.Response && event.body) {
          this.result.set({
            status: event.status,
            clientDurationMs: performance.now() - startedAt,
            response: event.body,
          });
          this.state.set('CONCLUIDO');
          this.finishExecution();
        }
      },
      error: (failure: unknown) => {
        const problem = failure instanceof ApiProblem ? failure : null;
        this.error.set({
          status: problem?.status ?? 0,
          title: problem?.title ?? 'Falha de conexão',
          detail:
            problem?.detail ??
            'Não foi possível alcançar a API. Verifique a conexão e tente novamente.',
          type: problem?.type,
          correlationId: problem?.correlationId,
          retryAfter: problem?.retryAfter,
        });
        this.state.set('ERRO');
        this.finishExecution();
      },
    });
  }

  cancel(): void {
    if (!this.running()) {
      return;
    }
    this.execution?.unsubscribe();
    this.execution = undefined;
    this.uploadProgress.set(0);
    this.state.set('AGUARDANDO');
    this.clearSecretVersion.update((version) => version + 1);
    this.cancellationMessage.set(
      'Solicitação cancelada no navegador. O processamento no servidor pode levar alguns instantes para encerrar.',
    );
  }

  ngOnDestroy(): void {
    this.execution?.unsubscribe();
    this.execution = undefined;
    this.result.set(null);
    this.error.set(null);
    this.clearSecretVersion.update((version) => version + 1);
  }

  private finishExecution(): void {
    this.execution = undefined;
    this.clearSecretVersion.update((version) => version + 1);
  }
}
