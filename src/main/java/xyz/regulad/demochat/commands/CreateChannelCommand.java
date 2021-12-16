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

public class CreateChannelCommand implements TabCompleter, CommandExecutor {
    private final @NotNull DemoChatPlugin demoChatPlugin;

    public CreateChannelCommand(final @NotNull DemoChatPlugin demoChatPlugin) {
        this.demoChatPlugin = demoChatPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(args.length >= 3)) {
            return false;
        }

        final @NotNull String name = args[0];
        final boolean sameworld = Boolean.parseBoolean(args[1]);
        final int distance = Integer.parseInt(args[2]);
        final @NotNull ChatChannel chatChannel = new ChatChannel(name, sameworld, distance);

        this.demoChatPlugin.registerChatChannel(chatChannel);

        sender.sendMessage(Component.text("Registered channel.").color(NamedTextColor.GREEN));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return switch (args.length) {
            case (1) -> new ArrayList<>(List.of(new String[]{"newchannel", "oldchannel"}));
            case (2) -> new ArrayList<>(List.of(new String[]{"true", "false"}));
            case (3) -> new ArrayList<>(List.of(new String[]{"-1", "3", "20"}));
            default -> null;
        };
    }
}
