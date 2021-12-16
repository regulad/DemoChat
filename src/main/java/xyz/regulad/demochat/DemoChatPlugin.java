package xyz.regulad.demochat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.demochat.commands.ChangeChannelCommand;
import xyz.regulad.demochat.commands.CreateChannelCommand;
import xyz.regulad.demochat.commands.DeleteChannelCommand;
import xyz.regulad.demochat.commands.ToggleFilterCommand;
import xyz.regulad.demochat.listener.ChatListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DemoChatPlugin extends JavaPlugin {
    private final @NotNull HikariConfig hikariConfig = new HikariConfig();
    private @Nullable HikariDataSource hikariDataSource = null;

    @Override
    public void onEnable() {
        // Save config
        this.saveDefaultConfig();

        // Configure MySQL
        final @Nullable String hostname = this.getConfig().getString("database.host");
        final int port = this.getConfig().getInt("database.port");
        final @Nullable String databaseName = this.getConfig().getString("database.db_name");
        final @Nullable String databaseOptions = this.getConfig().getString("database.options");
        if (hostname == null || databaseName == null) {
            throw new RuntimeException("Database not defined in configuration.");
        }

        final @NotNull String jdbcUri = String.format("jdbc:mysql://%s%s/%s%s", hostname, port != 3306 ? String.valueOf(port) : "", databaseName, databaseOptions != null ? databaseOptions : "");

        this.getLogger().info(String.format("Connecting to %s.", jdbcUri));
        this.hikariConfig.setJdbcUrl(jdbcUri);
        this.hikariConfig.setUsername(this.getConfig().getString("database.user"));
        this.hikariConfig.setPassword(this.getConfig().getString("database.password"));
        this.hikariDataSource = new HikariDataSource(this.hikariConfig);
        this.getLogger().info(String.format("Successfully connected to the MySQL database at %s.", jdbcUri));

        try {
            // Create channel table
            this.hikariDataSource.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS demochat (uuid CHAR(36) UNIQUE, channel VARCHAR(256) DEFAULT null, filter BOOLEAN DEFAULT FALSE, PRIMARY KEY (uuid));").execute();
        } catch (final @NotNull SQLException sqlException) {
            sqlException.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register events
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register command executors & tab listeners
        final @Nullable PluginCommand toggleFilterPluginCommand = this.getCommand("togglefilter");
        if (toggleFilterPluginCommand != null) {
            final @NotNull ToggleFilterCommand toggleFilterCommand = new ToggleFilterCommand(this);
            toggleFilterPluginCommand.setExecutor(toggleFilterCommand);
        }
        final @Nullable PluginCommand createChannelPluginCommand = this.getCommand("createchannel");
        if (createChannelPluginCommand != null) {
            final @NotNull CreateChannelCommand createChannelCommand = new CreateChannelCommand(this);
            createChannelPluginCommand.setExecutor(createChannelCommand);
            createChannelPluginCommand.setTabCompleter(createChannelCommand);
        }
        final @Nullable PluginCommand removeChannelPluginCommand = this.getCommand("removechannel");
        if (removeChannelPluginCommand != null) {
            final @NotNull DeleteChannelCommand removeChannelCommand = new DeleteChannelCommand(this);
            removeChannelPluginCommand.setExecutor(removeChannelCommand);
            removeChannelPluginCommand.setTabCompleter(removeChannelCommand);
        }
        final @Nullable PluginCommand changeChannelPluginCommand = this.getCommand("changechannel");
        if (changeChannelPluginCommand != null) {
            final @NotNull ChangeChannelCommand changeChannelCommand = new ChangeChannelCommand(this);
            changeChannelPluginCommand.setExecutor(changeChannelCommand);
            changeChannelPluginCommand.setTabCompleter(changeChannelCommand);
        }
    }

    @Override
    public void onDisable() {
        if (this.hikariDataSource != null) {
            this.hikariDataSource.close();
        }
    }

    /**
     * Gets all {@link Player}s in a given chat channel. This does not include distance and world checks.
     *
     * @param chatChannel The {@link ChatChannel} that all players will be in.
     * @return An {@link ArrayList} of {@link Player}s in the {@link ChatChannel}.
     */
    public @NotNull ArrayList<@NotNull Player> getPlayersInChannel(final @NotNull ChatChannel chatChannel) {
        final @NotNull ArrayList<@NotNull Player> playersInChannel = new ArrayList<>();
        for (final @NotNull Player player : this.getServer().getOnlinePlayers()) {
            final @NotNull ChatChannel playerChannel = this.getPlayerChatChannelOrDefault(player);
            if (chatChannel.equals(playerChannel)) {
                playersInChannel.add(player);
            }
        }
        return playersInChannel;
    }

    /**
     * Gets all {@link Player}s who can hear what the {@code speaker} is saying.
     *
     * @param speaker     The {@link Player} who is speaking.
     * @param chatChannel The {@link ChatChannel} the {@code speaker} is speaking in.
     * @return An {@link ArrayList} of {@link Player}s that can hear the {@code speaker}.
     */
    public @NotNull ArrayList<@NotNull Player> getPlayersWhoCanHear(final @NotNull Player speaker, final @NotNull ChatChannel chatChannel) {
        final @NotNull ArrayList<@NotNull Player> allPlayersInChannel = this.getPlayersInChannel(chatChannel);
        if (chatChannel.sameWorld()) {
            allPlayersInChannel.removeIf(player -> !player.getWorld().equals(speaker.getWorld()));
        }
        if (chatChannel.distance() > 0) {
            allPlayersInChannel.removeIf(player -> {
                try {
                    return speaker.getLocation().distance(player.getLocation()) > chatChannel.distance();
                } catch (final @NotNull IllegalArgumentException illegalArgumentException) {
                    return true;
                }
            });
        }
        return allPlayersInChannel;
    }

    /**
     * Gets all {@link ChatChannel}s present in the config.
     *
     * @return An {@link ArrayList} of all {@link ChatChannel}s defined in the configuration.
     */
    public @NotNull ArrayList<@NotNull ChatChannel> getChatChannels() {
        final @NotNull ArrayList<@NotNull ChatChannel> chatChannels = new ArrayList<>();
        for (final @NotNull Map<?, ?> channelConfiguration : this.getConfig().getMapList("channels")) {
            final @Nullable String name = (String) channelConfiguration.get("name");
            final boolean sameworld = (Boolean) channelConfiguration.get("sameworld");
            final int distance = (Integer) channelConfiguration.get("distance");

            if (name == null) {
                throw new RuntimeException("Missing name on channel.");
            }

            final @NotNull ChatChannel chatChannel = new ChatChannel(name, sameworld, distance);
            chatChannels.add(chatChannel);
        }
        return chatChannels;
    }

    /**
     * Gets a {@link ChatChannel} from its name.
     *
     * @param name The name of the {@link ChatChannel} in {@link String} form.
     * @return A {@link ChatChannel} with that name, or {@code null}.
     */
    public @Nullable ChatChannel getChatChannel(final @NotNull String name) {
        for (final @NotNull ChatChannel chatChannel : this.getChatChannels()) {
            if (chatChannel.name().equals(name)) {
                return chatChannel;
            }
        }
        return null;
    }

    /**
     * Adds a {@link ChatChannel} to the config.
     *
     * @param chatChannel A {@link ChatChannel} to add to the configuration.
     */
    public void registerChatChannel(final @NotNull ChatChannel chatChannel) {
        final @NotNull List<Map<?, ?>> existingList = this.getConfig().getMapList("channels"); // Not thread safe!
        existingList.add(chatChannel.asHashMap());
        this.getConfig().set("channels", existingList);
        this.saveConfig();
        this.reloadConfig();
    }

    /**
     * Removes a {@link ChatChannel} from the plugin's configuration.
     *
     * @param chatChannel The {@link ChatChannel} to remove from the plugin's configuration.
     */
    public void deregisterChatChannel(final @NotNull ChatChannel chatChannel) {
        final @NotNull List<Map<?, ?>> existingList = this.getConfig().getMapList("channels");
        // This only checks for the name, but that's ok since there should only be 1 entry with a name
        existingList.removeIf(map -> map.get("name").equals(chatChannel.name()));
        this.getConfig().set("channels", existingList);
        this.saveConfig();
        this.reloadConfig();
    }

    /**
     * Changes a {@link Player}'s chat channel.
     *
     * @param player      The {@link Player} that will have their row modified.
     * @param chatChannel The {@link ChatChannel} that the player will begin using.
     * @return {@code true} if the operation was successful.
     */
    public boolean setPlayerChatChannel(final @NotNull Player player, final @Nullable ChatChannel chatChannel) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO demochat (uuid, channel) VALUES(?, ?) ON DUPLICATE KEY UPDATE channel=?;");
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setString(2, chatChannel != null ? chatChannel.name() : null);
            preparedStatement.setString(3, chatChannel != null ? chatChannel.name() : null);

            preparedStatement.execute();

            return true;
        } catch (final @NotNull SQLException sqlException) {
            sqlException.printStackTrace();

            return false;
        }
    }

    /**
     * Changes a {@link Player}'s preference for using the filter.
     *
     * @param player The {@link Player} that will have their row modified.
     * @param filter {@code true} if the player will use the filter, {@code false} if they won't. The default is {@code false}.
     * @return {@code true} if the operation was successful.
     */
    public boolean setPlayerFilterPreference(final @NotNull Player player, final boolean filter) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO demochat (uuid, filter) VALUES(?, ?) ON DUPLICATE KEY UPDATE filter=?;");
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setBoolean(2, filter);
            preparedStatement.setBoolean(3, filter);

            preparedStatement.execute();

            return true;
        } catch (final @NotNull SQLException sqlException) {
            sqlException.printStackTrace();

            return false;
        }
    }

    /**
     * Gets a {@link Player}'s chat channel, or the default.
     *
     * @param player The {@link Player} whose row will be queried.
     * @return The {@link ChatChannel} that the player uses, or the default channel.
     */
    public @NotNull ChatChannel getPlayerChatChannelOrDefault(final @NotNull Player player) {
        final @Nullable ChatChannel possibleChatChannel = this.getPlayerChatChannel(player);
        return possibleChatChannel != null ? possibleChatChannel : this.getDefaultChatChannel();
    }

    /**
     * @return The default {@link ChatChannel} defined in the configuration.
     * @throws RuntimeException If an invalid {@link ChatChannel} was defined in the configuration.
     */
    public @NotNull ChatChannel getDefaultChatChannel() {
        final @Nullable ChatChannel defaultChatChannel = this.getChatChannel(Objects.requireNonNull(this.getConfig().getString("default_channel")));
        if (defaultChatChannel != null) {
            return defaultChatChannel;
        } else {
            throw new RuntimeException("Invalid default channel declared!");
        }
    }

    /**
     * Gets a {@link Player}'s chat channel.
     *
     * @param player The {@link Player} whose row will be queried.
     * @return The {@link ChatChannel} that the player uses, or {@code null} if it is not defined or not found.
     */
    public @Nullable ChatChannel getPlayerChatChannel(final @NotNull Player player) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM demochat WHERE uuid = ?;");
            preparedStatement.setString(1, player.getUniqueId().toString());

            final @NotNull ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.next();

            final @Nullable String preferredChatChannelName = resultSet.getString("channel");

            return preferredChatChannelName != null ? this.getChatChannel(preferredChatChannelName) : null;
        } catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.")) {
                sqlException.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Gets a {@link Player}'s preference for using the chat filter.
     *
     * @param player The {@link Player} whose row will be queried.
     * @return The {@link ChatChannel} that the player uses, or {@code null} if it is not defined or not found.
     */
    public boolean prefersFilter(final @NotNull Player player) {
        try (final @NotNull Connection connection = Objects.requireNonNull(this.hikariDataSource).getConnection()) {
            final @NotNull PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM demochat WHERE uuid = ?;");
            preparedStatement.setString(1, player.getUniqueId().toString());

            final @NotNull ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.next();

            return resultSet.getBoolean("filter");
        } catch (final @NotNull SQLException sqlException) {
            if (!sqlException.getMessage().equals("Illegal operation on empty result set.")) {
                sqlException.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Filters a {@link String} using the declared filters in the config.yml in a similar implementation to VentureChat.
     *
     * @param unformattedString The {@link String} to format.
     * @return The formatted {@link String}.
     */
    public @NotNull String filterString(final @NotNull String unformattedString) {
        @NotNull String formattedMessage = unformattedString;
        for (final @NotNull String filter : this.getConfig().getStringList("filters")) {
            int currentToken = 0;
            final String[] regexAndReplacement = {" ", " "};
            final @NotNull StringTokenizer tokenizer = new StringTokenizer(filter, ",");
            while (tokenizer.hasMoreTokens()) {
                if (currentToken < 2) {
                    regexAndReplacement[currentToken++] = tokenizer.nextToken(); // increments currentToken after it's use
                }
            }
            // (?i) = case insensitive
            formattedMessage = formattedMessage.replaceAll("(?i)" + regexAndReplacement[0] /* regex */, regexAndReplacement[1] /* replacement */);
        }
        return formattedMessage;
    }
}
