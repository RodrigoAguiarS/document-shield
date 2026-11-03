package br.com.documentshield.dto.response;

import br.com.documentshield.domain.enums.PolicyAuditOperation;
import br.com.documentshield.domain.enums.PolicyDecision;
import br.com.documentshield.domain.enums.PolicyStatus;
import br.com.documentshield.domain.enums.PolicyType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PolicyResponses {
    private PolicyResponses() {}

    public record Rule(
            UUID id,
            int pontuacaoMinima,
            int pontuacaoMaxima,
            PolicyDecision decisao,
            String descricao,
            int prioridade) {}

    public record Policy(
            UUID id,
            UUID policyKey,
            String nome,
            String descricao,
            PolicyType tipo,
            int versao,
            PolicyStatus status,
            Instant dataCriacao,
            Instant dataAtualizacao,
            Instant dataAtivacao,
            String identificadorCriador,
            long entityVersion,
            int quantidadeRegras,
            List<Rule> regras) {}

    public record Evaluation(
            UUID politicaId,
            UUID policyKey,
            String nome,
            int versao,
            PolicyType origem,
            PolicyDecision decisao,
            int pontuacaoAvaliada,
            Rule regraAplicada,
            String motivo) {}

    public record Binding(
            UUID integracaoId,
            UUID politicaId,
            String politicaNome,
            int versao,
            PolicyType origem,
            PolicyStatus status,
            Instant dataAtivacao,
            Instant dataVinculacao,
            boolean utilizaPoliticaGlobal) {}

    public record Audit(
            UUID id,
            UUID politicaId,
            UUID policyKey,
            Integer versao,
            PolicyAuditOperation operacao,
            String administrador,
            Instant dataEvento,
            String detalhes) {}
}
