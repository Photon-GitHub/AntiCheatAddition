package de.photon.AACAdditionPro.modules;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public enum ModuleType
{
    // Additions
    LOG_BOT("LogBot"),

    // Normal checks
    AUTO_FISH("AutoFish"),
    AUTO_POTION("AutoPotion"),
    ESP("Esp"),
    FASTSWITCH("Fastswitch"),
    IMPOSSIBLE_CHAT("ImpossibleChat", "sent illegal chat message (ImpossibleChat)"),
    INVENTORY("Inventory", "has suspicious inventory interactions."),
    INVENTORY_HEURISTICS("InventoryHeuristics"),
    INVENTORY_HIT("InventoryHit", "failed Killaura/Triggerbot (InventoryHit)"),
    INVENTORY_MOVE("InventoryMove"),
    INVENTORY_ROTATION("InventoryRotation", "failed InventoryMove/Autoarmor (InventoryRotation)"),
    KILLAURA_ENTITY("KillauraEntity", "failed KillauraEntity (hit the Killaura-Bot)"),
    MULTI_INTERACTION("MultiInteraction"),
    PACKET_ANALYSIS("PacketAnalysis"),
    PINGSPOOF("Pingspoof"),
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
    SCHEMATICA_CONTROL("ClientControl.Schematica", "uses Schematica"),
    VAPE_CONTROL("ClientControl.Vape", "uses Vape"),
    VERSION_CONTROL("ClientControl.VersionControl"),
    WORLDDOWNLOAD_CONTROL("ClientControl.WorldDownloader", "uses WorldDownloader");

    private final String configString;
    private final String violationMessage;

    public final static Set<ModuleType> VL_MODULETYPES = new HashSet<>();

    ModuleType(final String configString)
    {
        this(configString, "failed " + configString);
    }

    ModuleType(final String configString, final String violationMessage)
    {
        this.configString = configString;
        this.violationMessage = violationMessage;
    }
}