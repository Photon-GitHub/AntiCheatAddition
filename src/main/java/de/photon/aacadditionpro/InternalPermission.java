package de.photon.aacadditionpro;

import lombok.Getter;
import org.bukkit.permissions.Permissible;

public enum InternalPermission
{
    AAC_VERBOSE("aac.manage"),
    AAC_MANAGE("aac.verbose"),
    BYPASS("aacadditionpro.bypass"),
    INFO("aacadditionpro.info"),
    TABLISTREMOVE("aacadditionpro.tablistremove");

    @Getter
    private final String realPermission;

    InternalPermission(final String realPermission)
    {
        this.realPermission = realPermission;
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(final Permissible permissible, final String permission)
    {
        return permission == null || permissible.hasPermission(permission);
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     * @param permission  the permission that he should be checked for
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public static boolean hasPermission(final Permissible permissible, final InternalPermission permission)
    {
        return permission == null || permission.hasPermission(permissible);
    }

    /**
     * This checks if a {@link Permissible} has a certain permission.
     *
     * @param permissible the {@link Permissible} who should be checked
     *
     * @return true if the player has the permission or is op, otherwise false
     */
    public boolean hasPermission(final Permissible permissible)
    {
        return hasPermission(permissible, this.realPermission);
    }
}