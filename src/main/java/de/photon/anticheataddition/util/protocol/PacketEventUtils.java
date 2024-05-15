package de.photon.anticheataddition.util.protocol;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PacketEventUtils
{

    /**
     * Extracts the rotation (yaw and pitch) from a packet receive event.
     *
     * @param event the packet receive event
     *
     * @return a Rotation object containing the yaw and pitch
     *
     * @throws IllegalStateException if the packet type is not PLAYER_POSITION_AND_ROTATION or PLAYER_ROTATION
     */
    public static Rotation getRotationFromEvent(PacketReceiveEvent event)
    {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            final var wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            return new Rotation(wrapper.getYaw(), wrapper.getPitch());
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            final var wrapper = new WrapperPlayClientPlayerRotation(event);
            return new Rotation(wrapper.getYaw(), wrapper.getPitch());
        } else throw new IllegalStateException("Unexpected packet type: " + event.getPacketType());
    }

    /**
     * Record class representing the rotation of a player.
     */
    public record Rotation(float yaw, float pitch) {}
}
