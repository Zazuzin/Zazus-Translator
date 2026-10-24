package uk.co.zazuzin.translate.net;

import java.util.Locale;
import java.util.Set;

/** Rejects a provider response whose writing system plainly conflicts with the target. */
final class TranslationOutput {
    private static final Set<String> LATIN = Set.of(
        "en", "fr", "es", "de", "it", "pt", "nl", "pl", "tr", "cs",
        "ro", "sv", "da", "no", "fi", "hu", "id", "vi"
    );

    private TranslationOutput() {}

    static boolean accepts(String text, String target) {
        if (text == null || text.isBlank()) return false;
        // Never let formatting sequences supplied by an external provider affect chat.
        if (text.matches("(?s).*§[0-9a-fk-orA-FK-OR].*")) return false;
        String language = target.toLowerCase(Locale.ROOT).split("[-_]")[0];
        int expected = 0;
        int unexpected = 0;
        boolean known = LATIN.contains(language) || Set.of(
            "ru", "uk", "be", "kk", "zh", "ja", "ko", "ar", "fa", "ur",
            "hi", "mr", "el", "he", "th"
        ).contains(language);
        if (!known) return true;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (!Character.isLetter(codePoint)) continue;
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.COMMON || script == Character.UnicodeScript.INHERITED) continue;
            if (matches(script, language)) expected++;
            else unexpected++;
        }
        return expected > 0 && expected >= unexpected;
    }

    private static boolean matches(Character.UnicodeScript script, String language) {
        if (LATIN.contains(language)) return script == Character.UnicodeScript.LATIN;
        return switch (language) {
            case "ru", "uk", "be", "kk" -> script == Character.UnicodeScript.CYRILLIC;
            case "zh" -> script == Character.UnicodeScript.HAN;
            case "ja" -> script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA;
            case "ko" -> script == Character.UnicodeScript.HANGUL || script == Character.UnicodeScript.HAN;
            case "ar", "fa", "ur" -> script == Character.UnicodeScript.ARABIC;
            case "hi", "mr" -> script == Character.UnicodeScript.DEVANAGARI;
            case "el" -> script == Character.UnicodeScript.GREEK;
            case "he" -> script == Character.UnicodeScript.HEBREW;
            case "th" -> script == Character.UnicodeScript.THAI;
            default -> false;
        };
    }

    static int splitAfterPunctuation(String text) {
        for (int i = 1; i < text.length() - 2; i++) {
            if (",;!?".indexOf(text.charAt(i)) >= 0 && text.charAt(i + 1) == ' '
                && !text.substring(0, i).isBlank() && !text.substring(i + 2).isBlank()) return i;
        }
        return -1;
    }

    static String join(String left, char punctuation, String right, String target) {
        if (punctuation == ',' && (target.equalsIgnoreCase("en") || target.equalsIgnoreCase("fr"))
            && !right.isEmpty()
            && right.charAt(0) >= 'A' && right.charAt(0) <= 'Z') {
            right = Character.toLowerCase(right.charAt(0)) + right.substring(1);
        }
        return left.stripTrailing() + punctuation + " " + right.stripLeading();
    }
}

