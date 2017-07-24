package de.photon.AACAdditionPro.api;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class KillauraEntityAddon
{
    private final JavaPlugin plugin;
    private KillauraEntityController controller;

    protected KillauraEntityAddon(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    public abstract WrappedGameProfile getKillauraEntityGameProfile(Player player);

    /**
     * @return Whether the KillauraEntity is enabled and no other pulgin overrided your addon
     */
    protected final boolean isActive()
    {
        return controller != null;
    }

    public final JavaPlugin getPlugin()
    {
        return this.plugin;
    }

    public final KillauraEntityController getController()
    {
        return this.controller;
    }
}
