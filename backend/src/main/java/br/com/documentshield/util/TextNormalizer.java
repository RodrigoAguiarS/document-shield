package br.com.documentshield.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null)
            return "";
        return Normalizer.normalize(Normalizer.normalize(value, Normalizer.Form.NFKC), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[\\p{Cf}\\p{Cc}]", " ")
                .replaceAll("[^\\p{L}\\p{N}:]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String excerpt(String s, int start, int end) {
        if (s == null)
            return null;
        return s.substring(Math.max(0, start - 60), Math.min(s.length(), end + 60));
    }
}
