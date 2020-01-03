package de.photon.aacadditionpro.util.exceptions;

public class UnknownMinecraftVersion extends IllegalStateException
{
    public UnknownMinecraftVersion()
    {
        super("Unknown minecraft version.");
    }
}
