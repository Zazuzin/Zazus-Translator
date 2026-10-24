package uk.co.zazuzin.translate.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.zazuzin.translate.TranslateClickEvent;
import uk.co.zazuzin.translate.ZazuTranslateClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    private static final String ADD_MESSAGE_METHOD = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V";
    private static final Pattern UNSIGNED_PLAYER_MESSAGE = Pattern.compile(
        "^\\s*(?:<([A-Za-z0-9_]{3,16})>|\\[([A-Za-z0-9_]{3,16})\\]|([A-Za-z0-9_]{3,16}):)\\s+(.+)$"
    );

    @ModifyVariable(
        method = ADD_MESSAGE_METHOD,
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component zazuTranslate$appendButton(Component message) {
        if (ZazuTranslateClient.translations() == null || ZazuTranslateClient.translations().isInternalMessage()) {
            return message;
        }

        String plainText = message.getString();
        if (plainText.isBlank()) return message;

        Component button = Component.literal(" [Translate]").withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true)
            .withClickEvent(new TranslateClickEvent(plainText))
        );

        return message.copy().append(button);
    }

    @Inject(method = ADD_MESSAGE_METHOD, at = @At("TAIL"))
    private void zazuTranslate$autoTranslate(
        Component message,
        MessageSignature signature,
        GuiMessageSource source,
        GuiMessageTag tag,
        CallbackInfo callbackInfo
    ) {
        if (ZazuTranslateClient.translations() == null
            || ZazuTranslateClient.translations().isInternalMessage()) {
            return;
        }

        String plainText = message.getString();
        if (plainText.endsWith(" [Translate]")) {
            plainText = plainText.substring(0, plainText.length() - " [Translate]".length());
        }
        Matcher matcher = UNSIGNED_PLAYER_MESSAGE.matcher(plainText);
        boolean playerFormat = matcher.matches();
        String sender = playerFormat
            ? matcher.group(1) != null ? matcher.group(1)
                : matcher.group(2) != null ? matcher.group(2) : matcher.group(3)
            : null;
        if (sender != null && ZazuTranslateClient.config().ignores(sender)) return;
        if (signature == null && !looksLikeUnsignedPlayerChat(plainText)
            && !ZazuTranslateClient.config().translateSystemMessages()) return;
        ZazuTranslateClient.translations().autoTranslateAndDisplay(plainText);
    }

    private static boolean looksLikeUnsignedPlayerChat(String text) {
        Matcher matcher = UNSIGNED_PLAYER_MESSAGE.matcher(text);
        if (!matcher.matches()) return false;
        String sender = matcher.group(1) != null ? matcher.group(1)
            : matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        return !sender.equalsIgnoreCase("server")
            && !sender.equalsIgnoreCase("system")
            && !sender.equalsIgnoreCase("console");
    }
}

