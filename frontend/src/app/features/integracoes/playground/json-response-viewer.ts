import { Component, computed, input, signal } from '@angular/core';
import { PublicAnalysisResponseDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-json-response-viewer',
  standalone: true,
  templateUrl: './json-response-viewer.html',
  styleUrl: './json-response-viewer.css',
})
export class JsonResponseViewer {
  readonly response = input.required<PublicAnalysisResponseDto>();
  readonly formattedJson = computed(() => JSON.stringify(this.response(), null, 2));
  readonly copied = signal(false);
  readonly copyFailed = signal(false);

  async copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.formattedJson());
      this.copied.set(true);
      this.copyFailed.set(false);
      window.setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.copyFailed.set(true);
      this.copied.set(false);
    }
  }
}
