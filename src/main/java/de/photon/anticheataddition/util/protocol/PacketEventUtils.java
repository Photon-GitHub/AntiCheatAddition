package de.photon.anticheataddition.util.protocol;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PacketEventUtils
{
    public static Rotation getRotationFromEvent(PacketReceiveEvent event)
    {
        final float currentYaw;
        final float currentPitch;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            final var wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            currentYaw = wrapper.getYaw();
            currentPitch = wrapper.getPitch();
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            final var wrapper = new WrapperPlayClientPlayerRotation(event);
            currentYaw = wrapper.getYaw();
            currentPitch = wrapper.getPitch();
        } else {
            throw new IllegalStateException("Unexpected packet type: " + event.getPacketType());
        }

        return new Rotation(currentYaw, currentPitch);
    }

    public record Rotation(float yaw, float pitch) {}
}
