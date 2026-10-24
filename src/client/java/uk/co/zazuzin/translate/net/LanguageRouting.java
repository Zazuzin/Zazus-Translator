package uk.co.zazuzin.translate.net;

import java.util.Locale;

/** Maps Minecraft locale names to provider language codes. */
public final class LanguageRouting {
    private LanguageRouting() {}

    public static String targetForMinecraft(String minecraftCode, String provider) {
        if (minecraftCode == null || minecraftCode.isBlank()) return "en";
        String locale = minecraftCode.toLowerCase(Locale.ROOT).replace('-', '_');
        String language = locale.split("_", 2)[0];
        return switch (language) {
            case "nb", "nn" -> "no";
            case "zh" -> {
                boolean traditional = locale.equals("zh_tw") || locale.equals("zh_hk");
                yield switch (provider) {
                    case "deepl" -> traditional ? "zh-Hant" : "zh-Hans";
                    case "libretranslate" -> "zh";
                    default -> traditional ? "zh-TW" : "zh-CN";
                };
            }
            case "pt" -> {
                if (provider.equals("deepl")) yield locale.equals("pt_br") ? "pt-BR" : "pt-PT";
                if (provider.equals("mymemory") && locale.equals("pt_br")) yield "pt-BR";
                yield "pt";
            }
            case "en" -> provider.equals("deepl")
                ? locale.equals("en_gb") ? "en-GB" : "en-US" : "en";
            default -> language;
        };
    }

    public static boolean sameLanguage(String source, String target) {
        if (source == null || target == null || source.isBlank() || target.isBlank()
            || source.equalsIgnoreCase("auto")) return false;
        String first = source.toLowerCase(Locale.ROOT).replace('_', '-').split("-")[0];
        String second = target.toLowerCase(Locale.ROOT).replace('_', '-').split("-")[0];
        if (first.equals("nb") || first.equals("nn")) first = "no";
        if (second.equals("nb") || second.equals("nn")) second = "no";
        return first.equals(second);
    }

    public static String replyTarget(String code, String provider) {
        String normalized = code.toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.startsWith("zh")) {
            boolean traditional = normalized.endsWith("tw") || normalized.endsWith("hk")
                || normalized.endsWith("hant");
            if (provider.equals("deepl")) return traditional ? "zh-Hant" : "zh-Hans";
            if (provider.equals("libretranslate")) return "zh";
            return traditional ? "zh-TW" : "zh-CN";
        }
        if (provider.equals("libretranslate")) return normalized.split("-")[0];
        return code;
    }
}

