package de.photon.AACAdditionPro;

import org.bukkit.command.CommandSender;

public enum Permissions
{
    VERBOSE("aac.verbose"),
    INFO("aacadditionpro.info"),
    NEURAL("aacadditionpro.neural"),
    NEURAL_TRAIN("aacadditionpro.neural.train"),
    NEURAL_CHECK("aacadditionpro.neural.check");

    private final String realPermission;

    Permissions(final String realPermission)
    {
        this.realPermission = realPermission;
    }

    /**
     * This checks if a {@link CommandSender} has a certain permission.
     *
     * @param sender     the {@link CommandSender} who should be checked
     * @param permission the permission that he should be checked for
     *
     * @return true if the player has the permission, otherwise false
     */
    public static boolean hasPermission(final CommandSender sender, final Permissions permission)
    {
        return sender.hasPermission(permission.realPermission);
    }
}