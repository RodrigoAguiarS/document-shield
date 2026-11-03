package br.com.documentshield.detection.model;

public record PdfTextElement(
        String text,
        int page,
        float x,
        float y,
        float width,
        float height,
        String font,
        Float fontSize,
        String color,
        Float opacity,
        boolean renderingInvisible,
        float pageWidth,
        float pageHeight,
        int rotation) {}
