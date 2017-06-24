package de.photon.AACAdditionPro.addition;

import de.photon.AACAdditionPro.Module;

public interface Addition extends Module
{
    String getConfigString();

    @Override
    default void subEnable() {}

    @Override
    default void subDisable() {}
}
