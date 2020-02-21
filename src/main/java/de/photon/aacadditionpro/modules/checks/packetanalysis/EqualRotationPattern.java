package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.PositionData;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.logging.Level.SEVERE;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.PacketPattern} checks for rotation packets which have
 * exactly the same yaw/pitch values as the last packet. When moving these values are never equal to each other when
 * sent by a vanilla client.
 */
class EqualRotationPattern extends PatternModule.PacketPattern
{
    // A set of materials which hitboxes changed in minecraft 1.9
    private static final Set<Material> CHANGED_HITBOX_MATERIALS;

    static {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                CHANGED_HITBOX_MATERIALS = Collections.unmodifiableSet(EnumSet.of(Material.getMaterial("STAINED_GLASS_PANE"),
                                                                                  Material.getMaterial("THIN_GLASS"),
                                                                                  Material.getMaterial("IRON_FENCE"),
                                                                                  Material.CHEST,
                                                                                  Material.ANVIL));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Hitbox bugs are fixed in higher versions.
                CHANGED_HITBOX_MATERIALS = Collections.emptySet();
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    EqualRotationPattern()
    {
        super(ImmutableSet.of(PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK));
    }

    @Override
    protected int process(User user, PacketEvent packetEvent)
    {
        // Get the packet.
        final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

        final float currentYaw = lookWrapper.getYaw();
        final float currentPitch = lookWrapper.getPitch();

        // Boat false positive (usually worse cheats in vehicles as well)
        if (!user.getPlayer().isInsideVehicle() &&
            // Not recently teleported
            !user.getTeleportData().recentlyUpdated(0, 5000) &&
            // Same rotation values
            // LookPacketData automatically updates its values.
            currentYaw == user.getLookPacketData().getRealLastYaw() &&
            currentPitch == user.getLookPacketData().getRealLastPitch() &&
            // Labymod fp when standing still / hit in corner fp
            user.getPositionData().hasPlayerMovedRecently(100, PositionData.MovementType.XZONLY))
        {
            // Not a big performance deal as most packets have already been filtered out, now we just account for
            // the last false positives.
            // Sync call because the isHitboxInLiquids method will load chunks (prevent errors).
            try {
                if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () ->
                        // False positive when jumping from great heights into a pool with slime blocks on the bottom.
                        !(EntityUtil.isHitboxInLiquids(user.getPlayer().getLocation(),
                                                       user.getPlayer().isSneaking() ?
                                                       Hitbox.SNEAKING_PLAYER :
                                                       Hitbox.PLAYER) &&
                          user.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK) &&
                        // Fixes false positives on versions 1.9+ because of changed hitboxes
                        !(ServerVersion.getActiveServerVersion() == ServerVersion.MC188 &&
                          ServerVersion.getClientServerVersion(user.getPlayer()) != ServerVersion.MC188 &&
                          EntityUtil.isHitboxInMaterials(user.getPlayer().getLocation(),
                                                         user.getPlayer().isSneaking() ?
                                                         Hitbox.SNEAKING_PLAYER :
                                                         Hitbox.PLAYER, CHANGED_HITBOX_MATERIALS))).get(10, TimeUnit.SECONDS)))
                {
                    // Cancelled packets may cause problems.
                    if (user.getPacketAnalysisData().equalRotationExpected) {
                        user.getPacketAnalysisData().equalRotationExpected = false;
                        return 0;
                    }

                    message = "PacketAnalysisData-Verbose | Player: " + user.getPlayer().getName() + " sent equal rotations.";
                    return 1;
                }
            } catch (InterruptedException | ExecutionException e) {
                AACAdditionPro.getInstance().getLogger().log(SEVERE, "Unable to complete the EqualRotation calculations.", e);
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                AACAdditionPro.getInstance().getLogger().log(SEVERE, "Discard packet check due to high server load. If this message appears frequently please consider upgrading your server.");
            }
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.EqualRotation";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PACKET_ANALYSIS;
    }
}
