package xyz.regulad.demochat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.regulad.demochat.listener.ChatListener;
import xyz.regulad.demochat.util.DistanceUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

public class DemoChatPlugin extends JavaPlugin {
    private final @NotNull HikariConfig hikariConfig = new HikariConfig();
    private @Nullable HikariDataSource hikariDataSource = null;

    @Override
    public void onEnable() {
        // Save config
        this.saveDefaultConfig();

        // Configure MySQL
        this.hikariConfig.setJdbcUrl("jdbc:mysql://" + this.getConfig().getString("database.host") + ":" + this.getConfig().getInt("database.port") + "/" + this.getConfig().getString("database.db_name") + this.getConfig().getString("database.options"));
        this.hikariDataSource = new HikariDataSource(this.hikariConfig);
        this.getLogger().info("Successfully connected to the MySQL database.");

        try {
            // Create channel table
            this.hikariDataSource.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS demochat (uuid CHAR(36) UNIQUE, channel VARCHAR(256) DEFAULT null, filter INT DEFAULT 0, PRIMARY KEY (uuid));").execute();
        } catch (final @NotNull SQLException sqlException) {
            sqlException.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    public @NotNull Connection getConnection() throws SQLException {
        return Objects.requireNonNull(this.hikariDataSource).getConnection();
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
            final @Nullable ChatChannel playerChannel = this.getPlayerChatChannel(player);
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
                    return DistanceUtil.interDimensionDistance(speaker.getLocation(), player.getLocation()) < chatChannel.distance();
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
            final long distance = (Long) channelConfiguration.get("distance");

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
        this.getConfig().getMapList("channels").add(chatChannel.asHashMap());
    }

    /**
     * @param chatChannel The {@link ChatChannel} to remove from the plugin's
     * @return {@code true} if the item was removed from the list.
     */
    public boolean deregisterChatChannel(final @NotNull ChatChannel chatChannel) {
        return this.getConfig().getMapList("channels").remove(chatChannel.asHashMap());
    }

    /**
     * Changes a {@link Player}'s chat channel.
     *
     * @param player      The {@link Player} that will have their row modified.
     * @param chatChannel The {@link ChatChannel} that the player will begin using.
     * @return {@code true} if the operation was successful.
     */
    public boolean setPlayerChatChannel(final @NotNull Player player, final @Nullable ChatChannel chatChannel) {
        try {
            final @NotNull PreparedStatement preparedStatement = this.getConnection().prepareStatement("INSERT INTO demochat (uuid, channel) VALUES(?, ?) ON DUPLICATE KEY UPDATE channel=?;");
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
        try {
            final @NotNull PreparedStatement preparedStatement = this.getConnection().prepareStatement("INSERT INTO demochat (uuid, filter) VALUES(?, ?) ON DUPLICATE KEY UPDATE filter=?;");
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
        for (final @NotNull ChatChannel chatChannel : this.getChatChannels()) {
            if (chatChannel.name().equals(this.getConfig().getString("default_channel"))) {
                return chatChannel;
            }
        }
        throw new RuntimeException("Invalid default channel declared!");
    }

    /**
     * Gets a {@link Player}'s chat channel.
     *
     * @param player The {@link Player} whose row will be queried.
     * @return The {@link ChatChannel} that the player uses, or {@code null} if it is not defined or not found.
     */
    public @Nullable ChatChannel getPlayerChatChannel(final @NotNull Player player) {
        try {
            final @NotNull PreparedStatement preparedStatement = this.getConnection().prepareStatement("SELECT * FROM demochat WHERE uuid = ?;");
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
        try {
            final @NotNull PreparedStatement preparedStatement = this.getConnection().prepareStatement("SELECT * FROM demochat WHERE uuid = ?;");
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

    /**
     * Filters a {@link TextComponent} using the declared filters in the config.yml in a similar implementation to VentureChat.
     *
     * @param unformattedComponent The {@link TextComponent} to format.
     * @return The formatted {@link TextComponent}.
     */
    public @NotNull TextComponent filterComponent(final @NotNull TextComponent unformattedComponent) {
        return unformattedComponent.content(filterString(unformattedComponent.content()));
    }
}
