package br.com.documentshield.dto.request;

import br.com.documentshield.domain.enums.PolicyDecision;
import br.com.documentshield.domain.enums.PolicyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class PolicyRequests {
    private PolicyRequests() {}

    public record RuleRequest(
            @Min(0) @Max(100) int pontuacaoMinima,
            @Min(0) @Max(100) int pontuacaoMaxima,
            @NotNull PolicyDecision decisao,
            @Size(max = 1000) String descricao,
            @Min(0) int prioridade) {}

    public record SavePolicyRequest(
            @NotBlank @Size(max = 255) String nome,
            @Size(max = 1000) String descricao,
            @NotNull PolicyType tipo,
            @NotEmpty List<@Valid RuleRequest> regras) {}

    public record SimulatePolicyRequest(@Min(0) @Max(100) int pontuacao) {}

    public record BindPolicyRequest(@NotNull UUID politicaId) {}
}
