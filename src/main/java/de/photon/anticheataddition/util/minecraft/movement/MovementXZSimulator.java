package de.photon.anticheataddition.util.minecraft.movement;

import org.bukkit.event.player.PlayerMoveEvent;

public enum MovementXZSimulator {
    INSTANCE;

    public double getMovementXZSimulator(PlayerMoveEvent e) {
        return Math.max(Math.abs(e.getFrom().getX() - e.getTo().getX()), Math.abs(e.getFrom().getZ() - e.getTo().getZ()));
    }
}
