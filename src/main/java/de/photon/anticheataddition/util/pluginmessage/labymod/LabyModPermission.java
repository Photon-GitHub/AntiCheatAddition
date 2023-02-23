package de.photon.anticheataddition.util.pluginmessage.labymod;

import com.google.gson.JsonObject;
import de.photon.anticheataddition.modules.sentinel.mods.LabyModSentinel;
import lombok.Getter;

import java.util.Locale;

enum LabyModPermission
{
    // Permissions that are disabled by default
    IMPROVED_LAVA(false),
    CROSSHAIR_SYNC(false),
    REFILL_FIX(false),
    RANGE(false), // CLASSIC PVP - 1.16 only
    SLOWDOWN(false), // CLASSIC PVP - 1.16 only

    // GUI permissions
    GUI_ALL(true),
    GUI_POTION_EFFECTS(true),
    GUI_ARMOR_HUD(true),
    GUI_ITEM_HUD(true),

    // Permissions that are enabled by default
    BLOCKBUILD(true),
    TAGS(true),
    CHAT(true),
    ANIMATIONS(true),
    SATURATION_BAR(true);

    @Getter(lazy = true) private static final JsonObject permissionsJson = generatePermissionsJson();

    private static JsonObject generatePermissionsJson()
    {
        final var json = new JsonObject();
        for (LabyModPermission value : LabyModPermission.values()) json.addProperty(value.name(), value.configValue);
        return json;
    }

    private final boolean configValue;

    /**
     * @param defaultEnabled whether this permission is enabled/activated by default
     */
    LabyModPermission(boolean defaultEnabled)
    {
        this.configValue = LabyModSentinel.INSTANCE.loadBoolean(".disable" + this.name().toLowerCase(Locale.ENGLISH), defaultEnabled);
    }
}
