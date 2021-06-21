package de.photon.aacadditionpro;

import de.photon.aacadditionpro.user.data.DataKey;
import lombok.val;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserTest
{
    @Test
    public void materialTest()
    {
        Dummy.mockAACAdditionPro();

        val obsidian = Material.OBSIDIAN;
        val user = Dummy.mockUser();

        user.getDataMap().setObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED, obsidian);
        Assertions.assertSame(user.getDataMap().getObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED), obsidian);
    }
}
