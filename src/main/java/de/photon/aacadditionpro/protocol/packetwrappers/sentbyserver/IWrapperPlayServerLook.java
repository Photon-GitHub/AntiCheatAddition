package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayLook;
import de.photon.aacadditionpro.util.mathematics.RotationUtil;

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
