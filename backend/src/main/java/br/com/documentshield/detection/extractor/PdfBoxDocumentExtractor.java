package br.com.documentshield.detection.extractor;

import br.com.documentshield.detection.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Component
public class PdfBoxDocumentExtractor implements PdfDocumentExtractor {
    private final int pageLimit;
    private final int charLimit;

    public PdfBoxDocumentExtractor(@Value("${document-shield.processing.page-limit:500}") int pageLimit,
            @Value("${document-shield.processing.text-character-limit:5000000}") int charLimit) {
        this.pageLimit = pageLimit;
        this.charLimit = charLimit;
    }

    @Override
    public PdfExtractionResult extract(Path path) throws IOException {
        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            if (doc.isEncrypted())
                throw new IOException("PDF protegido por senha não é suportado");
            if (doc.getNumberOfPages() > pageLimit)
                throw new IOException("PDF excede o limite de páginas: " + pageLimit);
            List<PdfPageMetadata> pages = new ArrayList<>();
            int n = 1;
            for (PDPage p : doc.getPages()) {
                PDRectangle b = p.getCropBox();
                pages.add(new PdfPageMetadata(n++, b.getWidth(), b.getHeight(), p.getRotation()));
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
            for (String key : i.getMetadataKeys())
                put(metadata, key, i.getCustomMetadataValue(key));
            return new PdfExtractionResult(stripper.elements, pages, metadata, text);
        } catch (InvalidPasswordException e) {
            throw new IOException("PDF protegido por senha não é suportado", e);
        }
    }

    @Override
    public int pageCount(Path path) throws IOException {
        try (PDDocument d = Loader.loadPDF(path.toFile())) {
            if (d.isEncrypted())
                throw new IOException("PDF protegido por senha não é suportado");
            return d.getNumberOfPages();
        } catch (InvalidPasswordException e) {
            throw new IOException("PDF protegido por senha não é suportado", e);
        }
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank())
            m.put(k, v);
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
            if (value == null || value.isEmpty())
                return;
            chars += value.length();
            if (chars > charLimit)
                throw new CharacterLimitException();
            PDPage page = getCurrentPage();
            PDRectangle box = page.getCropBox();
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
            elements.add(new PdfTextElement(value, getCurrentPageNo(), t.getXDirAdj(), t.getYDirAdj(),
                    t.getWidthDirAdj(), t.getHeightDir(), font, t.getFontSizeInPt(), color, opacity, invisible,
                    box.getWidth(), box.getHeight(), page.getRotation()));
            super.processTextPosition(t);
        }
    }

    private static final class CharacterLimitException extends RuntimeException {
    }
}
