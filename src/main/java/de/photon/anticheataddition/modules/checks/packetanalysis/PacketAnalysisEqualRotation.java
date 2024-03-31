package de.photon.anticheataddition.modules.checks.packetanalysis;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.protocol.PacketEventUtils;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.concurrent.TimeUnit;

public final class PacketAnalysisEqualRotation extends ViolationModule implements Listener
{
    public static final PacketAnalysisEqualRotation INSTANCE = new PacketAnalysisEqualRotation();

    private PacketAnalysisEqualRotation()
    {
        super("PacketAnalysis.parts.EqualRotation");
    }

    private static boolean changedHitboxMaterialsPresent(User user)
    {
        return ServerVersion.is18() &&
               user.getClientVersion() != ServerVersion.MC18 &&
               SetUtil.containsAny(user.getHitboxLocation().getPartiallyIncludedMaterials(), MaterialUtil.INSTANCE.getChangedHitboxMaterials());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final PacketEventUtils.Rotation rotation = PacketEventUtils.getRotationFromEvent(event);

                    // Equal rotation.
                    // LookPacketData automatically updates these values.
                    if (rotation.yaw() != user.getData().floating.lastPacketYaw ||
                        rotation.pitch() != user.getData().floating.lastPacketPitch) return;

                    // Interactions with firework as well as entering and exiting vehicles fast can cause false positives.
                    if (user.getTimeMap().at(TimeKey.PACKET_ANALYSIS_EQUAL_ROTATION_INTERACTION).recentlyUpdated(100) ||
                        // Boat false positive (usually worse cheats in vehicles as well)
                        user.getPlayer().isInsideVehicle() ||
                        // Not recently teleported
                        user.hasTeleportedRecently(5000) ||
                        // 1.17 client false positive when throwing exp bottles.
                        user.getTimeMap().at(TimeKey.EXPERIENCE_BOTTLE_THROWN).recentlyUpdated(5000) ||
                        // LabyMod fp when standing still / hit in corner fp
                        !user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 100) ||
                        // 1.17 false positives
                        user.getTimeMap().at(TimeKey.HOTBAR_SWITCH).recentlyUpdated(3000) && user.hasSneakedRecently(3000) ||
                        user.getTimeMap().at(TimeKey.RIGHT_CLICK_ITEM_EVENT).recentlyUpdated(400) ||
                        !PacketAdapterBuilder.checkSync(10, TimeUnit.SECONDS,
                                                        // False positive when jumping from great heights into a pool with slime blocks / beds on the bottom.
                                                        () -> !(user.isInLiquids() && MaterialUtil.INSTANCE.getBounceMaterials().contains(user.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) &&
                                                              // Fixes false positives on versions 1.9+ because of changed hitboxes
                                                              !changedHitboxMaterialsPresent(user))) return;

                    // Cancelled packets may cause problems.
                    if (user.getData().bool.packetAnalysisEqualRotationExpected) {
                        user.getData().bool.packetAnalysisEqualRotationExpected = false;
                        return;
                    }

                    getManagement().flag(Flag.of(user).setDebug(() -> "PacketAnalysisData-Debug | Player: " + user.getPlayer().getName() + " sent equal rotations."));
                }).build();

        return ModuleLoader.builder(this)
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    private void handleEqualRotationExpectedInteraction(Player player)
    {
        final var user = User.getUser(player);
        if (User.isUserInvalid(user, this)) return;

        user.getTimeMap().at(TimeKey.PACKET_ANALYSIS_EQUAL_ROTATION_INTERACTION).update();
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event)
    {
        if (event.getEntered() instanceof Player player) {
            handleEqualRotationExpectedInteraction(player);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event)
    {
        if (event.getExited() instanceof Player player) {
            handleEqualRotationExpectedInteraction(player);
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 5).build();
    }
}
