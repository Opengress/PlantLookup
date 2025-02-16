package net.opengress.plantlookup.utils;

import static java.text.Normalizer.*;

public class StringUtils {
    public static String clean(String input) {
        String normalized = normalize(input, Form.NFD);
        return normalized.replaceAll("\\p{M}", ""); // Remove diacritical marks
    }
}
