package uk.co.zazuzin.translate.net;

/** Minecraft chat limits, checked after translation and before sending. */
public final class ReplyValidation {
    private ReplyValidation() {}

    public static String error(String text) {
        if (text == null || text.isBlank()) return "Translation is empty";
        if (text.length() > 256) return "Translation exceeds 256 characters; shorten your reply";
        if (text.codePoints().anyMatch(cp -> cp < 32 || cp == 127 || cp == 167))
            return "Translation contains characters Minecraft chat cannot send";
        return null;
    }
}

