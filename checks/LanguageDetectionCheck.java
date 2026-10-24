package uk.co.zazuzin.translate.net;

import java.util.Objects;

public final class LanguageDetectionCheck {
    public static void main(String[] args) {
        check("Привет, как дела?", "ru");
        check("Привіт, як справи?", "uk");
        check("Hello, please", "en");
        check("Bonjour, merci", "fr");
        check("Hola, gracias", "es");
        check("Hallo, danke", "de");
        check("Bonjour, comment allez-vous aujourd’hui ?", "fr");
        check("Hello, how are you today?", "en");
        check("Hello", "en");
        check("Olá, obrigado", "pt");
        check("Ciao, grazie", "it");
        check("Bedankt, vandaag", "nl");
        check("Cześć, dziękuję", "pl");
        check("Merhaba, teşekkürler", "tr");
        check("今日は元気ですか", "ja");
        check("안녕하세요", "ko");
        check("你好", "zh-CN");
        check("مرحبا", "ar");
        check("سلام، چطوری", "fa");
        check("שלום", "he");
        check("नमस्ते", "hi");
        check("สวัสดี", "th");
        check("مرحباً", "ar");
        check("hi", null);
        check("123 😀", null);
        for (String language : new String[]{"de", "fr", "es", "it", "pt", "ru", "ja", "ko", "ar"}) {
            if (!LanguageRouting.replyTarget(language, "mymemory").equals(language))
                throw new AssertionError("Reply target changed: " + language);
        }
        if (!LanguageRouting.replyTarget("zh-TW", "deepl").equals("zh-Hant")
            || !LanguageRouting.replyTarget("zh-CN", "libretranslate").equals("zh"))
            throw new AssertionError("Provider reply target mapping failed");
        if (ReplyValidation.error("Hallo! Danke für deine Hilfe.") != null
            || ReplyValidation.error("你好！") != null || ReplyValidation.error("مرحبا") != null
            || ReplyValidation.error("") == null || ReplyValidation.error("a".repeat(257)) == null
            || ReplyValidation.error("hello\nworld") == null)
            throw new AssertionError("Outgoing translation validation failed");
        if (!LanguageRouting.targetForMinecraft("fr_fr", "mymemory").equals("fr")
            || !LanguageRouting.sameLanguage("fr-FR", "fr")
            || LanguageRouting.sameLanguage("en", "fr"))
            throw new AssertionError("French target routing failed");
        if (!LanguageRouting.targetForMinecraft("en_us", "mymemory").equals("en")
            || !LanguageRouting.targetForMinecraft("zh_tw", "mymemory").equals("zh-TW")
            || !LanguageRouting.targetForMinecraft("pt_br", "deepl").equals("pt-BR"))
            throw new AssertionError("Minecraft locale mapping failed");
        if (TranslationOutput.accepts("ሰላም ዛሬ እንዴት ነህ", "en"))
            throw new AssertionError("MyMemory's German to English response is Amharic");
        if (!TranslationOutput.accepts("How are you doing today?", "en"))
            throw new AssertionError("Valid English translation was rejected");
        if (TranslationOutput.accepts("", "fr")
            || !TranslationOutput.join("Bonjour", ',', "Tu vas bien aujourd’hui ?", "fr")
                .equals("Bonjour, tu vas bien aujourd’hui ?"))
            throw new AssertionError("French fallback failed");
        if (!TranslationOutput.accepts("今日は元気ですか", "ja"))
            throw new AssertionError("Valid Japanese translation was rejected");
        if (!TranslationOutput.accepts("Привет, как дела?", "ru"))
            throw new AssertionError("Valid Russian translation was rejected");
        int split = TranslationOutput.splitAfterPunctuation("Hallo, wie geht es dir heute?");
        if (split != 5 || !TranslationOutput.join("Hello", ',', "How are you doing today?", "en")
            .equals("Hello, how are you doing today?"))
            throw new AssertionError("German clause fallback failed");
    }

    private static void check(String input, String expected) {
        String actual = LanguageDetection.detect(input);
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " for '" + input + "' but got " + actual);
        }
    }
}

