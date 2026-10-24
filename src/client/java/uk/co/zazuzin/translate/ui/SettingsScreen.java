package uk.co.zazuzin.translate.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import uk.co.zazuzin.translate.config.TranslateConfig;
import uk.co.zazuzin.translate.net.LanguageRouting;

public final class SettingsScreen extends Screen {
    private static final String[] COLORS = {"red", "blue", "green", "yellow", "purple", "orange", "pink"};
    private static final String[] PROVIDERS = {"mymemory", "libretranslate", "deepl"};
    private final Screen parent;
    private boolean providerPage;
    private String endpoint, apiKey, source, target, reply, color, ignored, provider;
    private boolean auto, bold, system, minecraftTarget;
    private EditBox targetField, replyField, ignoredField, endpointField, keyField, sourceField;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Zazu's Translator Settings"));
        this.parent = parent;
        TranslateConfig config = ZazuTranslateClient.config();
        endpoint = config.endpoint();
        apiKey = config.apiKey();
        source = config.sourceLanguage();
        target = config.targetLanguage();
        reply = config.replyLanguage();
        color = config.translationColor();
        ignored = config.ignoredPlayers();
        provider = config.provider();
        auto = config.autoTranslateIncoming();
        bold = config.translationBold();
        system = config.translateSystemMessages();
        minecraftTarget = config.useMinecraftLanguage();
    }

    private SettingsScreen(SettingsScreen original, boolean providerPage) {
        super(Component.literal("Zazu's Translator Settings"));
        this.parent = original.parent;
        this.providerPage = providerPage;
        this.endpoint = original.endpoint;
        this.apiKey = original.apiKey;
        this.source = original.source;
        this.target = original.target;
        this.reply = original.reply;
        this.color = original.color;
        this.ignored = original.ignored;
        this.provider = original.provider;
        this.auto = original.auto;
        this.bold = original.bold;
        this.system = original.system;
        this.minecraftTarget = original.minecraftTarget;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 145;
        if (providerPage) {
            addRenderableWidget(Button.builder(Component.literal("Provider: " + providerLabel()), b -> {
                provider = next(PROVIDERS, provider);
                endpoint = TranslateConfig.defaultEndpoint(provider);
                endpointField.setValue(endpoint);
                b.setMessage(Component.literal("Provider: " + providerLabel()));
            }).bounds(x, 36, 290, 20).build());
            endpointField = field(x, 78, 290, "Provider endpoint", endpoint, 240);
            keyField = field(x, 122, 290, "API key", apiKey, 240);
            sourceField = field(x, 166, 100, "Source language", source, 12);
            addRenderableWidget(Button.builder(Component.literal("General options"), b -> {
                captureFields();
                this.minecraft.gui.setScreen(new SettingsScreen(this, false));
            }).bounds(x, 205, 140, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> saveAndClose())
                .bounds(x + 150, 205, 140, 20).build());
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("Auto incoming: " + onOff(auto)), b -> {
            auto = !auto;
            b.setMessage(Component.literal("Auto incoming: " + onOff(auto)));
        }).bounds(x, 36, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("System messages: " + onOff(system)), b -> {
            system = !system;
            b.setMessage(Component.literal("System messages: " + onOff(system)));
        }).bounds(x + 150, 36, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal(targetModeLabel()), b -> {
            minecraftTarget = !minecraftTarget;
            if (targetField != null) targetField.active = !minecraftTarget;
            b.setMessage(Component.literal(targetModeLabel()));
        }).bounds(x, 62, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Bold: " + onOff(bold)), b -> {
            bold = !bold;
            b.setMessage(Component.literal("Bold: " + onOff(bold)));
        }).bounds(x + 150, 62, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Colour: " + color), b -> {
            color = next(COLORS, color);
            b.setMessage(Component.literal("Colour: " + color));
        }).bounds(x, 88, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Provider: " + providerLabel()), b -> {
            captureFields();
            this.minecraft.gui.setScreen(new SettingsScreen(this, true));
        }).bounds(x + 150, 88, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Choose target"), b -> {
            captureFields();
            persist();
            this.minecraft.gui.setScreen(new LanguageScreen(this, false));
        }).bounds(x, 114, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Choose reply"), b -> {
            captureFields();
            persist();
            this.minecraft.gui.setScreen(new LanguageScreen(this, true));
        }).bounds(x + 150, 114, 140, 20).build());

        targetField = field(x, 158, 65, "Target", target, 12);
        targetField.active = !minecraftTarget;
        replyField = field(x + 80, 158, 65, "Reply", reply, 12);
        ignoredField = field(x + 160, 158, 130, "Ignored players", ignored, 500);
        addRenderableWidget(Button.builder(Component.literal("Reply preview"), b -> {
            captureFields();
            persist();
            this.minecraft.gui.setScreen(new ReplyScreen(this));
        }).bounds(x + 75, 190, 140, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> saveAndClose())
            .bounds(x + 75, 220, 140, 20).build());
    }

    private EditBox field(int x, int y, int width, String hint, String value, int max) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.literal(hint));
        box.setMaxLength(max);
        box.setValue(value);
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }

    private void captureFields() {
        if (providerPage) {
            if (endpointField != null) endpoint = endpointField.getValue().strip();
            if (keyField != null) apiKey = keyField.getValue().strip();
            if (sourceField != null) source = sourceField.getValue().strip();
        } else {
            if (targetField != null) target = targetField.getValue().strip();
            if (replyField != null) reply = replyField.getValue().strip();
            if (ignoredField != null) ignored = ignoredField.getValue().strip();
        }
    }

    void selectLanguage(String code, boolean forReply) {
        if (forReply) reply = code;
        else {
            target = code;
            minecraftTarget = false;
        }
        this.minecraft.gui.setScreen(new SettingsScreen(this, false));
    }

    private void persist() {
        TranslateConfig previous = ZazuTranslateClient.config();
        ZazuTranslateClient.setConfig(TranslateConfig.sanitize(new TranslateConfig(
            endpoint, apiKey, source, target, auto, previous.timeoutSeconds(),
            provider, reply, color, bold, ignored, system, minecraftTarget)));
    }

    private void saveAndClose() {
        captureFields();
        persist();
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = this.width / 2 - 145;
        graphics.text(this.font, "Zazu's Translator", x, 10, 0xFFFFFFFF, true);
        if (providerPage) {
            graphics.text(this.font, "Endpoint", x, 65, 0xFFFFFFFF, true);
            graphics.text(this.font, "API key (shown while editing)", x, 109, 0xFFFFFFFF, true);
            graphics.text(this.font, "Source code (auto or language code)", x, 153, 0xFFFFFFFF, true);
        } else {
            graphics.text(this.font, "Custom target", x, 145, 0xFFFFFFFF, true);
            graphics.text(this.font, "Reply to", x + 80, 145, 0xFFFFFFFF, true);
            graphics.text(this.font, "Ignore names (comma separated)", x + 160, 145, 0xFFFFFFFF, true);
            String language = this.minecraft.options.languageCode;
            String active = minecraftTarget ? LanguageRouting.targetForMinecraft(language, provider) : target;
            graphics.text(this.font, minecraftTarget
                ? "Minecraft " + language + " -> " + active + "; skip native"
                : "Custom target " + active + "; skip native",
                x, 23, 0xFFAAAAAA, true);
        }
    }

    private static String onOff(boolean value) { return value ? "On" : "Off"; }
    private String targetModeLabel() { return "Target: " + (minecraftTarget ? "Minecraft" : "Custom"); }

    private String providerLabel() {
        return switch (provider) {
            case "deepl" -> "DeepL";
            case "libretranslate" -> "Libre";
            default -> "MyMemory";
        };
    }

    private static String next(String[] choices, String current) {
        for (int i = 0; i < choices.length; i++) {
            if (choices[i].equals(current)) return choices[(i + 1) % choices.length];
        }
        return choices[0];
    }
}

