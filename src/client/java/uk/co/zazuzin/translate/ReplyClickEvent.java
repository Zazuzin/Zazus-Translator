package uk.co.zazuzin.translate;

import net.minecraft.network.chat.ClickEvent;

/** Local chat action; never forwarded as a server command. */
public record ReplyClickEvent(String sourceLanguage, String targetLanguage, String draft) implements ClickEvent {
    @Override
    public Action action() {
        return Action.RUN_COMMAND;
    }
}

