package de.photon.AACAdditionPro.addition;

import de.photon.AACAdditionPro.Module;

public interface Addition extends Module
{
    @Override
    default void subEnable() {}

    @Override
    default void subDisable() {}

    @Override
    default String getName()
    {
        return this.getConfigString();
    }
}
