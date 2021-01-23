package de.photon.aacadditionproold.util.exceptions;

public class UnknownMinecraftVersion extends IllegalStateException
{
    public UnknownMinecraftVersion()
    {
        super("Unknown minecraft version.");
    }
}
