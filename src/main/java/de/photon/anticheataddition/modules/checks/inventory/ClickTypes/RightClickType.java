package de.photon.anticheataddition.modules.checks.inventory.ClickTypes;

import org.bukkit.event.inventory.ClickType;

public class RightClickType implements SupportedClickType {
    @Override
    public boolean isSupported(ClickType clickType) {
        return clickType == ClickType.RIGHT;
    }
}
