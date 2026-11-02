import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('initializes CSRF, logs in with form data and renews CSRF', () => {
    service
      .login('admin', 'senha')
      .subscribe((session) => expect(session.authenticated).toBeTrue());
    let req = http.expectOne('/api/v1/admin/auth/csrf');
    expect(req.request.method).toBe('GET');
    req.flush({});
    req = http.expectOne('/api/v1/admin/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toContain('username=admin');
    req.flush({ username: 'admin', authenticated: true });
    req = http.expectOne('/api/v1/admin/auth/csrf');
    req.flush({});
    expect(service.session()?.username).toBe('admin');
  });
});
