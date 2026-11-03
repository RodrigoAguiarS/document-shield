import { TestBed } from '@angular/core/testing';
import { PolicyEditor } from './policy-editor';

describe('PolicyEditor', () => {
  it('starts with a complete default policy and emits it', async () => {
    await TestBed.configureTestingModule({ imports: [PolicyEditor] }).compileComponents();
    const fixture = TestBed.createComponent(PolicyEditor);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const saved = jasmine.createSpy('savePolicy');
    component.savePolicy.subscribe(saved);
    component.form.nome = 'Política Financeira';

    expect(component.issues()).toEqual([]);
    component.submit();

    expect(saved).toHaveBeenCalled();
    expect(saved.calls.mostRecent().args[0].regras.length).toBe(3);
  });

  it('identifies gaps and overlaps before submission', async () => {
    await TestBed.configureTestingModule({ imports: [PolicyEditor] }).compileComponents();
    const fixture = TestBed.createComponent(PolicyEditor);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.form.regras[1].pontuacaoMinima = 40;
    expect(component.issues().join(' ')).toContain('Falta uma decisão');

    component.form.regras[1].pontuacaoMinima = 20;
    expect(component.issues().join(' ')).toContain('sobreposição');
  });
});
