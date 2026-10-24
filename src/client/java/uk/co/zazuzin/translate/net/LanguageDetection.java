package uk.co.zazuzin.translate.net;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Conservative offline fallback for providers that require an explicit source code.
 * A null result means that the text is too ambiguous to send safely.
 */
final class LanguageDetection {
    private static final Map<String, Set<String>> WORDS = Map.ofEntries(
        Map.entry("en", Set.of("the", "this", "that", "you", "your", "are", "what", "why", "where", "thanks", "please", "hello", "sorry")),
        Map.entry("fr", Set.of("bonjour", "merci", "vous", "avec", "pour", "dans", "est", "suis", "pas", "oui", "quoi", "pourquoi")),
        Map.entry("es", Set.of("hola", "gracias", "usted", "estoy", "estás", "porque", "pero", "también", "quiero", "buenos", "dónde")),
        Map.entry("de", Set.of("hallo", "danke", "ich", "nicht", "und", "mit", "für", "warum", "bitte", "bist", "heute")),
        Map.entry("pt", Set.of("olá", "obrigado", "obrigada", "você", "não", "estou", "também", "porque", "isso", "muito", "hoje")),
        Map.entry("it", Set.of("ciao", "grazie", "sono", "come", "perché", "questo", "oggi", "buongiorno", "prego", "ragazzi")),
        Map.entry("nl", Set.of("hallo", "bedankt", "niet", "jij", "voor", "waarom", "alsjeblieft", "goed", "vandaag", "zijn")),
        Map.entry("pl", Set.of("cześć", "dziękuję", "jestem", "dlaczego", "proszę", "dobrze", "dzisiaj", "nie", "tak", "jest")),
        Map.entry("tr", Set.of("merhaba", "teşekkürler", "nasılsın", "neden", "evet", "hayır", "için", "bugün", "benim", "çok")),
        Map.entry("cs", Set.of("ahoj", "děkuji", "prosím", "proč", "jsem", "dnes", "dobře", "ano", "ne", "máš")),
        Map.entry("ro", Set.of("salut", "mulțumesc", "bună", "astăzi", "pentru", "sunt", "foarte", "unde", "vreau")),
        Map.entry("sv", Set.of("hej", "tack", "inte", "varför", "idag", "också", "mycket", "du", "är", "jag")),
        Map.entry("da", Set.of("tak", "ikke", "hvorfor", "også", "meget", "jeg", "dig", "hvad", "godt")),
        Map.entry("no", Set.of("takk", "ikke", "hvorfor", "også", "veldig", "jeg", "deg", "hva", "bra")),
        Map.entry("fi", Set.of("kiitos", "hei", "miksi", "minä", "sinä", "olen", "tänään", "hyvä", "mitä")),
        Map.entry("hu", Set.of("szia", "köszönöm", "miért", "vagyok", "vagy", "ma", "igen", "nem", "hogyan")),
        Map.entry("id", Set.of("halo", "terima", "kasih", "saya", "kamu", "tidak", "kenapa", "apa", "dengan")),
        Map.entry("vi", Set.of("xin", "chào", "cảm", "ơn", "không", "tôi", "bạn", "tại", "sao", "hôm"))
    );

    private static final Map<String, String> GREETINGS = Map.ofEntries(
        Map.entry("hello", "en"), Map.entry("thanks", "en"),
        Map.entry("bonjour", "fr"), Map.entry("gracias", "es"),
        Map.entry("hola", "es"), Map.entry("danke", "de"),
        Map.entry("obrigado", "pt"), Map.entry("obrigada", "pt"),
        Map.entry("grazie", "it"), Map.entry("merhaba", "tr"),
        Map.entry("kiitos", "fi"), Map.entry("dziękuję", "pl"),
        Map.entry("děkuji", "cs"), Map.entry("mulțumesc", "ro"),
        Map.entry("köszönöm", "hu"), Map.entry("takk", "no")
    );

    private LanguageDetection() {}

