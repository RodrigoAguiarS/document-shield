package br.com.documentshield.detection.extractor;

import br.com.documentshield.detection.model.PdfExtractionResult;
import br.com.documentshield.detection.model.PdfPageMetadata;
import br.com.documentshield.detection.model.PdfTextElement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxDocumentExtractor implements PdfDocumentExtractor {
    private final int pageLimit;
    private final int charLimit;

    public PdfBoxDocumentExtractor(
            @Value("${document-shield.processing.page-limit:500}") int pageLimit,
            @Value("${document-shield.processing.text-character-limit:5000000}") int charLimit) {
        this.pageLimit = pageLimit;
        this.charLimit = charLimit;
    }

    @Override
    public PdfExtractionResult extract(Path path) throws IOException {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            if (doc.isEncrypted()) {
                throw new IOException("PDF protegido por senha não é suportado");
            }
            if (doc.getNumberOfPages() > pageLimit) {
                throw new IOException("PDF excede o limite de páginas: " + pageLimit);
            }
            List<PdfPageMetadata> pages = new ArrayList<>();
            int n = 1;
            for (PDPage p : doc.getPages()) {
                PDRectangle b = p.getCropBox();
                int rotation = normalizedRotation(p.getRotation());
                boolean swapsAxes = rotation == 90 || rotation == 270;
                pages.add(new PdfPageMetadata(
                        n++,
                        swapsAxes ? b.getHeight() : b.getWidth(),
                        swapsAxes ? b.getWidth() : b.getHeight(),
                        rotation));
            }
            PositionStripper stripper = new PositionStripper(charLimit);
            String text = stripper.getText(doc);
            PDDocumentInformation i = doc.getDocumentInformation();
            Map<String, String> metadata = new LinkedHashMap<>();
            put(metadata, "title", i.getTitle());
            put(metadata, "author", i.getAuthor());
            put(metadata, "subject", i.getSubject());
            put(metadata, "keywords", i.getKeywords());
            put(metadata, "creator", i.getCreator());
            put(metadata, "producer", i.getProducer());
            for (String key : i.getMetadataKeys()) {
                put(metadata, key, i.getCustomMetadataValue(key));
            }
            return new PdfExtractionResult(stripper.elements, pages, metadata, text);
        } catch (InvalidPasswordException e) {
            throw new IOException("PDF protegido por senha não é suportado", e);
        }
    }

    @Override
    public int pageCount(Path path) throws IOException {
        try (PDDocument d = Loader.loadPDF(path.toFile())) {
            if (d.isEncrypted()) {
                throw new IOException("PDF protegido por senha não é suportado");
            }
            return d.getNumberOfPages();
        } catch (InvalidPasswordException e) {
            throw new IOException("PDF protegido por senha não é suportado", e);
        }
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v);
        }
    }

    private static final class PositionStripper extends PDFTextStripper {
        private final List<PdfTextElement> elements = new ArrayList<>();
        private final int charLimit;
        private int chars;

        PositionStripper(int charLimit) throws IOException {
            this.charLimit = charLimit;
            setSortByPosition(true);
        }

        @Override
        protected void processTextPosition(TextPosition t) {
            String value = t.getUnicode();
            if (value == null || value.isEmpty()) {
                return;
            }
            chars += value.length();
            if (chars > charLimit) {
                throw new CharacterLimitException();
            }
            PDPage page = getCurrentPage();
            PDRectangle box = page.getCropBox();
            int rotation = normalizedRotation(page.getRotation());
            boolean swapsAxes = rotation == 90 || rotation == 270;
            float height = t.getHeight();
            String color = null;
            Float opacity = null;
            boolean invisible = false;
            try {
                PDColor c = getGraphicsState().getNonStrokingColor();
                color = String.format("#%06X", c.toRGB() & 0xFFFFFF);
                opacity = (float) getGraphicsState().getNonStrokeAlphaConstant();
                invisible = getGraphicsState().getTextState().getRenderingMode() == RenderingMode.NEITHER;
            } catch (Exception ignored) {
            }
            String font = t.getFont() == null ? null : t.getFont().getName();
            elements.add(new PdfTextElement(
                    value,
                    getCurrentPageNo(),
                    t.getX(),
                    t.getY() - height,
                    t.getWidth(),
                    height,
                    font,
                    t.getFontSizeInPt(),
                    color,
                    opacity,
                    invisible,
                    swapsAxes ? box.getHeight() : box.getWidth(),
                    swapsAxes ? box.getWidth() : box.getHeight(),
                    rotation));
            super.processTextPosition(t);
        }
    }

    private static int normalizedRotation(int rotation) {
        return Math.floorMod(rotation, 360);
    }

    private static final class CharacterLimitException extends RuntimeException {}
}
