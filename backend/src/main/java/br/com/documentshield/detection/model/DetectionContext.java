package br.com.documentshield.detection.model;

import java.nio.file.Path;
import java.util.UUID;

public record DetectionContext(UUID documentId, Path path, PdfExtractionResult extraction) {}
