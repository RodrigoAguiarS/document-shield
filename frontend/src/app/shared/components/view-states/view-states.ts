import { Component, input, output } from '@angular/core';
@Component({
  selector: 'app-loading-state',
  standalone: true,
  template: `<div class="loading-block" role="status">
    <span class="state-icon"><i class="pi pi-spin pi-spinner"></i></span>
    <p>{{ message() }}</p>
  </div>`,
})
export class LoadingState {
  message = input('Carregando dados…');
}
@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `<div class="empty-block">
    <span class="state-icon"><i [class]="icon()"></i></span>
    <h2>{{ title() }}</h2>
    <p>{{ message() }}</p>
    <ng-content />
  </div>`,
})
export class EmptyState {
  title = input('Nenhum resultado');
  message = input('Não há informações para exibir.');
  icon = input('pi pi-inbox');
}
@Component({
  selector: 'app-error-state',
  standalone: true,
  template: `<div class="error-block" role="alert">
    <span class="state-icon"><i class="pi pi-exclamation-circle"></i></span>
    <h2>{{ title() }}</h2>
    <p>{{ message() }}</p>
    <button class="btn btn-secondary" type="button" (click)="retry.emit()">
      <i class="pi pi-refresh"></i>Tentar novamente
    </button>
  </div>`,
})
export class ErrorState {
  title = input('Não foi possível carregar');
  message = input('Verifique a conexão e tente novamente.');
  retry = output<void>();
}
