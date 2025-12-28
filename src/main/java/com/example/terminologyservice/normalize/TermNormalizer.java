package com.example.terminologyservice.normalize;

import java.util.Map;

public class TermNormalizer {

    private static final Map<String, String> ROMAN = Map.of(
            "i", "1",
            "ii", "2",
            "iii", "3",
            "iv", "4",
            "v", "5"
    );

    public static String normalize(String text) {
        if (text == null) return "";

        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD);
        text = text.toLowerCase();
        text = text.replaceAll("\\([^)]*\\)", "");
        text = text.replaceAll("[-_/]", " ");
        text = text.replaceAll("[^a-z0-9\\s]", "");
        text = text.replaceAll("\\s+", " ").trim();

        // Roman numerals → digits (token-based)
        for (var entry : ROMAN.entrySet()) {
            text = text.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        return text;
    }
}
