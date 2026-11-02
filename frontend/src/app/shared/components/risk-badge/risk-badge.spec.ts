import { TestBed } from '@angular/core/testing';
import { RiskBadge } from './risk-badge';
describe('RiskBadge', () => {
  it('renders level with a textual label, not color alone', async () => {
    await TestBed.configureTestingModule({ imports: [RiskBadge] }).compileComponents();
    const f = TestBed.createComponent(RiskBadge);
    f.componentRef.setInput('level', 'CRITICO');
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('CRITICO');
    expect((f.nativeElement.querySelector('.risk') as HTMLElement).classList).toContain('critico');
  });
  it('explains a missing risk', async () => {
    await TestBed.configureTestingModule({ imports: [RiskBadge] }).compileComponents();
    const f = TestBed.createComponent(RiskBadge);
    f.componentRef.setInput('level', null);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain('NÃO AVALIADO');
  });
});
