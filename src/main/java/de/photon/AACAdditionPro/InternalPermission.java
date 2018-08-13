package de.photon.AACAdditionPro;

import lombok.Getter;
import org.bukkit.permissions.Permissible;

public enum InternalPermission
{
    BYPASS("aacadditionpro.bypass"),
    VERBOSE("aac.verbose"),
    INFO("aacadditionpro.info"),
    ENTITYCHECK("aacadditionpro.entitycheck"),
    NEURAL("aacadditionpro.neural"),
    NEURAL_CREATE("aacadditionpro.neural.create"),
    NEURAL_TRAIN("aacadditionpro.neural.train"),
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
    public static boolean hasPermission(final Permissible permissible, final InternalPermission permission)
    {
        return hasPermission(permissible, permission.realPermission);
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
        return permission == null || permissible.isOp() || permissible.hasPermission(permission);
    }
}