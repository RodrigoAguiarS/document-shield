package br.com.documentshield.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.documentshield.domain.enums.PolicyDecision;
import br.com.documentshield.domain.enums.PolicyType;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PolicyEngineTest {
    private final PolicyValidationService validation = new PolicyValidationService();
    private final PolicyEngine engine = new PolicyEngine(validation);

    @Test
    void evaluatesDefaultPolicyAtAllExactBoundariesDeterministically() {
        SecurityPolicySnapshot policy = policy(List.of(
                rule(0, 29, PolicyDecision.ALLOW),
                rule(30, 49, PolicyDecision.REVIEW),
                rule(50, 100, PolicyDecision.BLOCK)));

        assertThat(engine.evaluate(policy, 0).decision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(engine.evaluate(policy, 29).decision()).isEqualTo(PolicyDecision.ALLOW);
        assertThat(engine.evaluate(policy, 30).decision()).isEqualTo(PolicyDecision.REVIEW);
        assertThat(engine.evaluate(policy, 49).decision()).isEqualTo(PolicyDecision.REVIEW);
        assertThat(engine.evaluate(policy, 50).decision()).isEqualTo(PolicyDecision.BLOCK);
        assertThat(engine.evaluate(policy, 100).decision()).isEqualTo(PolicyDecision.BLOCK);
        assertThat(engine.evaluate(policy, 50)).isEqualTo(engine.evaluate(policy, 50));
    }

    @Test
    void rejectsOverlapGapOutOfRangeAndInvalidScores() {
        assertThatThrownBy(() -> validation.validate(policy(List.of(
                        rule(0, 30, PolicyDecision.ALLOW),
                        rule(25, 50, PolicyDecision.REVIEW),
                        rule(51, 100, PolicyDecision.BLOCK)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sobrepostos");
        assertThatThrownBy(() -> validation.validate(
                        policy(List.of(rule(0, 29, PolicyDecision.ALLOW), rule(40, 100, PolicyDecision.BLOCK)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem decisão");
        assertThatThrownBy(() -> validation.validate(policy(List.of(rule(-1, 100, PolicyDecision.BLOCK)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 0 e 100");
        assertThatThrownBy(() -> engine.evaluate(policy(List.of(rule(0, 100, PolicyDecision.ALLOW))), 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 0 e 100");
    }

    @Test
    void remainsDeterministicUnderConcurrentEvaluation() {
        SecurityPolicySnapshot policy = policy(List.of(
                rule(0, 29, PolicyDecision.ALLOW),
                rule(30, 49, PolicyDecision.REVIEW),
                rule(50, 100, PolicyDecision.BLOCK)));

        assertThat(IntStream.range(0, 1_000)
                        .parallel()
                        .mapToObj(ignored -> engine.evaluate(policy, 49).decision())
                        .distinct())
                .containsExactly(PolicyDecision.REVIEW);
    }

    private SecurityPolicySnapshot policy(List<PolicyRule> rules) {
        return new SecurityPolicySnapshot(UUID.randomUUID(), UUID.randomUUID(), "Teste", 1, PolicyType.GLOBAL, rules);
    }

    private PolicyRule rule(int min, int max, PolicyDecision decision) {
        return new PolicyRule(min, max, decision, null, min);
    }
}
