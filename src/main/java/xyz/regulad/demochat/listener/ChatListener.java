package xyz.regulad.demochat.listener;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.demochat.ChatChannel;
import xyz.regulad.demochat.DemoChatPlugin;
import xyz.regulad.demochat.event.AsyncDemoChatEvent;
import xyz.regulad.demochat.util.ChatRendererUtil;

import java.util.ArrayList;

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
    public void changeWhoCanHear(final @NotNull AsyncChatEvent asyncChatEvent) {
        final @NotNull ChatChannel destinationChannel = this.demoChatPlugin.getPlayerChatChannelOrDefault(asyncChatEvent.getPlayer());
        final @NotNull ArrayList<@NotNull Player> canHear = (this.demoChatPlugin.getPlayersWhoCanHear(asyncChatEvent.getPlayer(), destinationChannel));
        asyncChatEvent.viewers().removeIf(audience -> {
            if (audience instanceof final @NotNull Player player) {
                return !canHear.contains(player);
            }
            return false;
        });
    }

    @EventHandler
    public void addRenderer(final @NotNull AsyncChatEvent asyncChatEvent) {
        final @Nullable ChatRenderer existingChatRenderer = ChatRendererUtil.isViewerUnaware(asyncChatEvent.renderer()) ? null : asyncChatEvent.renderer();
        asyncChatEvent.renderer((source, sourceDisplayName, message, viewer) -> {
            final @NotNull ChatRenderer localRenderer = existingChatRenderer != null ? existingChatRenderer : ChatRenderer.defaultRenderer();
            if (viewer instanceof final @NotNull Player player && message instanceof final @NotNull TextComponent textComponent) {
                if (this.demoChatPlugin.prefersFilter(player)) {
                    return localRenderer.render(source, sourceDisplayName, textComponent.content(this.demoChatPlugin.filterString(textComponent.content())), viewer);
                }
            } // If message isn't a TextComponent, this won't work.
            return localRenderer.render(source, sourceDisplayName, message, viewer);
        });
    }
}
