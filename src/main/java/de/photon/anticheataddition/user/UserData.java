package de.photon.anticheataddition.user;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.Data;
import de.photon.anticheataddition.user.data.TimestampMap;
import de.photon.anticheataddition.user.data.subdata.BrandChannelData;
import de.photon.anticheataddition.user.data.subdata.LookPacketData;
import de.photon.anticheataddition.util.mathematics.Hitbox;
import lombok.Getter;

@Getter
public class UserData {
    private final Data data = new Data();
    private final TimestampMap timeMap = new TimestampMap();

    private final BrandChannelData brandChannelData = new BrandChannelData();
    private final LookPacketData lookPacketData = new LookPacketData();

    public boolean isBypassed(User user, String bypassPermission) {
        Preconditions.checkArgument(bypassPermission.startsWith(InternalPermission.BYPASS.getRealPermission()), "Invalid bypass permission");
        return InternalPermission.hasPermission(user.getPlayer(), bypassPermission);
    }

    public boolean inAdventureOrSurvivalMode(User user) {
        return User.inAdventureOrSurvivalMode(user.getPlayer());
    }

    public Hitbox.HitboxLocation getHitboxLocation(User user) {
        return Hitbox.hitboxLocationOf(user.getPlayer());
    }

    public boolean isInLiquids(User user) {
        return getHitboxLocation(user).isInLiquids();
    }

    // Other methods remain unchanged
}
