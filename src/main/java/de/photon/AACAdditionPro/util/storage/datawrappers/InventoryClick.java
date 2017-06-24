package de.photon.AACAdditionPro.util.storage.datawrappers;

import de.photon.AACAdditionPro.userdata.User;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;

@RequiredArgsConstructor(suppressConstructorProperties = true)
public class InventoryClick
{
    public final User user;
    public final Material type;
    public final long timeStamp = System.currentTimeMillis();
    public final int position;
    public final int clickType;
}
