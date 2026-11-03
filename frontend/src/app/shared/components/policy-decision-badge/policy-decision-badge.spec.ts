import { TestBed } from '@angular/core/testing';
import { PolicyDecisionBadge } from './policy-decision-badge';

describe('PolicyDecisionBadge', () => {
  it('communicates the decision with text in addition to color', async () => {
    await TestBed.configureTestingModule({ imports: [PolicyDecisionBadge] }).compileComponents();
    const fixture = TestBed.createComponent(PolicyDecisionBadge);
    fixture.componentRef.setInput('decision', 'BLOCK');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('BLOCK');
    expect(fixture.nativeElement.querySelector('.decision').classList).toContain('block');
  });
});
