import { Routes } from '@angular/router';
import { AppShell } from './layout/shell/app-shell';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
    title: 'Acesso administrativo | Document Shield',
  },
  {
    path: '',
    component: AppShell,
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
        title: 'Visão geral | Document Shield',
      },
      {
        path: 'documentos',
        loadComponent: () =>
          import('./features/documentos/documents-page').then((m) => m.DocumentsPage),
        title: 'Documentos | Document Shield',
      },
      {
        path: 'documentos/:id',
        loadComponent: () =>
          import('./features/documentos/document-detail-page').then((m) => m.DocumentDetailPage),
        title: 'Documento | Document Shield',
      },
      {
        path: 'analises',
        loadComponent: () =>
          import('./features/analises/analyses-page').then((m) => m.AnalysesPage),
        title: 'Análises | Document Shield',
      },
      {
        path: 'analises/:id',
        loadComponent: () =>
          import('./features/analises/analysis-detail-page').then((m) => m.AnalysisDetailPage),
        title: 'Detalhes da análise | Document Shield',
      },
      {
        path: 'ocorrencias/:id',
        loadComponent: () =>
          import('./features/ocorrencias/occurrence-page').then((m) => m.OccurrencePage),
        title: 'Ocorrência | Document Shield',
      },
      {
        path: 'regras',
        loadComponent: () => import('./features/regras/rules-page').then((m) => m.RulesPage),
        title: 'Regras | Document Shield',
      },
      {
        path: 'integracoes/api-keys',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/integracoes/api-keys-page').then((m) => m.ApiKeysPage),
        title: 'API Keys | Document Shield',
      },
      {
        path: 'integracoes/monitoramento',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/integracoes/monitoring-page').then((m) => m.MonitoringPage),
        title: 'Monitoramento da API | Document Shield',
      },
      {
        path: 'integracoes/playground',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/integracoes/playground/api-playground-page').then(
            (m) => m.ApiPlaygroundPage,
          ),
        title: 'API Playground | Document Shield',
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
