package de.photon.aacadditionpro.util.visibility

import org.bukkit.entity.Player

object PlayerVisibility {
    private val equipmentHider: PlayerInformationHider = PlayerEquipmentHider()
    private val playerHider: PlayerInformationHider = PlayerHider()

    /**
     * This method will fully hide the toBeHidden [Player] from the observer [Player]
     */
    fun fullyHidePlayer(observer: Player?, toBeHidden: Player?) {
        playerHider.hidePlayer(observer, toBeHidden)
        equipmentHider.revealPlayer(observer, toBeHidden)
    }

    /**
     * This method will hide the equipment of the hideEquipment [Player] from the observer [Player]
     */
    fun hideEquipment(observer: Player?, hideEquipment: Player?) {
        equipmentHider.hidePlayer(observer, hideEquipment)
        playerHider.revealPlayer(observer, hideEquipment)
    }

    /**
     * This method will fully reveal the toBeRevealed [Player] from the observer [Player]
     */
    fun revealPlayer(observer: Player?, toBeRevealed: Player?) {
        playerHider.revealPlayer(observer, toBeRevealed)
        equipmentHider.revealPlayer(observer, toBeRevealed)
    }

    fun enable() {
        equipmentHider.registerListeners()
        playerHider.registerListeners()
    }

    fun disable() {
        equipmentHider.unregisterListeners()
        equipmentHider.clear()
        playerHider.unregisterListeners()
        playerHider.clear()
    }
}