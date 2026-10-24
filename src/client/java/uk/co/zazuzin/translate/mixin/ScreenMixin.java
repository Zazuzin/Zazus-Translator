package uk.co.zazuzin.translate.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.zazuzin.translate.TranslateClickEvent;
import uk.co.zazuzin.translate.ReplyClickEvent;
import uk.co.zazuzin.translate.ui.QuickReplyChatScreen;
import uk.co.zazuzin.translate.ZazuTranslateClient;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
    private static void zazuTranslate$handleClick(
        ClickEvent event,
        Minecraft minecraft,
        Screen activeScreen,
        CallbackInfo callbackInfo
    ) {
        if (event instanceof TranslateClickEvent translateClickEvent) {
            ZazuTranslateClient.translations().translateAndDisplay(translateClickEvent.message());
            callbackInfo.cancel();
        } else if (event instanceof ReplyClickEvent reply) {
            if (minecraft.player != null) minecraft.gui.setScreen(new QuickReplyChatScreen(
                reply.sourceLanguage(), reply.targetLanguage(), reply.draft()));
            callbackInfo.cancel();
        }
    }
}

