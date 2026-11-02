package br.com.documentshield.config;

import br.com.documentshield.domain.entity.RegraDeteccao;
import br.com.documentshield.domain.enums.*;
import br.com.documentshield.repository.RegraDeteccaoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class RuleBootstrap implements CommandLineRunner {
    private final RegraDeteccaoRepository repo;

    public RuleBootstrap(RegraDeteccaoRepository r) {
        repo = r;
    }

    @Override
    @Transactional
    public void run(String... a) {
        List<Seed> s = List.of(
                new Seed("DS-001", "Fonte minúscula", TipoOcorrencia.FONTE_MINUSCULA, NivelRisco.BAIXO, 12,
                        "{\"limitePt\":2}"),
                new Seed("DS-002", "Texto potencialmente invisível", TipoOcorrencia.TEXTO_INVISIVEL, NivelRisco.MEDIO,
                        28, "{\"opacidadeMaxima\":0.05}"),
                new Seed("DS-003", "Texto fora da página", TipoOcorrencia.TEXTO_FORA_DA_PAGINA, NivelRisco.MEDIO, 24,
                        "{}"),
                new Seed("DS-004", "Unicode suspeito", TipoOcorrencia.UNICODE_SUSPEITO, NivelRisco.MEDIO, 22, "{}"),
                new Seed("DS-005", "Padrões de Prompt Injection", TipoOcorrencia.PROMPT_INJECTION, NivelRisco.ALTO, 55,
                        "{\"idiomas\":[\"pt\",\"en\"]}"),
                new Seed("DS-006", "Metadados suspeitos", TipoOcorrencia.METADADO_SUSPEITO, NivelRisco.ALTO, 45, "{}"),
                new Seed("DS-007", "Alteração de papel", TipoOcorrencia.INSTRUCAO_IA, NivelRisco.ALTO, 48, "{}"),
                new Seed("DS-008", "Tentativa de ocultação", TipoOcorrencia.TEXTO_OFUSCADO, NivelRisco.ALTO, 50, "{}"));
        for (Seed x : s)
            if (repo.findByCodigo(x.code).isEmpty())
                repo.save(RegraDeteccao.builder().codigo(x.code).nome(x.name)
                        .descricao("Regra nativa " + x.code + " do motor Document Shield").tipoOcorrencia(x.type)
                        .nivelRiscoPadrao(x.risk).pontuacaoBase(x.score).parametrosJson(x.json).ativa(true)
                        .versao("1.0.0").build());
    }

    private record Seed(String code, String name, TipoOcorrencia type, NivelRisco risk, int score, String json) {
    }
}
