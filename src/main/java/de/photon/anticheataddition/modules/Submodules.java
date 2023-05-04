package de.photon.anticheataddition.modules;

import java.util.Set;

/**
 * This defines the handling of Sub-{@link Module}s of a {@link Module}.
 * Especially, it provides methods to activate and deactivate them.
 */
public interface Submodules
{
    Set<Module> getChildren();

    default void enableChildren()
    {
        for (Module module : getChildren()) module.activate();
    }

    default void disableChildren()
    {
        for (Module module : getChildren()) module.deactivate();
    }
}
