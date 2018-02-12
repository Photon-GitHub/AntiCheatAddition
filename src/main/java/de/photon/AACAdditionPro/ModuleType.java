package de.photon.AACAdditionPro;

import lombok.Getter;

@Getter
public enum ModuleType
{
    // Additions
    LOG_BOT("LogBot"),
    PER_HEURISTIC_COMMANDS("Heuristic-Addition"),

    // Normal checks
    AUTO_FISH("AutoFish"),
    AUTO_POTION("AutoPotion"),
    EQUAL_ROTATION("EqualRotation"),
    ESP("Esp"),
    FASTSWITCH("Fastswitch"),
    FLYPATCH("FlyPatch"),
    FREECAM("Freecam"),
    GRAVITATIONAL_MODIFIER("GravitationalModifier"),
    INVENTORY_CHAT("InventoryChat", "sent chat message while in Inventory (InventoryChat)"),
    INVENTORY_HEURISTICS("InventoryHeuristics"),
    INVENTORY_HIT("InventoryHit", "failed Killaura/Triggerbot (InventoryHit)"),
    INVENTORY_MOVE("InventoryMove"),
    INVENTORY_ROTATION("InventoryRotation", "failed InventoryMove/Autoarmor (InventoryRotation)"),
    KILLAURA_ENTITY("KillauraEntity", "failed KillauraEntity (hit the Killaura-Bot)"),
    MULTI_INTERACTION("MultiInteraction"),
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
    WORLDDOWNLOAD_CONTROL("ClientControl.WorldDownloader", "uses WorldDownloader");

    private final String configString;
    private final String violationMessage;

    ModuleType(final String configString, final String violationMessage)
    {
        this.configString = configString;
        this.violationMessage = violationMessage;
    }

    ModuleType(final String configString)
    {
        this.configString = configString;
        this.violationMessage = "failed " + configString;
    }
}