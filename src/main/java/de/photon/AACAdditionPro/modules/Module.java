package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;

public interface Module
{
    /**
     * This enables the check according to its interfaces.
     */
    static void enableModule(final Module module)
    {
        try {
            // ServerVersion check
            if (module instanceof RestrictedServerVersion && !RestrictedServerVersion.allowedToStart((RestrictedServerVersion) module)) {
                sendNotice(module, module.getConfigString() + " is not compatible with your server version.");
                return;
            }

            // Enabled
            if (!AACAdditionPro.getInstance().getConfig().getBoolean(module.getConfigString() + ".enabled")) {
                sendNotice(module, module.getConfigString() + " was chosen not to be enabled.");
                return;
            }

            // Dependency check
            if (module instanceof Dependency && !Dependency.allowedToStart((Dependency) module)) {
                sendNotice(module, module.getConfigString() + " has been not been enabled as of missing dependencies.");
                return;
            }

            // Load the config values
            ConfigUtils.processLoadFromConfiguration(module, module.getConfigString());

            if (module instanceof ListenerModule) {
                ListenerModule.enable((ListenerModule) module);
            }

            if (module instanceof PacketListenerModule) {
                PacketListenerModule.enable((PacketListenerModule) module);
            }

            if (module instanceof PluginMessageListenerModule) {
                PluginMessageListenerModule.enable((PluginMessageListenerModule) module);
            }

            if (module instanceof PatternModule) {
                PatternModule.enablePatterns((PatternModule) module);
            }

            module.enable();

            // Make sure that parts don't change the state of the PatternModule
            if (!(module instanceof PatternModule.Pattern)) {
                module.getModuleType().setEnabled(true);
            }

            sendNotice(module, module.getConfigString() + " has been enabled.");
        } catch (final Exception e) {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be enabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * This disables the check according to its interfaces.
     */
    static void disableModule(final Module module)
    {
        try {
            if (module instanceof ListenerModule) {
                ListenerModule.disable((ListenerModule) module);
            }

            if (module instanceof PacketListenerModule) {
                PacketListenerModule.disable((PacketListenerModule) module);
            }

            if (module instanceof PluginMessageListenerModule) {
                PluginMessageListenerModule.disable((PluginMessageListenerModule) module);
            }

            if (module instanceof PatternModule) {
                PatternModule.disablePatterns((PatternModule) module);
            }

            module.disable();

            // Make sure that parts don't change the state of the PatternModule
            if (!(module instanceof PatternModule.Pattern)) {
                module.getModuleType().setEnabled(false);
            }

            sendNotice(module, module.getConfigString() + " has been disabled.");
        } catch (final Exception e) {
            VerboseSender.getInstance().sendVerboseMessage(module.getConfigString() + " could not be disabled.", true, true);
            e.printStackTrace();
        }
    }

    /**
     * Sends a message if {@link Module#shouldNotify()} returns true.
     */
    static void sendNotice(final Module module, final String message)
    {
        if (module.shouldNotify()) {
            VerboseSender.getInstance().sendVerboseMessage(message, true, false);
        }
    }

    /**
     * All additional chores during enabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void enable() {}

    /**
     * All additional chores during disabling that are not handled by the {@link Module} - subinterfaces.
     */
    default void disable() {}

    /**
     * Whether or not there are messages regarding this module when enabled/disabled.
     */
    default boolean shouldNotify()
    {
        return true;
    }

    /**
     * Gets the direct path representing this module in the config.
     */
    default String getConfigString()
    {
        return this.getModuleType().getConfigString();
    }

    /**
     * Gets the {@link ModuleType} of this {@link Module}
     */
    ModuleType getModuleType();
}
