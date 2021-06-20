package de.photon.aacadditionpro.modules.checks.packetanalysis;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ModulePacketAdapter;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.MaterialUtil;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.logging.Level.SEVERE;

public class PacketAnalysisEqualRotation extends ViolationModule
{
    // A set of materials which hitboxes changed in minecraft 1.9
    private static final Set<Material> CHANGED_HITBOX_MATERIALS = ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ? Sets.immutableEnumSet(Material.getMaterial("STAINED_GLASS_PANE"),
                                                                                                                                                       Material.getMaterial("THIN_GLASS"),
                                                                                                                                                       Material.getMaterial("IRON_FENCE"),
                                                                                                                                                       Material.CHEST,
                                                                                                                                                       Material.ANVIL) : ImmutableSet.of();

    public PacketAnalysisEqualRotation()
    {
        super("PacketAnalysis.parts.EqualRotation");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val packetAdapter = new EqualRotationAdapter(this);
        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(200, 1).build();
    }

    private class EqualRotationAdapter extends ModulePacketAdapter
    {
        public EqualRotationAdapter(Module module)
        {
            super(module, ListenerPriority.LOW, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK);
        }

        @Override
        public void onPacketReceiving(final PacketEvent packetEvent)
        {
            val user = User.safeGetUserFromPacketEvent(packetEvent);
            if (User.isUserInvalid(user, this.getModule())) return;

            // Get the packet.
            final IWrapperPlayClientLook lookWrapper = packetEvent::getPacket;

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
                // Labymod fp when standing still / hit in corner fp
                user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 100))
            {
                // Not a big performance deal as most packets have already been filtered out, now we just account for
                // the last false positives.
                // Sync call because the isHitboxInLiquids method will load chunks (prevent errors).
                try {
                    if (Boolean.TRUE.equals(Bukkit.getScheduler().callSyncMethod(AACAdditionPro.getInstance(), () ->
                            // False positive when jumping from great heights into a pool with slime blocks on the bottom.
                            !(Hitbox.PLAYER.isInLiquids(user.getPlayer().getLocation()) &&
                              user.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SLIME_BLOCK) &&
                            // Fixes false positives on versions 1.9+ because of changed hitboxes
                            !(ServerVersion.getActiveServerVersion() == ServerVersion.MC18 &&
                              ServerVersion.getClientServerVersion(user.getPlayer()) != ServerVersion.MC18 &&
                              MaterialUtil.containsMaterials(Hitbox.PLAYER.getPartiallyIncludedMaterials(user.getPlayer().getLocation()), CHANGED_HITBOX_MATERIALS))).get(10, TimeUnit.SECONDS)))
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
        }
    }
}