    static String detect(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean latin = false;
        boolean cyrillic = false;
        boolean han = false;
        boolean arabic = false;
        boolean devanagari = false;

        for (int offset = 0; offset < lower.length();) {
            int cp = lower.codePointAt(offset);
            offset += Character.charCount(cp);
            switch (Character.UnicodeScript.of(cp)) {
                case HIRAGANA, KATAKANA -> { return "ja"; }
                case HANGUL -> { return "ko"; }
                case GREEK -> { return "el"; }
                case HEBREW -> { return "he"; }
                case THAI -> { return "th"; }
                case ARMENIAN -> { return "hy"; }
                case GEORGIAN -> { return "ka"; }
                case BENGALI -> { return "bn"; }
                case GUJARATI -> { return "gu"; }
                case GURMUKHI -> { return "pa"; }
                case TAMIL -> { return "ta"; }
                case TELUGU -> { return "te"; }
                case KANNADA -> { return "kn"; }
                case MALAYALAM -> { return "ml"; }
                case SINHALA -> { return "si"; }
                case MYANMAR -> { return "my"; }
                case KHMER -> { return "km"; }
                case LAO -> { return "lo"; }
                case DEVANAGARI -> devanagari = true;
                case ARABIC -> arabic = true;
                case HAN -> han = true;
                case CYRILLIC -> cyrillic = true;
                case LATIN -> latin = true;
                default -> {}
            }
        }
        if (devanagari) return lower.indexOf('ळ') >= 0 ? "mr" : "hi";
        if (arabic) {
            if (containsAny(lower, "ٹ", "ڈ", "ڑ", "ں", "ھ", "ے")) return "ur";
            return containsAny(lower, "پ", "چ", "ژ", "گ") ? "fa" : "ar";
        }
        if (han) return containsAny(lower, "這", "麼", "嗎", "們", "個", "謝") ? "zh-TW" : "zh-CN";
        if (cyrillic) {
            if (containsAny(lower, "і", "ї", "є", "ґ")) return "uk";
            if (containsAny(lower, "ў")) return "be";
            if (containsAny(lower, "ә", "ғ", "қ", "ң", "ө", "ұ", "ү", "һ")) return "kk";
            if (containsAny(lower, "ј", "љ", "њ", "ђ", "ћ", "џ")) return "sr";
            return "ru";
        }
        if (!latin) return null;

        if (containsAny(lower, "¿", "¡", "ñ")) return "es";
        if (containsAny(lower, "ã", "õ")) return "pt";
        if (containsAny(lower, "ß")) return "de";
        if (containsAny(lower, "ł", "ą", "ę", "ź", "ż")) return "pl";
        if (containsAny(lower, "ğ", "ı", "ş")) return "tr";
        if (containsAny(lower, "ț", "ș")) return "ro";
        if (containsAny(lower, "ơ", "ư", "đ", "ấ", "ệ")) return "vi";
        if (containsAny(lower, "ř", "ů", "ě")) return "cs";
        if (containsAny(lower, "ő", "ű")) return "hu";

        String[] words = lower.split("[^\\p{L}]+");
        if (words.length == 1 && GREETINGS.containsKey(words[0])) return GREETINGS.get(words[0]);
        Map<String, Integer> scores = new HashMap<>();
        for (String word : words) {
            if (word.length() < 2) continue;
            for (var entry : WORDS.entrySet()) {
                if (entry.getValue().contains(word)) scores.merge(entry.getKey(), 1, Integer::sum);
            }
        }
        String best = null;
        int highest = 0, runnerUp = 0;
        for (var entry : scores.entrySet()) {
            int score = entry.getValue();
            if (score > highest) {
                runnerUp = highest;
                highest = score;
                best = entry.getKey();
            } else if (score > runnerUp) {
                runnerUp = score;
            }
        }
        // A short or tied Latin-script phrase cannot be identified reliably.
        return highest >= 2 && highest > runnerUp ? best : null;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }
}

