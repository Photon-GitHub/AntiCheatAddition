package de.photon.anticheataddition;

import lombok.Getter;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

public enum InternalPermission
{
    BYPASS("bypass"),
    DEBUG("debug"),
    INFO("info"),
    INTERNALTEST("internaltest"),
    MAINCOMMAND("maincomamnd"),
    SETVL("setvl");

    @Getter @NotNull private final String realPermission;

    InternalPermission(final String realPermission)
    {
        this.realPermission = "anticheataddition." + realPermission;
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(@NotNull final Permissible permissible, @NotNull final String permission)
    {
        return permissible.hasPermission(permission);
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public boolean hasPermission(@NotNull final Permissible permissible)
    {
        return hasPermission(permissible, this.realPermission);
    }
}
