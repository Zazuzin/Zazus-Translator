package uk.co.zazuzin.translate;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.zazuzin.translate.config.TranslateConfig;
import uk.co.zazuzin.translate.net.TranslationService;
import uk.co.zazuzin.translate.ui.ReplyScreen;
import uk.co.zazuzin.translate.ui.SettingsScreen;

public final class ZazuTranslateClient implements ClientModInitializer {
    public static final String MOD_ID = "zazu_translate";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile TranslateConfig config;
    private static TranslationService translationService;

    @Override
    public void onInitializeClient() {
        config = TranslateConfig.load();
        translationService = new TranslationService(config);
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        KeyMapping settings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zazu_translate.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_F8, category));
        KeyMapping reply = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.zazu_translate.reply", InputConstants.Type.KEYSYM, InputConstants.KEY_F9, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settings.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new SettingsScreen(null));
            }
            while (reply.consumeClick()) {
                if (client.player != null && client.gui.screen() == null)
                    client.gui.setScreen(new ReplyScreen(null));
            }
        });
        LOGGER.info("Zazu's Translator 1.0.0 initialized; target mode is {}",
            config.useMinecraftLanguage() ? "Minecraft language" : config.targetLanguage());
    }

    public static TranslateConfig config() {
        return config;
    }

    public static TranslationService translations() {
        return translationService;
    }

    public static void setConfig(TranslateConfig updated) {
        config = updated;
        updated.save();
        translationService.updateConfig(updated);
    }
}

