package de.photon.aacadditionpro.user.subdata;

import de.photon.aacadditionpro.user.User;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class ConsumeData extends SubData
{
    private ItemStack lastConsumedItemStack;

    public ConsumeData(User user)
    {
        super(user);
    }
}
