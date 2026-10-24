package uk.co.zazuzin.translate.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import uk.co.zazuzin.translate.net.TranslationService.LanguageChoice;

import java.util.List;

public final class LanguageScreen extends Screen {
    private final SettingsScreen parent;
    private final boolean forReply;
    private List<LanguageChoice> languages;
    private String status = "Loading provider languages...";
    private int page;

    public LanguageScreen(SettingsScreen parent, boolean forReply) {
        super(Component.literal("Choose translation language"));
        this.parent = parent;
        this.forReply = forReply;
    }

    private LanguageScreen(SettingsScreen parent, boolean forReply,
                           List<LanguageChoice> languages, String status, int page) {
        this(parent, forReply);
        this.languages = languages;
        this.status = status;
        this.page = page;
    }

    @Override
    protected void init() {
        if (languages == null) {
            ZazuTranslateClient.translations().loadTargetLanguages(
                values -> {
                    if (this.minecraft.gui.screen() == this) this.minecraft.gui.setScreen(
                        new LanguageScreen(parent, forReply, values, values.size() + " available languages", 0));
                },
                error -> {
                    if (this.minecraft.gui.screen() == this) this.minecraft.gui.setScreen(
                        new LanguageScreen(parent, forReply, List.of(), error, 0));
                });
        }
        int x = width / 2 - 120;
        if (languages != null) {
            int start = page * 8;
            for (int i = start; i < Math.min(start + 8, languages.size()); i++) {
                LanguageChoice language = languages.get(i);
                int y = 34 + (i - start) * 22;
                addRenderableWidget(Button.builder(
                    Component.literal(language.name() + " (" + language.code() + ")"),
                    b -> parent.selectLanguage(language.code(), forReply))
                    .bounds(x, y, 240, 20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("Previous"), b ->
                this.minecraft.gui.setScreen(new LanguageScreen(parent, forReply, languages, status, page - 1)))
                .bounds(x, 216, 75, 20).build()).active = page > 0;
            addRenderableWidget(Button.builder(Component.literal("Next"), b ->
                this.minecraft.gui.setScreen(new LanguageScreen(parent, forReply, languages, status, page + 1)))
                .bounds(x + 82, 216, 75, 20).build()).active = (page + 1) * 8 < languages.size();
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(x + 165, 216, 75, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, status, width / 2 - 120, 18, 0xFFFFFFFF, true);
    }
}

