package uk.co.zazuzin.translate.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import uk.co.zazuzin.translate.ReplyClickEvent;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import uk.co.zazuzin.translate.net.ReplyValidation;

import java.util.Locale;

/** Vanilla chat with a translation target for this one submission only. */
public final class QuickReplyChatScreen extends ChatScreen {
    private final String sourceLanguage;
    private final String targetLanguage;
    private boolean submitted;

    public QuickReplyChatScreen(String sourceLanguage, String targetLanguage, String draft) {
        super(draft, false);
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    @Override
    public void handleChatInput(String message, boolean addToHistory) {
        String text = message.strip();
        // Slash commands retain vanilla behavior and are never translated.
        if (text.startsWith("/")) {
            super.handleChatInput(message, addToHistory);
            return;
        }
        if (text.isEmpty() || submitted) return;
        submitted = true;
        if (addToHistory) this.minecraft.gui.hud.getChat().addRecentChat(text);
        var connection = this.minecraft.getConnection();
        if (connection == null) {
            failure(text, "Not connected; nothing sent.");
            return;
        }
        ZazuTranslateClient.translations().showStatus("Translating reply to "
            + targetLanguage.toUpperCase(Locale.ROOT) + "...");
        ZazuTranslateClient.translations().translateReply(text, sourceLanguage, targetLanguage, translated -> {
            if (this.minecraft.getConnection() != connection || this.minecraft.player == null) {
                failure(text, "Connection changed; nothing sent.");
                return;
            }
            String error = ReplyValidation.error(translated);
            if (error != null) {
                failure(text, error + "; nothing sent.");
                return;
            }
            connection.sendChat(translated);
        }, error -> failure(text, error + "; nothing sent."));
    }

    private void failure(String draft, String error) {
        Component retry = Component.literal(" [Retry " + targetLanguage.toUpperCase(Locale.ROOT) + "]")
            .withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ReplyClickEvent(sourceLanguage, targetLanguage, draft)));
        ZazuTranslateClient.translations().showReplyFailure(error, retry);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, "Public reply: " + sourceLanguage.toUpperCase(Locale.ROOT) + " -> "
            + targetLanguage.toUpperCase(Locale.ROOT), 4, this.height - 50, 0xFFFFFF55, true);
        graphics.text(this.font, "Enter: translate & send | Esc: cancel",
            4, this.height - 39, 0xFFFFFF55, true);
    }
}

