import { Component, input } from '@angular/core';
import { PlaygroundError } from './playground.models';

@Component({
  selector: 'app-playground-error-view',
  standalone: true,
  templateUrl: './playground-error-view.html',
  styleUrl: './playground-error-view.css',
})
export class PlaygroundErrorView {
  readonly error = input.required<PlaygroundError>();

  statusLabel(status: number): string {
    if (status === 0) {
      return 'Falha de conexão';
    }
    const labels: Record<number, string> = {
      400: 'Requisição inválida',
      401: 'API Key inválida ou ausente',
      403: 'Operação não autorizada',
      413: 'Arquivo acima do limite',
      415: 'Formato não suportado',
      422: 'Documento não analisável',
      429: 'Rate limit excedido',
      500: 'Falha interna',
      503: 'Capacidade indisponível',
      504: 'Tempo limite excedido',
    };
    return labels[status] ?? 'Falha na solicitação';
  }
}
