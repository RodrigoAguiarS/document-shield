package br.com.documentshield.detection.model;

import java.util.*;

public record PdfExtractionResult(List<PdfTextElement> elements, List<PdfPageMetadata> pages,
        Map<String, String> metadata, String plainText) {
    public PdfExtractionResult {
        elements = List.copyOf(elements);
        pages = List.copyOf(pages);
        metadata = Map.copyOf(metadata);
    }
}
