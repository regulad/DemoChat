package xyz.regulad.demochat.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xyz.regulad.demochat.ChatChannel;

public class AsyncDemoChatEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final @NotNull ChatChannel channel;
    private final @NotNull Player sender;

    public AsyncDemoChatEvent(final @NotNull ChatChannel channel, final @NotNull Player sender) {
        super(true);
        this.channel = channel;
        this.sender = sender;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public final @NotNull ChatChannel getChannel() {
        return this.channel;
    }

    public final @NotNull Player getSender() {
        return this.sender;
    }
}
