package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum ModuleType
{
    // Additions
    BRAND_HIDER("BrandHider", "Hides the server version in F3 mode."),
    LOG_BOT("LogBot", "Automatically removes old logs."),

    // Normal checks
    AUTO_EAT("AutoEat"),
    AUTO_FISH("AutoFish"),
    AUTO_POTION("AutoPotion"),
    ESP("Esp"),
    FASTSWITCH("Fastswitch"),
    IMPOSSIBLE_CHAT("ImpossibleChat", "sent illegal chat message (ImpossibleChat)"),
    INVENTORY("Inventory", "has suspicious inventory interactions."),
    KEEPALIVE("KeepAlive", "Detects "),
    PACKET_ANALYSIS("PacketAnalysis"),
    SCAFFOLD("Scaffold"),
    SKINBLINKER("Skinblinker"),
    TEAMING("Teaming", "could be teaming"),
    TOWER("Tower"),

    // Client control
    BETTERSPRINTING_CONTROL("ClientControl.BetterSprinting", "uses BetterSprinting"),
    DAMAGE_INDICATOR("ClientControl.DamageIndicator", "uses DamageIndicator"),
    FIVEZIG_CONTROL("ClientControl.5zig", "uses 5zig"),
    FORGE_CONTROL("ClientControl.Forge", "uses Forge"),
    LABYMOD_CONTROL("ClientControl.LabyMod", "uses LabyMod"),
    LITELOADER_CONTROL("ClientControl.LiteLoader", "uses LiteLoader"),
    OLD_LABYMOD_CONTROL("ClientControl.OldLabyMod", "uses LabyMod"),
    PXMOD_CONTROL("ClientControl.PXMod", "uses PXMod"),
    SCHEMATICA_CONTROL("ClientControl.Schematica", "uses Schematica"),
    VAPE_CONTROL("ClientControl.Vape", "uses Vape"),
    VERSION_CONTROL("ClientControl.VersionControl"),
    WORLDDOWNLOAD_CONTROL("ClientControl.WorldDownloader", "uses WorldDownloader");

    public static final Set<ModuleType> VL_MODULETYPES = EnumSet.noneOf(ModuleType.class);
    private final String configString;
    private final String violationMessage;
    private String info;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean enabled;

    ModuleType(final String configString)
    {
        this(configString, "failed " + configString);
    }

    ModuleType(final String configString, final String violationMessage)
    {
        this.configString = configString;
        this.violationMessage = violationMessage;
    }

    public String getInfo()
    {
        if (info == null)
        {
            info = AACAdditionPro.getInstance().getConfig().getString(configString + ".aacfeatureinfo");
        }
        return info;
    }
}
