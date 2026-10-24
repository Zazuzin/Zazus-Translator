package uk.co.zazuzin.translate.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import java.util.ArrayList;
import java.util.List;

public final class ReplyScreen extends Screen {
    private final Screen parent;
    private EditBox input;
    private Button send;
    private String preview;
    private String status = "Write in English, preview the translation, then send.";
    private String previewFor;

    public ReplyScreen(Screen parent) {
        super(Component.literal("Translate a reply"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 145;
        input = new EditBox(this.font, x, 55, 290, 20, Component.literal("English reply"));
        input.setMaxLength(256);
        input.setHint(Component.literal("Type your English reply here"));
        input.setResponder(text -> {
            if (!text.equals(previewFor)) {
                preview = null;
                status = "Preview your reply before sending.";
                if (send != null) send.active = false;
            }
        });
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.literal("Translate preview"), b -> requestPreview())
            .bounds(x, 178, 140, 20).build());
        send = addRenderableWidget(Button.builder(Component.literal("Send translated reply"), b -> sendReply())
            .bounds(x + 150, 178, 140, 20).build());
        send.active = false;
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(x + 75, 207, 140, 20).build());
    }

    private void requestPreview() {
        String original = input.getValue().strip();
        status = "Translating...";
        preview = null;
        send.active = false;
        ZazuTranslateClient.translations().previewReply(original, translated -> {
            if (this.minecraft.gui.screen() != this || !input.getValue().strip().equals(original)) return;
            preview = translated;
            previewFor = original;
            if (translated.length() > 256) {
                status = "Translation is over 256 characters. Shorten your English reply.";
                send.active = false;
            } else {
                status = "Review the result before sending.";
                send.active = true;
            }
        }, error -> {
            if (this.minecraft.gui.screen() != this) return;
            status = error;
        });
    }

    private void sendReply() {
        if (preview == null || !input.getValue().strip().equals(previewFor)
            || this.minecraft.getConnection() == null) return;
        this.minecraft.getConnection().sendChat(preview);
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = width / 2 - 145;
        graphics.text(this.font, "Reply language: " + ZazuTranslateClient.config().replyLanguage(),
            x, 24, 0xFFFFFFFF, true);
        graphics.text(this.font, "English message", x, 42, 0xFFFFFFFF, true);
        graphics.text(this.font, "Translation preview", x, 89, 0xFFFFFFFF, true);
        if (preview != null) {
            List<String> lines = wrap(preview, 44);
            for (int i = 0; i < Math.min(lines.size(), 6); i++) {
                graphics.text(this.font, lines.get(i), x, 103 + 10 * i, 0xFF55FF55, true);
            }
        }
        String shownStatus = status.length() > 70 ? status.substring(0, 67) + "..." : status;
        graphics.text(this.font, shownStatus, x, 165, 0xFFAAAAAA, true);
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= width) {
                lines.add(remaining);
                break;
            }
            int cut = remaining.lastIndexOf(' ', width);
            if (cut < width / 2) cut = width;
            lines.add(remaining.substring(0, cut).strip());
            remaining = remaining.substring(cut).stripLeading();
        }
        return lines;
    }
}

