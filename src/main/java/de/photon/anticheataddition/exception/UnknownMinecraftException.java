package de.photon.anticheataddition.exception;

public class UnknownMinecraftException extends IllegalStateException
{
    public UnknownMinecraftException()
    {
        super("Unknown minecraft version.");
    }
}
