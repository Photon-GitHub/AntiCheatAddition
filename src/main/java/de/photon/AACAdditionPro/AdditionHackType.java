package de.photon.AACAdditionPro;

import lombok.Getter;

@Getter
public enum AdditionHackType
{
    // Normal checks
    AUTO_FISH("AutoFish"),
    AUTO_POTION("AutoPotion"),
    BLINDNESS_SPRINT("BlindnessSprint", "failed Sprint (Blindness)"),
    EQUAL_ROTATION("EqualRotation"),
    ESP("Esp"),
    FASTSWITCH("Fastswitch"),
    FREECAM("Freecam"),
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
    FIVEZIG_CONTROL("ClientControl.5zig", "uses 5zig"),
    FORGE_CONTROL("ClientControl.Forge",  "uses Forge"),
    LABYMOD_CONTROL("ClientControl.LabyMod",  "uses LabyMod"),
    LITELOADER_CONTROL("ClientControl.LiteLoader",  "uses LiteLoader"),
    SCHEMATICA_CONTROL("ClientControl.Schematica",  "uses Schematica"),
    WORLDDOWNLOAD_CONTROL("ClientControl.WorldDownloader",  "uses WorldDownloader");

    private final String configString;
    private final String violationMessage;

    private final static String CLIENTCONTROL_PATTERN = "CONTROL";

    AdditionHackType(final String configString, final String violationMessage)
    {
        this.configString = configString;
        this.violationMessage = violationMessage;
    }

    AdditionHackType(final String configString)
    {
        this.configString = configString;
        this.violationMessage = "failed " + configString;
    }
}