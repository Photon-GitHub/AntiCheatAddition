package de.photon.aacadditionpro.util.visibility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerVisibility
{
    private static final PlayerVisibility instance = new PlayerVisibility();

    private final PlayerInformationHider equipmentHider = new PlayerEquipmentHider();
    private final PlayerInformationHider playerHider = new PlayerHider();


    /**
     * This sets how much information of the watched {@link Player} the observing {@link Player} can obtain.
     *
     * @param observer the {@link Player} observing the watched {@link Player}.
     * @param watched  the {@link Player} that shall be (not at all / partially / fully) hidden from the observer.
     * @param hideMode the {@link HideMode} which defines the degree of hiding.
     */
    public void setPlayerVisibility(Player observer, Player watched, HideMode hideMode)
    {
        switch (hideMode) {
            case FULL:
                playerHider.hidePlayer(observer, watched);
                equipmentHider.revealPlayer(observer, watched);
                break;
            case INFORMATION_ONLY:
                equipmentHider.hidePlayer(observer, watched);
                playerHider.revealPlayer(observer, watched);
                break;
            case NONE:
                playerHider.revealPlayer(observer, watched);
                equipmentHider.revealPlayer(observer, watched);
                break;
            default:
                throw new IllegalStateException("Unknown Hidemode.");
        }
    }
}
