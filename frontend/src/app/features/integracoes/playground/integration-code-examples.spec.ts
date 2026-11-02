import { TestBed } from '@angular/core/testing';
import { IntegrationCodeExamples } from './integration-code-examples';

describe('IntegrationCodeExamples', () => {
  it('provides all requested languages with placeholders instead of real secrets', async () => {
    await TestBed.configureTestingModule({
      imports: [IntegrationCodeExamples],
    }).compileComponents();
    const fixture = TestBed.createComponent(IntegrationCodeExamples);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.samples.map((sample) => sample.id)).toEqual([
      'curl',
      'java',
      'python',
      'javascript',
    ]);
    for (const sample of component.samples) {
      expect(sample.code).toContain('SUA_API_KEY');
      expect(sample.code).toContain('CAMINHO_DO_DOCUMENTO');
      expect(sample.code).toContain('/api/v1/public/analises');
      expect(sample.code).not.toContain('dsk_');
    }
  });
});
