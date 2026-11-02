import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/api.models';

export class ApiProblem extends Error {
  constructor(
    public status: number,
    public detail: string,
    public title = 'Falha na solicitação',
    public type?: string,
    public correlationId?: string,
    public retryAfter?: string,
  ) {
    super(detail);
  }
}
export const problemInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as ProblemDetail | string | null;
      const detail =
        typeof body === 'object' && body?.detail
          ? body.detail
          : typeof body === 'string' && body.length < 300
            ? body
            : 'Não foi possível concluir a solicitação.';
      const title = typeof body === 'object' && body?.title ? body.title : 'Falha na solicitação';
      const type = typeof body === 'object' ? body?.type : undefined;
      const correlationId = typeof body === 'object' ? body?.correlacaoId : undefined;
      const retryAfter = error.headers.get('Retry-After') ?? undefined;
      return throwError(
        () => new ApiProblem(error.status, detail, title, type, correlationId, retryAfter),
      );
    }),
  );
