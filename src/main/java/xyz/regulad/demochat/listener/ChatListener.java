package xyz.regulad.demochat.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import xyz.regulad.demochat.ChatChannel;
import xyz.regulad.demochat.DemoChatPlugin;
import xyz.regulad.demochat.event.AsyncDemoChatEvent;

public class ChatListener implements Listener {
    private final @NotNull DemoChatPlugin demoChatPlugin;

    public ChatListener(final @NotNull DemoChatPlugin demoChatPlugin) {
        this.demoChatPlugin = demoChatPlugin;
    }

    @EventHandler
    public void propagateToAsyncDemoChatEvent(final @NotNull AsyncChatEvent asyncChatEvent) {
        final @NotNull ChatChannel destinationChannel = this.demoChatPlugin.getPlayerChatChannelOrDefault(asyncChatEvent.getPlayer());
        final @NotNull AsyncDemoChatEvent asyncDemoChatEvent = new AsyncDemoChatEvent(destinationChannel, asyncChatEvent.getPlayer());
        this.demoChatPlugin.getServer().getPluginManager().callEvent(asyncDemoChatEvent);
    }

    @EventHandler
    public void modifyChatEvent(final @NotNull AsyncChatEvent asyncChatEvent) {
        final @NotNull ChatChannel destinationChannel = this.demoChatPlugin.getPlayerChatChannelOrDefault(asyncChatEvent.getPlayer());
        asyncChatEvent.viewers().clear();
        asyncChatEvent.viewers().addAll(this.demoChatPlugin.getPlayersWhoCanHear(asyncChatEvent.getPlayer(), destinationChannel));
        // Now only viewers who can hear the chat following the channel logic can see it.

        final @NotNull ChatRenderer existingChatRenderer = asyncChatEvent.renderer();
        asyncChatEvent.renderer((source, sourceDisplayName, message, viewer) -> {
            if (viewer instanceof final @NotNull Player player && this.demoChatPlugin.prefersFilter(player) && message instanceof final @NotNull TextComponent textComponent) {
                return existingChatRenderer.render(source, sourceDisplayName, this.demoChatPlugin.filterComponent(textComponent), viewer);
            } // If message isn't a TextComponent, this won't work.
            return existingChatRenderer.render(source, sourceDisplayName, message, viewer);
        });
    }
}
