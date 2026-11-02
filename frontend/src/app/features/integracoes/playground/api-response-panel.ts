import { Component, effect, input, signal } from '@angular/core';
import { RiskBadge } from '../../../shared/components/risk-badge/risk-badge';
import { JsonResponseViewer } from './json-response-viewer';
import { PlaygroundOccurrences } from './playground-occurrences';
import { PlaygroundErrorView } from './playground-error-view';
import { PlaygroundError, PlaygroundExecutionState, PlaygroundResult } from './playground.models';

type ResponseTab = 'overview' | 'occurrences' | 'json';

@Component({
  selector: 'app-api-response-panel',
  standalone: true,
  imports: [RiskBadge, JsonResponseViewer, PlaygroundOccurrences, PlaygroundErrorView],
  templateUrl: './api-response-panel.html',
  styleUrl: './api-response-panel.css',
})
export class ApiResponsePanel {
  readonly state = input.required<PlaygroundExecutionState>();
  readonly result = input<PlaygroundResult | null>(null);
  readonly error = input<PlaygroundError | null>(null);
  readonly cancellationMessage = input('');
  readonly activeTab = signal<ResponseTab>('overview');

  constructor() {
    effect(() => {
      this.result();
      this.error();
      this.activeTab.set('overview');
    });
  }

  selectTab(tab: ResponseTab): void {
    this.activeTab.set(tab);
  }

  formatDuration(milliseconds: number): string {
    return milliseconds < 1000
      ? `${Math.round(milliseconds)} ms`
      : `${(milliseconds / 1000).toFixed(2)} s`;
  }

  label(value: string): string {
    return value.replaceAll('_', ' ');
  }
}
