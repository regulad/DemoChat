package xyz.regulad.demochat.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.demochat.ChatChannel;
import xyz.regulad.demochat.DemoChatPlugin;

import java.util.ArrayList;
import java.util.List;

public class ChangeChannelCommand implements TabCompleter, CommandExecutor {
    private final @NotNull DemoChatPlugin demoChatPlugin;

    public ChangeChannelCommand(final @NotNull DemoChatPlugin demoChatPlugin) {
        this.demoChatPlugin = demoChatPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof final @NotNull Player player) {
            if (!(args.length > 0)) {
                return false;
            }
            final @Nullable ChatChannel channel = this.demoChatPlugin.getChatChannel(args[0]);
            this.demoChatPlugin.getServer().getScheduler().runTaskAsynchronously(this.demoChatPlugin, () -> {
                if (this.demoChatPlugin.setPlayerChatChannel(player, channel)) {
                    sender.sendMessage(Component.text(String.format("Channel changed to %s.", channel != null ? channel.name() : String.format("default (%s)", this.demoChatPlugin.getDefaultChatChannel()))));
                } else {
                    sender.sendMessage(Component.text("An error occurred.").color(NamedTextColor.RED));
                }
            });
            return true;
        } else {
            sender.sendMessage(Component.text("This command can only be executed by a player.").color(NamedTextColor.RED));
            return false;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final @NotNull ArrayList<@NotNull String> options = new ArrayList<>();
        this.demoChatPlugin.getChatChannels().forEach((chatChannel -> options.add(chatChannel.name())));
        return options;
    }
}
