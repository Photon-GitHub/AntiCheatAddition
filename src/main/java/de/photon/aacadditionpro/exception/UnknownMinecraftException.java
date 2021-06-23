package de.photon.aacadditionpro.exception;

public class UnknownMinecraftException extends IllegalStateException
{
    public UnknownMinecraftException()
    {
        super("Unknown minecraft version.");
    }
}
