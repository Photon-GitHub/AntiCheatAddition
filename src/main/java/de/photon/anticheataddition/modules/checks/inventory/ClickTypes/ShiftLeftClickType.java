package de.photon.anticheataddition.modules.checks.inventory.ClickTypes;

import org.bukkit.event.inventory.ClickType;

public class ShiftLeftClickType implements SupportedClickType {
    @Override
    public boolean isSupported(ClickType clickType) {
        return clickType == ClickType.SHIFT_LEFT;
    }
}

