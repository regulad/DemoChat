package xyz.regulad.demochat.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.regulad.demochat.DemoChatPlugin;

public class ToggleFilterCommand implements CommandExecutor {
    final @NotNull DemoChatPlugin demoChatPlugin;

    public ToggleFilterCommand(final @NotNull DemoChatPlugin demoChatPlugin) {
        this.demoChatPlugin = demoChatPlugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof final @NotNull Player player) {
            this.demoChatPlugin.getServer().getScheduler().runTaskAsynchronously(this.demoChatPlugin, () -> {
                boolean filterState = this.demoChatPlugin.prefersFilter(player);
                if (this.demoChatPlugin.setPlayerFilterPreference(player, !filterState)) {
                    filterState = !filterState; // We swapped it.
                    sender.sendMessage(Component.text(filterState ? "Filter is now on." : "Filter is now off.").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("An error occurred.").color(NamedTextColor.RED));
                }
            });
        } else {
            sender.sendMessage(Component.text("This command can only be executed by a player.").color(NamedTextColor.RED));
        }
        return true;
    }
}
