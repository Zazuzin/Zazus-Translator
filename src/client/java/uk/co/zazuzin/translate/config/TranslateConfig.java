package uk.co.zazuzin.translate.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import uk.co.zazuzin.translate.ZazuTranslateClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public record TranslateConfig(
    String endpoint,
    String apiKey,
    String sourceLanguage,
    String targetLanguage,
    boolean autoTranslateIncoming,
    int timeoutSeconds,
    String provider,
    String replyLanguage,
    String translationColor,
    boolean translationBold,
    String ignoredPlayers,
    boolean translateSystemMessages,
    boolean useMinecraftLanguage
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("zazu-translate.json");
    private static final String MYMEMORY = "https://api.mymemory.translated.net/get";
    private static final Set<String> COLORS = Set.of("red", "blue", "green", "yellow", "purple", "orange", "pink");

    public static TranslateConfig defaults() {
        return new TranslateConfig(MYMEMORY, "", "auto", "en", true, 15,
            "mymemory", "ru", "green", true, "", false, true);
    }

    public static TranslateConfig load() {
        TranslateConfig defaults = defaults();
        if (Files.notExists(PATH)) {
            defaults.save();
            return defaults;
        }
        try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("autoTranslateIncoming")) json.addProperty("autoTranslateIncoming", true);
            if (!json.has("translationBold")) json.addProperty("translationBold", true);
            if (!json.has("useMinecraftLanguage")) {
                // Existing non-English overrides stay custom; the old English default
                // follows Minecraft from this version onward.
                String oldTarget = json.has("targetLanguage")
                    ? json.get("targetLanguage").getAsString() : "en";
                json.addProperty("useMinecraftLanguage", oldTarget.equalsIgnoreCase("en"));
            }
            if (!json.has("provider")) {
                String endpoint = json.has("endpoint") ? json.get("endpoint").getAsString() : MYMEMORY;
                String key = json.has("apiKey") ? json.get("apiKey").getAsString() : "";
                boolean oldDefault = (endpoint.equals("https://libretranslate.com/translate")
                    || endpoint.equals("https://translate.googleapis.com/translate_a/single"))
                    && key.isBlank();
                String provider = oldDefault || endpoint.contains("mymemory.translated.net")
                    ? "mymemory" : endpoint.contains("deepl.com") ? "deepl" : "libretranslate";
                json.addProperty("provider", provider);
            }
            TranslateConfig parsed = GSON.fromJson(json, TranslateConfig.class);
            TranslateConfig result = sanitize(parsed);
            if (!result.equals(parsed)) result.save();
            return result;
        } catch (Exception exception) {
            ZazuTranslateClient.LOGGER.error("Could not read translation config", exception);
            return defaults;
        }
    }

    public static String defaultEndpoint(String provider) {
        return switch (provider) {
            case "deepl" -> "https://api-free.deepl.com/v2/translate";
            case "libretranslate" -> "https://libretranslate.com/translate";
            default -> MYMEMORY;
        };
    }

    public static TranslateConfig sanitize(TranslateConfig value) {
        TranslateConfig defaults = defaults();
        if (value == null) return defaults;
        String provider = clean(value.provider, "mymemory").toLowerCase(Locale.ROOT);
        if (!Set.of("mymemory", "libretranslate", "deepl").contains(provider)) provider = "mymemory";
        String endpoint = clean(value.endpoint, defaultEndpoint(provider));
        if (provider.equals("mymemory") && (endpoint.equals("https://libretranslate.com/translate")
            || endpoint.equals("https://translate.googleapis.com/translate_a/single"))) endpoint = MYMEMORY;
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) endpoint = defaultEndpoint(provider);
        String color = clean(value.translationColor, defaults.translationColor).toLowerCase(Locale.ROOT);
        if (!COLORS.contains(color)) color = defaults.translationColor;
        return new TranslateConfig(endpoint,
            value.apiKey == null ? "" : value.apiKey.trim(),
            clean(value.sourceLanguage, "auto"),
            clean(value.targetLanguage, "en").toLowerCase(Locale.ROOT),
            value.autoTranslateIncoming,
            Math.max(3, Math.min(60, value.timeoutSeconds)),
            provider,
            clean(value.replyLanguage, "ru").toLowerCase(Locale.ROOT),
            color,
            value.translationBold,
            value.ignoredPlayers == null ? "" : value.ignoredPlayers.trim(),
            value.translateSystemMessages,
            value.useMinecraftLanguage);
    }

    public boolean ignores(String player) {
        if (player == null) return false;
        for (String name : ignoredPlayers.split(",")) {
            if (name.trim().equalsIgnoreCase(player)) return true;
        }
        return false;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception exception) {
            ZazuTranslateClient.LOGGER.error("Could not save translation config", exception);
        }
    }
}

