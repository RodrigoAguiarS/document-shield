import { Component, input } from '@angular/core';
import { PublicOccurrenceDto } from '../../../core/models/api.models';
import { RiskBadge } from '../../../shared/components/risk-badge/risk-badge';

@Component({
  selector: 'app-playground-occurrences',
  standalone: true,
  imports: [RiskBadge],
  templateUrl: './playground-occurrences.html',
  styleUrl: './playground-occurrences.css',
})
export class PlaygroundOccurrences {
  readonly occurrences = input.required<PublicOccurrenceDto[]>();

  label(value: string): string {
    return value.replaceAll('_', ' ');
  }
}
