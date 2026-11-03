package br.com.documentshield.detection.rule;

import br.com.documentshield.detection.model.DetectionFinding;
import br.com.documentshield.detection.model.EvidenceDraft;
import br.com.documentshield.detection.model.PdfTextElement;
import br.com.documentshield.domain.enums.NivelRisco;
import br.com.documentshield.domain.enums.TipoEvidencia;
import br.com.documentshield.domain.enums.TipoOcorrencia;
import br.com.documentshield.util.TextNormalizer;
import java.util.List;

final class RuleSupport {
    private RuleSupport() {}

    static DetectionFinding element(
            String code,
            TipoOcorrencia type,
            NivelRisco risk,
            String title,
            String desc,
            PdfTextElement e,
            int score,
            double confidence,
            TipoEvidencia evidenceType,
            String detail) {
        return new DetectionFinding(
                code,
                type,
                risk,
                title,
                desc,
                e.text(),
                TextNormalizer.normalize(e.text()),
                e.page(),
                e.x(),
                e.y(),
                e.width(),
                e.height(),
                e.pageWidth(),
                e.pageHeight(),
                e.rotation(),
                "PDFBOX_ROTATED_CROP_TOP_LEFT",
                true,
                e.font(),
                e.fontSize(),
                e.color(),
                e.opacity(),
                score,
                confidence,
                List.of(new EvidenceDraft(evidenceType, detail, e.text(), null, desc, e.page())));
    }

    static DetectionFinding text(
            String code,
            TipoOcorrencia type,
            NivelRisco risk,
            String title,
            String desc,
            String content,
            int score,
            double confidence,
            TipoEvidencia evidenceType,
            Integer page) {
        return new DetectionFinding(
                code,
                type,
                risk,
                title,
                desc,
                content,
                TextNormalizer.normalize(content),
                page,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                score,
                confidence,
                List.of(new EvidenceDraft(
                        evidenceType,
                        desc,
                        content,
                        null,
                        "Correspondência após normalização NFKC e análise de padrões",
                        page)));
    }
}
