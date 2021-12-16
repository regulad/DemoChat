package xyz.regulad.demochat.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.demochat.ChatChannel;
import xyz.regulad.demochat.DemoChatPlugin;

import java.util.ArrayList;
import java.util.List;

public class DeleteChannelCommand implements TabCompleter, CommandExecutor {
    private final @NotNull DemoChatPlugin demoChatPlugin;

    public DeleteChannelCommand(final @NotNull DemoChatPlugin demoChatPlugin) {
        this.demoChatPlugin = demoChatPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(args.length > 0)) {
            return false;
        }
        final @Nullable ChatChannel channel = this.demoChatPlugin.getChatChannel(args[0]);
        if (channel != null) {
            this.demoChatPlugin.deregisterChatChannel(channel);
            sender.sendMessage(Component.text("Deregistered channel.").color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not deregister channel.").color(NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        final @NotNull ArrayList<@NotNull String> options = new ArrayList<>();
        this.demoChatPlugin.getChatChannels().forEach((chatChannel -> options.add(chatChannel.name())));
        return options;
    }
}
