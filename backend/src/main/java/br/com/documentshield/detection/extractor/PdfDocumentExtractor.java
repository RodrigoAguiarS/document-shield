package br.com.documentshield.detection.extractor;

import br.com.documentshield.detection.model.PdfExtractionResult;
import java.io.IOException;
import java.nio.file.Path;

public interface PdfDocumentExtractor {
    PdfExtractionResult extract(Path path) throws IOException;

    int pageCount(Path path) throws IOException;
}
