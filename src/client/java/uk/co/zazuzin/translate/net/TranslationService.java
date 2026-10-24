package uk.co.zazuzin.translate.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import uk.co.zazuzin.translate.ReplyClickEvent;
import uk.co.zazuzin.translate.config.TranslateConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TranslationService {
    private static final Pattern SENDER_PREFIX = Pattern.compile(
        "^(<[^>]{1,24}>|\\[[^\\]]{1,24}\\]|[A-Za-z0-9_]{3,16}:)\\s+(.+)$"
    );
    private static final ThreadLocal<Boolean> INTERNAL_MESSAGE = ThreadLocal.withInitial(() -> false);
    private static final List<LanguageChoice> COMMON_LANGUAGES = List.of(
        new LanguageChoice("en", "English"), new LanguageChoice("ru", "Russian"),
        new LanguageChoice("es", "Spanish"), new LanguageChoice("fr", "French"),
        new LanguageChoice("de", "German"), new LanguageChoice("it", "Italian"),
        new LanguageChoice("pt", "Portuguese"), new LanguageChoice("nl", "Dutch"),
        new LanguageChoice("pl", "Polish"), new LanguageChoice("uk", "Ukrainian"),
        new LanguageChoice("tr", "Turkish"), new LanguageChoice("ja", "Japanese"),
        new LanguageChoice("ko", "Korean"), new LanguageChoice("zh-CN", "Chinese"),
        new LanguageChoice("ar", "Arabic"), new LanguageChoice("hi", "Hindi")
    );

    public record LanguageChoice(String code, String name) {}
    private record TranslationResult(String text, String language) {}

    private volatile TranslateConfig config;
    private final HttpClient httpClient;
    private final ThreadPoolExecutor executor;
    private final Map<String, TranslationResult> cache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TranslationResult> eldest) {
            return size() > 128;
        }
    };
    private volatile long rateLimitUntil;
    private long lastRequestAt;
    private long lastAutoWarningAt;

    public TranslationService(TranslateConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(32), runnable -> {
                Thread thread = new Thread(runnable, "zazu-translate-worker");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void updateConfig(TranslateConfig updated) {
        this.config = updated;
        this.rateLimitUntil = 0;
        synchronized (cache) { cache.clear(); }
    }

    public boolean isInternalMessage() {
        return INTERNAL_MESSAGE.get();
    }

    public void translateAndDisplay(String text) {
        enqueueIncoming(text, true);
    }

    public void autoTranslateAndDisplay(String text) {
        if (config.autoTranslateIncoming()) enqueueIncoming(text, false);
    }

    private void enqueueIncoming(String text, boolean manual) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.isEmpty()) {
            if (manual) showError("There is no text to translate.");
            return;
        }
        Matcher sender = SENDER_PREFIX.matcher(normalized);
        String prefix = sender.matches() ? sender.group(1) + " " : "";
        String body = prefix.isEmpty() ? normalized : sender.group(2);
        TranslateConfig snapshot = config;
        String target = snapshot.useMinecraftLanguage()
            ? LanguageRouting.targetForMinecraft(Minecraft.getInstance().options.languageCode, snapshot.provider())
            : snapshot.targetLanguage();
        if (manual) showLocal(Component.literal("[Zazu's Translator] Translating...").withStyle(ChatFormatting.GRAY));

        try {
            CompletableFuture.supplyAsync(() -> request(body, target,
                snapshot.sourceLanguage(), snapshot), executor).whenComplete((result, error) -> {
                if (error != null) {
                    ZazuTranslateClient.LOGGER.warn("Translation request failed", error);
                    if (manual) showError(friendlyError(error));
                    else showAutoWarning(friendlyError(error));
                    return;
                }
                if (result == null || result.text().equalsIgnoreCase(body)) {
                    if (manual) showLocal(Component.literal(
                        "[Zazu's Translator] No translation available or message already in the target language.")
                        .withStyle(ChatFormatting.GRAY));
                    return;
                }
                String source = result.language().equals("auto") ? "?" : result.language().toUpperCase(Locale.ROOT);
                Component label = Component.literal("↳ [" + source + "→"
                    + target.toUpperCase(Locale.ROOT) + "] " + prefix)
                    .withStyle(ChatFormatting.GRAY);
                Component translation = Component.literal(result.text())
                    .withStyle(style -> style.withColor(color(snapshot.translationColor()))
                        .withBold(snapshot.translationBold()));
                Component line = label.copy().append(translation);
                if (!result.language().equalsIgnoreCase("auto") && !result.language().isBlank()) {
                    line = line.copy().append(Component.literal(" [Reply " + source + "]")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                            .withBold(false).withClickEvent(new ReplyClickEvent(target, result.language(), ""))));
                }
                showLocal(line);
            });
        } catch (RejectedExecutionException exception) {
            if (manual) showError("Translation queue is full. Try again shortly.");
            else showAutoWarning("Translation queue is full. Some messages were skipped.");
        }
    }

    public void previewReply(String english, Consumer<String> success, Consumer<String> failure) {
        String text = english == null ? "" : english.strip();
        if (text.isEmpty()) {
            failure.accept("Enter a reply first.");
            return;
        }
        TranslateConfig snapshot = config;
        try {
            CompletableFuture.supplyAsync(() -> request(text, snapshot.replyLanguage(), "en", snapshot), executor)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) failure.accept(friendlyError(error));
                    else success.accept(result == null ? text : result.text());
                }));
        } catch (RejectedExecutionException exception) {
            failure.accept("Translation queue is full. Try again shortly.");
        }
    }

    public void translateReply(String text, String source, String target,
                               Consumer<String> success, Consumer<String> failure) {
        TranslateConfig snapshot = config;
        // Provider changes must not leave target-only locale variants in source codes.
        String sourceCode = snapshot.provider().equals("mymemory")
            ? source : source.replace('_', '-').split("-")[0];
        String targetCode = LanguageRouting.replyTarget(target, snapshot.provider());
        try {
            CompletableFuture.supplyAsync(() -> request(text, targetCode, sourceCode, snapshot), executor)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) failure.accept(friendlyError(error));
                    else if (result != null) success.accept(result.text());
                    else if (LanguageRouting.sameLanguage(source, target)) success.accept(text);
                    else failure.accept("No translation available");
                }));
        } catch (RejectedExecutionException exception) {
            failure.accept("Translation queue is full");
        }
    }

    public void showStatus(String message) {
        showLocal(Component.literal("[Zazu's Translator] " + message).withStyle(ChatFormatting.YELLOW));
    }

    public void showReplyFailure(String message, Component retry) {
        showLocal(Component.literal("[Zazu's Translator] " + message).withStyle(ChatFormatting.RED).append(retry));
    }

    public void loadTargetLanguages(Consumer<List<LanguageChoice>> success, Consumer<String> failure) {
        TranslateConfig snapshot = config;
        try {
            CompletableFuture.supplyAsync(() -> fetchLanguages(snapshot), executor)
                .whenComplete((languages, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) failure.accept(friendlyError(error));
                    else success.accept(languages);
                }));
        } catch (RejectedExecutionException exception) {
            failure.accept("Translation queue is full.");
        }
    }

    private List<LanguageChoice> fetchLanguages(TranslateConfig settings) {
        if (settings.provider().equals("mymemory")) return COMMON_LANGUAGES;
        try {
            String uri = settings.provider().equals("deepl")
                ? settings.endpoint().replace("/translate", "/languages") + "?type=target"
                : settings.endpoint().replace("/translate", "/languages");
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                .header("Accept", "application/json");
            if (settings.provider().equals("deepl")) {
                if (settings.apiKey().isBlank()) throw new IllegalStateException("DeepL needs an API key");
                builder.header("Authorization", "DeepL-Auth-Key " + settings.apiKey());
            }
            HttpResponse<String> response = httpClient.send(builder.GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            requireSuccess(response);
            JsonArray values = JsonParser.parseString(response.body()).getAsJsonArray();
            List<LanguageChoice> choices = new ArrayList<>();
            Map<String, String> names = new HashMap<>();
            Set<String> targetCodes = new HashSet<>();
            for (JsonElement entry : values) {
                JsonObject lang = entry.getAsJsonObject();
                String code = lang.has("language") ? lang.get("language").getAsString()
                    : lang.get("code").getAsString();
                String name = lang.has("name") ? lang.get("name").getAsString() : code;
                names.put(code.toLowerCase(Locale.ROOT), name);
                if (lang.has("targets") && lang.get("targets").isJsonArray()) {
                    for (JsonElement target : lang.getAsJsonArray("targets")) {
                        targetCodes.add(target.getAsString().toLowerCase(Locale.ROOT));
                    }
                }
            }
            for (var entry : names.entrySet()) {
                if (!settings.provider().equals("libretranslate") || targetCodes.isEmpty()
                    || targetCodes.contains(entry.getKey())) {
                    choices.add(new LanguageChoice(entry.getKey(), entry.getValue()));
                }
            }
            choices.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            return choices;
        } catch (Exception exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private TranslationResult request(String text, String target, String configuredSource, TranslateConfig settings) {
        try {
            String source = configuredSource;
            String detected = LanguageDetection.detect(text);
            if (source.equalsIgnoreCase("auto") && settings.provider().equals("mymemory")) {
                source = detected;
            }
            if (source == null) return null;
            if (LanguageRouting.sameLanguage(source, target)
                || source.equalsIgnoreCase("auto") && LanguageRouting.sameLanguage(detected, target)) return null;
            String key = settings.provider() + "|" + settings.endpoint() + "|" + source + "|" + target + "|" + text;
            synchronized (cache) {
                TranslationResult cached = cache.get(key);
                if (cached != null) return cached;
            }
            if (System.currentTimeMillis() < rateLimitUntil) {
                throw new IllegalStateException("Provider rate limit; waiting before retrying");
            }
            long gap = 250 - (System.currentTimeMillis() - lastRequestAt);
            if (gap > 0) Thread.sleep(gap);
            lastRequestAt = System.currentTimeMillis();
            TranslationResult result = switch (settings.provider()) {
                case "deepl" -> requestDeepL(text, source, target, settings);
                case "libretranslate" -> requestLibre(text, source, target, settings);
                default -> requestMyMemory(text, source, target, settings);
            };
            if (LanguageRouting.sameLanguage(result.language(), target)) return null;
            synchronized (cache) { cache.put(key, result); }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Translation interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private TranslationResult requestMyMemory(String text, String source, String target,
                                              TranslateConfig settings) throws Exception {
        if (text.getBytes(StandardCharsets.UTF_8).length > 500)
            throw new IllegalStateException("MyMemory accepts up to 500 UTF-8 bytes");
        String translated = requestMyMemoryText(text, source, target, settings);
        if (TranslationOutput.accepts(translated, target))
            return new TranslationResult(translated.strip(), source);

        // A contaminated translation-memory entry can claim to be English while
        // containing another script. Translate two clauses separately if possible.
        int split = TranslationOutput.splitAfterPunctuation(text);
        if (split >= 0) {
            String left = text.substring(0, split).strip();
            String right = text.substring(split + 1).strip();
            Thread.sleep(250);
            String translatedLeft = requestMyMemoryText(left, source, target, settings);
            Thread.sleep(250);
            String translatedRight = requestMyMemoryText(right, source, target, settings);
            if (TranslationOutput.accepts(translatedLeft, target)
                && TranslationOutput.accepts(translatedRight, target)) {
                return new TranslationResult(TranslationOutput.join(
                    translatedLeft, text.charAt(split), translatedRight, target), source);
            }
        }
        throw new IllegalStateException("Provider returned an empty or wrong-language translation");
    }

    private String requestMyMemoryText(String text, String source, String target,
                                       TranslateConfig settings) throws Exception {
        String uri = settings.endpoint() + (settings.endpoint().contains("?") ? "&" : "?")
            + "q=" + encode(text) + "&langpair=" + encode(source + "|" + target);
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(settings.timeoutSeconds())).GET().build());
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.has("responseStatus") && root.get("responseStatus").getAsInt() != 200) {
            String detail = root.has("responseDetails") && !root.get("responseDetails").isJsonNull()
                ? root.get("responseDetails").getAsString() : "unknown provider error";
            throw new IllegalStateException("MyMemory: " + detail);
        }
        String translated = root.getAsJsonObject("responseData").get("translatedText").getAsString();
        return translated == null ? "" : translated.strip();
    }

    private TranslationResult requestLibre(String text, String source, String target,
                                           TranslateConfig settings) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("q", text);
        body.addProperty("source", source);
        body.addProperty("target", target);
        body.addProperty("format", "text");
        if (!settings.apiKey().isBlank()) body.addProperty("api_key", settings.apiKey());
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(settings.endpoint()))
            .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build());
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String detected = source;
        if (root.has("detectedLanguage") && root.get("detectedLanguage").isJsonObject()) {
            JsonObject info = root.getAsJsonObject("detectedLanguage");
            if (info.has("language")) detected = info.get("language").getAsString();
        }
        return new TranslationResult(requireText(root.get("translatedText").getAsString()), detected);
    }

    private TranslationResult requestDeepL(String text, String source, String target,
                                           TranslateConfig settings) throws Exception {
        if (settings.apiKey().isBlank()) throw new IllegalStateException("DeepL needs an API key");
        JsonObject body = new JsonObject();
        JsonArray lines = new JsonArray();
        lines.add(text);
        body.add("text", lines);
        body.addProperty("target_lang", target.toUpperCase(Locale.ROOT));
        if (!source.equalsIgnoreCase("auto")) body.addProperty("source_lang", source.toUpperCase(Locale.ROOT));
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(settings.endpoint()))
            .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Authorization", "DeepL-Auth-Key " + settings.apiKey())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject()
            .getAsJsonArray("translations").get(0).getAsJsonObject();
        String detected = result.has("detected_source_language")
            ? result.get("detected_source_language").getAsString() : source;
        return new TranslationResult(requireText(result.get("text").getAsString()), detected);
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        requireSuccess(response);
        return response;
    }

    private void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) return;
        if (response.statusCode() == 429) rateLimitUntil = System.currentTimeMillis() + 15_000;
        String detail = "";
        try {
            JsonElement json = JsonParser.parseString(response.body());
            if (json.isJsonObject() && json.getAsJsonObject().has("error"))
                detail = ": " + json.getAsJsonObject().get("error").getAsString();
        } catch (Exception ignored) {}
        throw new IllegalStateException("Provider returned HTTP " + response.statusCode() + detail);
    }

    private static String requireText(String text) {
        if (text == null || text.isBlank()) throw new IllegalStateException("Provider returned an empty translation");
        return text.strip();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static int color(String name) {
        return switch (name) {
            case "red" -> 0xFF5555;
            case "blue" -> 0x5599FF;
            case "yellow" -> 0xFFFF55;
            case "purple" -> 0xAA55FF;
            case "orange" -> 0xFFAA00;
            case "pink" -> 0xFF77CC;
            default -> 0x55FF55;
        };
    }

    private static String friendlyError(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Translation failed" : cause.getMessage();
    }

    private void showAutoWarning(String message) {
        synchronized (this) {
            if (System.currentTimeMillis() - lastAutoWarningAt < 60_000) return;
            lastAutoWarningAt = System.currentTimeMillis();
        }
        showError(message + ". Automatic errors are muted for one minute.");
    }

    private static void showError(String message) {
        showLocal(Component.literal("[Zazu's Translator] " + message).withStyle(ChatFormatting.RED));
    }

    private static void showLocal(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.gui == null) return;
            INTERNAL_MESSAGE.set(true);
            try {
                minecraft.gui.hud.getChat().addClientSystemMessage(message);
            } finally {
                INTERNAL_MESSAGE.set(false);
            }
        });
    }
}

