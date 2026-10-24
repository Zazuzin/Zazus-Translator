package uk.co.zazuzin.translate;

import net.minecraft.network.chat.ClickEvent;

public final class TranslateClickEvent implements ClickEvent {
    private final String message;

    public TranslateClickEvent(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    @Override
    public Action action() {
        return Action.RUN_COMMAND;
    }
}

