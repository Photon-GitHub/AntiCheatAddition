package de.photon.anticheataddition.modules.checks.inventory.ClickTypes;

import org.bukkit.event.inventory.ClickType;

public interface SupportedClickType {
    boolean isSupported(ClickType clickType);
}
