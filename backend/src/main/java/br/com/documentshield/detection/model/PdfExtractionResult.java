package br.com.documentshield.detection.model;

import java.util.List;
import java.util.Map;

public record PdfExtractionResult(
        List<PdfTextElement> elements, List<PdfPageMetadata> pages, Map<String, String> metadata, String plainText) {
    public PdfExtractionResult {
        elements = List.copyOf(elements);
        pages = List.copyOf(pages);
        metadata = Map.copyOf(metadata);
    }
}
