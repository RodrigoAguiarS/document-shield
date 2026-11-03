import { Component, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  PolicyDecision,
  PolicyType,
  SavePolicyRequest,
  SecurityPolicyDto,
} from '../../core/models/api.models';

interface EditableRule {
  pontuacaoMinima: number;
  pontuacaoMaxima: number;
  decisao: PolicyDecision;
  descricao: string | null;
  prioridade: number;
}

@Component({
  selector: 'app-policy-editor',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './policy-editor.html',
  styleUrl: './policy-editor.css',
})
export class PolicyEditor {
  readonly policy = input<SecurityPolicyDto | null>(null);
  readonly savePolicy = output<SavePolicyRequest>();
  readonly cancelEditor = output<void>();

  form: SavePolicyRequest = this.emptyForm();

  constructor() {
    effect(() => {
      const policy = this.policy();
      this.form = policy
        ? {
            nome: policy.nome,
            descricao: policy.descricao,
            tipo: policy.tipo,
            regras: policy.regras.map((rule) => ({
              pontuacaoMinima: rule.pontuacaoMinima,
              pontuacaoMaxima: rule.pontuacaoMaxima,
              decisao: rule.decisao,
              descricao: rule.descricao,
              prioridade: rule.prioridade,
            })),
          }
        : this.emptyForm();
    });
  }

  issues(): string[] {
    if (!this.form.regras.length) return ['Adicione pelo menos um intervalo.'];
    const ordered = [...this.form.regras].sort(
      (left, right) => left.pontuacaoMinima - right.pontuacaoMinima,
    );
    const issues: string[] = [];
    let expected = 0;
    for (const rule of ordered) {
      if (rule.pontuacaoMinima < 0 || rule.pontuacaoMaxima > 100) {
        issues.push('As pontuações devem permanecer entre 0 e 100.');
      }
      if (rule.pontuacaoMinima > rule.pontuacaoMaxima) {
        issues.push(`O intervalo ${rule.pontuacaoMinima}–${rule.pontuacaoMaxima} está invertido.`);
      } else if (rule.pontuacaoMinima < expected) {
        issues.push(`Há sobreposição a partir da pontuação ${rule.pontuacaoMinima}.`);
      } else if (rule.pontuacaoMinima > expected) {
        issues.push(`Falta uma decisão entre ${expected} e ${rule.pontuacaoMinima - 1}.`);
      }
      expected = Math.max(expected, rule.pontuacaoMaxima + 1);
    }
    if (expected <= 100) issues.push(`Falta uma decisão entre ${expected} e 100.`);
    return [...new Set(issues)];
  }

  addRule(): void {
    const maximum = this.form.regras.reduce(
      (value, rule) => Math.max(value, rule.pontuacaoMaxima),
      -1,
    );
    const minimum = Math.min(100, maximum + 1);
    this.form.regras.push({
      pontuacaoMinima: minimum,
      pontuacaoMaxima: 100,
      decisao: 'BLOCK',
      descricao: null,
      prioridade: this.form.regras.length + 1,
    });
  }

  removeRule(index: number): void {
    this.form.regras.splice(index, 1);
    this.form.regras.forEach((rule, position) => (rule.prioridade = position + 1));
  }

  submit(): void {
    if (!this.form.nome.trim() || this.issues().length) return;
    this.savePolicy.emit({
      ...this.form,
      nome: this.form.nome.trim(),
      descricao: this.form.descricao?.trim() || null,
      regras: this.form.regras.map((rule, index) => ({ ...rule, prioridade: index + 1 })),
    });
  }

  private emptyForm(): SavePolicyRequest {
    const rules: EditableRule[] = [
      { pontuacaoMinima: 0, pontuacaoMaxima: 29, decisao: 'ALLOW', descricao: null, prioridade: 1 },
      {
        pontuacaoMinima: 30,
        pontuacaoMaxima: 49,
        decisao: 'REVIEW',
        descricao: null,
        prioridade: 2,
      },
      {
        pontuacaoMinima: 50,
        pontuacaoMaxima: 100,
        decisao: 'BLOCK',
        descricao: null,
        prioridade: 3,
      },
    ];
    return { nome: '', descricao: null, tipo: 'INTEGRACAO' as PolicyType, regras: rules };
  }
}
