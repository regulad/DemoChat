package xyz.regulad.demochat;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public record ChatChannel(@NotNull String name, boolean sameWorld, long distance) {
    public ChatChannel(final @NotNull String name, final boolean sameWorld, final long distance) {
        this.name = name;
        this.sameWorld = sameWorld;
        this.distance = distance;
    }

    /**
     * Constructs the {@link ChatChannel} as a {@link HashMap} that can be added to the configuration.
     *
     * @return A {@link HashMap} representing the {@link ChatChannel}.
     */
    public @NotNull HashMap<@NotNull Object, @NotNull Object> asHashMap() {
        final @NotNull HashMap<@NotNull Object, @NotNull Object> hashMap = new HashMap<>();
        hashMap.put("name", this.name);
        hashMap.put("sameworld", this.sameWorld);
        hashMap.put("distance", this.distance);
        return hashMap;
    }
}
