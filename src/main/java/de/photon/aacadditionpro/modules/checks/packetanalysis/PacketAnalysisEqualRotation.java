package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.protocol.PacketAdapterBuilder;
import de.photon.aacadditionpro.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.minecraft.world.MaterialUtil;
import de.photon.aacadditionpro.util.oldmessaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.logging.Level.SEVERE;

public class PacketAnalysisEqualRotation extends ViolationModule
{
    public PacketAnalysisEqualRotation()
    {
        super("PacketAnalysis.parts.EqualRotation");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = PacketAdapterBuilder
                .of(PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK)
                .priority(ListenerPriority.LOW)
                .onReceiving(event -> {
                    val user = User.safeGetUserFromPacketEvent(event);
                    if (User.isUserInvalid(user, this)) return;

                    // Get the packet.
                    final IWrapperPlayClientLook lookWrapper = event::getPacket;

                    val currentYaw = lookWrapper.getYaw();
                    val currentPitch = lookWrapper.getPitch();

                    // Boat false positive (usually worse cheats in vehicles as well)
                    if (!user.getPlayer().isInsideVehicle() &&
                        // Not recently teleported
                        !user.hasTeleportedRecently(5000) &&
                        // Same rotation values
                        // LookPacketData automatically updates its values.
                        currentYaw == user.getDataMap().getFloat(DataKey.FloatKey.LAST_PACKET_YAW) &&
                        currentPitch == user.getDataMap().getFloat(DataKey.FloatKey.LAST_PACKET_PITCH) &&
                        // 1.17 client false positive when throwing exp bottles.
                        user.getTimestampMap().at(TimestampKey.LAST_EXPERIENCE_BOTTLE_THROWN).notRecentlyUpdated(5000) &&
                        // LabyMod fp when standing still / hit in corner fp
                        user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 100) &&
                        // 1.17 false positives
                        !(user.getTimestampMap().at(TimestampKey.LAST_HOTBAR_SWITCH).recentlyUpdated(3000) && user.hasSneakedRecently(3000)))
                    {
                        // Not a big performance deal as most packets have already been filtered out, now we just account for the last false positives.
                        // Sync call because the isHitboxInLiquids method will load chunks (prevent errors).
                        try {
                            if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () ->
                                    // False positive when jumping from great heights into a pool with slime blocks / beds on the bottom.
                                    !(user.isInLiquids() &&
                                      MaterialUtil.BOUNCE_MATERIALS.contains(user.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) &&
                                    // Fixes false positives on versions 1.9+ because of changed hitboxes
                                    !(ServerVersion.is18() &&
                                      ServerVersion.getClientServerVersion(user.getPlayer()) != ServerVersion.MC18 &&
                                      MaterialUtil.containsMaterials(user.getHitbox().getPartiallyIncludedMaterials(user.getPlayer().getLocation()), MaterialUtil.CHANGED_HITBOX_MATERIALS))).get(10, TimeUnit.SECONDS)))
                            {
                                // Cancelled packets may cause problems.
                                if (user.getDataMap().getBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED)) {
                                    user.getDataMap().setBoolean(DataKey.BooleanKey.PACKET_ANALYSIS_EQUAL_ROTATION_EXPECTED, false);
                                    return;
                                }

                                getManagement().flag(Flag.of(user).setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent equal rotations.")));
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            AACAdditionPro.getInstance().getLogger().log(SEVERE, "Unable to complete the EqualRotation calculations.", e);
                            Thread.currentThread().interrupt();
                        } catch (TimeoutException e) {
                            AACAdditionPro.getInstance().getLogger().log(SEVERE, "Discard packet check due to high server load. If this message appears frequently please consider upgrading your server.");
                        }
                    }
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(200, 3).build();
    }
}
