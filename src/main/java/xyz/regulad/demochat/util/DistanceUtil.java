package xyz.regulad.demochat.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class DistanceUtil {
    /**
     * Calculates the distance between two {@link Location}s, even if they are in different worlds.
     *
     * @param from The {@link Location} distance is being calculated from.
     * @param to   The {@link Location} distance is being calculated to.
     * @return The absolute distance between {@code from} and {@code to}, with world scaling.
     * @throws IllegalArgumentException If one {@link Location} is in The End.
     */
    public static double interDimensionDistance(final @NotNull Location from, final @NotNull Location to) {
        if (from.getWorld().getEnvironment().equals(to.getWorld().getEnvironment())) { // These are in worlds with similar environments.
            if (from.getWorld().equals(to.getWorld())) {
                return from.distance(to); // We can use the bukkit shortcut
            } else {
                return from.distance(new Location(from.getWorld(), to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch())); // A little hacky, but it'll do.
            }
        } else if (from.getWorld().getEnvironment().equals(World.Environment.NETHER) && to.getWorld().getEnvironment().equals(World.Environment.NORMAL)) { // from is the nether, to is the overworld
            return to.distance(new Location(to.getWorld(), from.getX() / 8, from.getY() / 8, from.getZ() / 8, from.getYaw(), from.getPitch())); // A little hacky, but it'll do.
        } else if (to.getWorld().getEnvironment().equals(World.Environment.NETHER) && from.getWorld().getEnvironment().equals(World.Environment.NORMAL)) { // to is the nether, from is the overworld
            return from.distance(new Location(from.getWorld(), to.getX() / 8, to.getY() / 8, to.getZ() / 8, to.getYaw(), to.getPitch())); // A little hacky, but it'll do.
        } else {
            throw new IllegalArgumentException("Cannot measure distance between " + from.getWorld().getName() + " and " + to.getWorld().getName());
        }
    }
}
