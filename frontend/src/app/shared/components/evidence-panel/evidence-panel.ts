import { Component, input } from '@angular/core';
import { EvidenceDto } from '../../../core/models/api.models';
import { EmptyState } from '../view-states/view-states';

@Component({
  selector: 'app-evidence-panel',
  standalone: true,
  imports: [EmptyState],
  template: `
    <section class="evidence">
      <h3>
        Evidências técnicas <span>({{ items().length }})</span>
      </h3>
      @if (items().length) {
        @for (item of items(); track item.id) {
          <details>
            <summary>
              <span><i class="pi pi-shield" aria-hidden="true"></i>{{ label(item.tipo) }}</span
              ><i class="pi pi-chevron-down" aria-hidden="true"></i>
            </summary>
            <div class="evidence-body">
              <strong>{{ item.descricao }}</strong>
              @if (item.numeroPagina !== null) {
                <dl>
                  <dt>Página</dt>
                  <dd>{{ item.numeroPagina }}</dd>
                </dl>
              }
              @if (item.valorEncontrado) {
                <div class="evidence-field">
                  <span>Valor encontrado</span>
                  <pre>{{ item.valorEncontrado }}</pre>
                </div>
              }
              @if (item.valorEsperado) {
                <div class="evidence-field">
                  <span>Valor esperado</span>
                  <pre>{{ item.valorEsperado }}</pre>
                </div>
              }
              @if (item.detalhesTecnicos) {
                <div class="evidence-field">
                  <span>Detalhes técnicos</span>
                  <pre>{{ item.detalhesTecnicos }}</pre>
                </div>
              }
            </div>
          </details>
        }
      } @else {
        <app-empty-state
          title="Sem evidências adicionais"
          message="A ocorrência não possui evidências técnicas registradas."
          icon="pi pi-file"
        />
      }
    </section>
  `,
  styles: [
    `
      .evidence h3 {
        margin: 0.2rem 0 0.7rem;
        font-size: 0.85rem;
      }
      .evidence h3 span {
        color: var(--muted);
        font-weight: 500;
      }
      details {
        border: 1px solid var(--line);
        border-radius: 6px;
        margin-bottom: 0.5rem;
        background: var(--surface);
      }
      summary {
        display: flex;
        justify-content: space-between;
        align-items: center;
        min-height: 2.75rem;
        padding: 0.75rem;
        cursor: pointer;
        font-size: 0.78rem;
        font-weight: 700;
        list-style: none;
      }
      summary span {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }
      summary > i {
        font-size: 0.65rem;
        transition: transform 0.15s;
      }
      details[open] summary > i {
        transform: rotate(180deg);
      }
      .evidence-body {
        padding: 0.8rem;
        border-top: 1px solid var(--line);
        display: grid;
        gap: 0.7rem;
        font-size: 0.76rem;
      }
      .evidence-field {
        display: grid;
        gap: 0.35rem;
      }
      .evidence-field > span {
        font-weight: 700;
        color: var(--muted);
      }
      pre {
        margin: 0;
        padding: 0.65rem;
        background: #090d12;
        border: 1px solid var(--line);
        border-radius: 5px;
        font: inherit;
        font-family: var(--font-mono);
        white-space: pre-wrap;
        overflow-wrap: anywhere;
      }
      dl {
        display: flex;
        gap: 0.5rem;
        margin: 0;
      }
      dt {
        font-weight: 700;
      }
      dd {
        margin: 0;
      }
    `,
  ],
})
export class EvidencePanel {
  items = input.required<EvidenceDto[]>();
  label(value: string) {
    return value.replaceAll('_', ' ');
  }
}
