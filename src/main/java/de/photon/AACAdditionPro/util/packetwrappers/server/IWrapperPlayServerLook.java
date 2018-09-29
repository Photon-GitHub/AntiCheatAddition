package de.photon.AACAdditionPro.util.packetwrappers.server;

import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayLook;

public interface IWrapperPlayServerLook extends IWrapperPlayLook
{
    /**
     * This is a method to support multiple packets in one interface.
     * Minecraft 1.8.8 has various other packet layouts that make this method one necessary.
     */
    default int getByteOffset()
    {
        return 0;
    }

    @Override
    default float getYaw()
    {
        return RotationUtil.convertFixedRotation(getHandle().getBytes().read(getByteOffset()));
    }

    @Override
    default void setYaw(float value)
    {
        getHandle().getBytes().write(getByteOffset(), RotationUtil.getFixRotation(value));
    }

    @Override
    default float getPitch()
    {
        return RotationUtil.convertFixedRotation(getHandle().getBytes().read(1 + getByteOffset()));
    }

    @Override
    default void setPitch(float value)
    {
        getHandle().getBytes().write(1 + getByteOffset(), RotationUtil.getFixRotation(value));
    }
}
