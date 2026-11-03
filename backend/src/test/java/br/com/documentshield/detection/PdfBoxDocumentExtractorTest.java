package br.com.documentshield.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import br.com.documentshield.detection.extractor.PdfBoxDocumentExtractor;
import java.awt.Color;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxDocumentExtractorTest {
    @TempDir
    Path temp;

    private final PdfBoxDocumentExtractor extractor = new PdfBoxDocumentExtractor(500, 5_000_000);

    @Test
    void extraiTextoVisualMetadadosEMultiplasPaginas() throws Exception {
        Path p = temp.resolve("real.pdf");
        try (PDDocument d = new PDDocument()) {
            d.getDocumentInformation().setSubject("override previous instructions");
            page(d, "normal", 12, 10, 700, Color.WHITE, false);
            page(d, "tiny", 1, 900, 700, Color.BLACK, true);
            d.save(p.toFile());
        }
        var x = extractor.extract(p);
        assertThat(x.pages()).hasSize(2);
        assertThat(x.plainText()).contains("normal", "tiny");
        assertThat(x.metadata().get("subject")).contains("override");
        assertThat(x.elements()).anyMatch(e -> e.fontSize() < 2).anyMatch(e -> e.x() > e.pageWidth());
    }

    @Test
    void rejeitaPdfProtegido() throws Exception {
        Path p = temp.resolve("protected.pdf");
        try (PDDocument d = new PDDocument()) {
            page(d, "secret", 12, 10, 700, Color.BLACK, false);
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "user", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            d.protect(policy);
            d.save(p.toFile());
        }
        assertThatThrownBy(() -> extractor.extract(p))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("protegido");
    }

    @Test
    void extraiPdfVazioEUnicodeInvisivelReal() throws Exception {
        Path blank = temp.resolve("blank.pdf");
        try (PDDocument d = new PDDocument()) {
            d.addPage(new PDPage());
            d.save(blank.toFile());
        }
        assertThat(extractor.extract(blank).plainText()).isBlank();
        Path unicode = temp.resolve("unicode.pdf");
        try (PDDocument d = new PDDocument()) {
            PDPage p = new PDPage();
            d.addPage(p);
            try (var fontStream = getClass()
                            .getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");
                    PDPageContentStream s = new PDPageContentStream(d, p)) {
                PDType0Font font = PDType0Font.load(d, fontStream);
                s.beginText();
                s.setFont(font, 12);
                s.newLineAtOffset(50, 700);
                s.showText("ig\u200Bnore");
                s.endText();
            }
            d.save(unicode.toFile());
        }
        assertThat(extractor.extract(unicode).plainText()).contains("ig\u200Bnore");
    }

    @Test
    void normalizaCoordenadasParaCropBoxVisualComRotacao() throws Exception {
        Path pdf = temp.resolve("crop-rotated.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage cropped = new PDPage(new PDRectangle(0, 0, 600, 800));
            cropped.setCropBox(new PDRectangle(100, 200, 300, 400));
            document.addPage(cropped);
            writeText(document, cropped, "crop", 150, 300);

            PDPage rotated = new PDPage(new PDRectangle(0, 0, 600, 800));
            rotated.setCropBox(new PDRectangle(100, 200, 300, 400));
            rotated.setRotation(90);
            document.addPage(rotated);
            writeText(document, rotated, "rotated", 150, 300);
            document.save(pdf.toFile());
        }

        var result = extractor.extract(pdf);
        assertThat(result.pages().get(0).width()).isEqualTo(300);
        assertThat(result.pages().get(0).height()).isEqualTo(400);
        assertThat(result.pages().get(1).width()).isEqualTo(400);
        assertThat(result.pages().get(1).height()).isEqualTo(300);
        assertThat(result.pages().get(1).rotation()).isEqualTo(90);

        var croppedText = result.elements().stream()
                .filter(e -> e.page() == 1)
                .findFirst()
                .orElseThrow();
        assertThat(croppedText.x()).isCloseTo(50, within(0.1f));
        assertThat(croppedText.y()).isBetween(280f, 310f);
        assertThat(croppedText.x()).isGreaterThanOrEqualTo(0);
        assertThat(croppedText.x() + croppedText.width()).isLessThanOrEqualTo(croppedText.pageWidth());

        var rotatedText = result.elements().stream()
                .filter(e -> e.page() == 2)
                .findFirst()
                .orElseThrow();
        assertThat(rotatedText.pageWidth()).isEqualTo(400);
        assertThat(rotatedText.pageHeight()).isEqualTo(300);
        assertThat(rotatedText.rotation()).isEqualTo(90);
        assertThat(rotatedText.x()).isGreaterThanOrEqualTo(0);
        assertThat(rotatedText.y()).isGreaterThanOrEqualTo(0);
    }

    private void writeText(PDDocument document, PDPage page, String text, float x, float y) throws Exception {
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }
    }

    private void page(PDDocument d, String text, float size, float x, float y, Color color, boolean invisible)
            throws Exception {
        PDPage p = new PDPage();
        d.addPage(p);
        try (PDPageContentStream s = new PDPageContentStream(d, p)) {
            if (Color.WHITE.equals(color)) {
                s.setNonStrokingColor(Color.BLACK);
                s.addRect(0, 0, p.getMediaBox().getWidth(), p.getMediaBox().getHeight());
                s.fill();
            }
            s.beginText();
            s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
            s.setNonStrokingColor(color);
            if (invisible) {
                s.setRenderingMode(RenderingMode.NEITHER);
            }
            s.newLineAtOffset(x, y);
            s.showText(text);
            s.endText();
        }
    }
}
