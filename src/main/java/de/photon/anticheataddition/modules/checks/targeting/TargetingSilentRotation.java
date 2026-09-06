package de.photon.anticheataddition.modules.checks.targeting;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.TargetingData;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.world.entity.EntityUtil;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Detects repeated use-item rotations which differ from the player's accepted camera rotation.
 */
public final class TargetingSilentRotation extends ViolationModule
{
    public static final TargetingSilentRotation INSTANCE = new TargetingSilentRotation();

    private static final Set<String> AIM_DEPENDENT_ITEMS = Set.of(
            "BOW", "CROSSBOW", "TRIDENT", "FISHING_ROD", "ENDER_PEARL", "SNOWBALL", "EGG", "WIND_CHARGE",
            "FIRE_CHARGE", "ENDER_EYE", "SPLASH_POTION", "LINGERING_POTION", "EXPERIENCE_BOTTLE", "FIREWORK_ROCKET");
    private static final long FLAG_COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(1L);

    private final double mismatchDegrees = Math.max(0.1D, loadDouble(".mismatch_degrees", 2D));
    private final long comparisonWindowNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(1L,
                                                                                       loadInt(".comparison_window_ms", 250)));
    private final int requiredMismatches = Math.max(1, loadInt(".required_mismatches", 8));
    private final int eligibleSamples = Math.max(requiredMismatches, loadInt(".eligible_samples", 12));

    private TargetingSilentRotation()
    {
        super("Targeting.parts.SilentRotation");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var packetAdapter = PacketAdapterBuilder
                .of(this, PacketType.Play.Client.USE_ITEM)
                .priority(PacketListenerPriority.LOW)
                .onReceiving((event, user) -> {
                    final long timestamp = System.nanoTime();
                    if (user.getPlayer().isInsideVehicle() ||
                        user.getPlayer().isFlying() ||
                        EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) ||
                        user.getTargetingData().isTargetingSuppressed(timestamp) ||
                        user.getData().object.packetFloodData.isThrottled(timestamp)) return;

                    final WrapperPlayClientUseItem wrapper;
                    try {
                        wrapper = new WrapperPlayClientUseItem(event);
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    if (!Double.isFinite(wrapper.getYaw()) ||
                        !Double.isFinite(wrapper.getPitch()) ||
                        wrapper.getPitch() < -90F ||
                        wrapper.getPitch() > 90F) {
                        getManagement().flag(Flag.of(user)
                                                 .setAddedVl(150)
                                                 .setDebug(() -> "TargetingData-Debug | Player: " + user.getPlayer().getName() +
                                                                  " sent a non-finite or illegal USE_ITEM rotation."));
                        return;
                    }

                    final ItemStack item = itemInHand(user, wrapper.getHand());
                    if (item == null || !AIM_DEPENDENT_ITEMS.contains(item.getType().name())) return;

                    final TargetingData.RotationSample movementRotation = user.getTargetingData()
                                                                               .nearestRotation(timestamp,
                                                                                               comparisonWindowNanos)
                                                                               .orElse(null);
                    if (movementRotation == null) return;

                    final double yawDifference = MathUtil.yawDistance(wrapper.getYaw(), movementRotation.yaw());
                    final double pitchDifference = Math.abs(wrapper.getPitch() - movementRotation.pitch());
                    final boolean mismatch = yawDifference > mismatchDegrees || pitchDifference > mismatchDegrees;
                    final var window = user.getData().object.targetingSilentRotationData.observe(mismatch,
                                                                                                  eligibleSamples,
                                                                                                  requiredMismatches,
                                                                                                  timestamp,
                                                                                                  FLAG_COOLDOWN_NANOS);
                    if (window.shouldFlag()) {
                        getManagement().flag(Flag.of(user)
                                                 .setAddedVl(15)
                                                 .setDebug(() -> "TargetingData-Debug | Player: " + user.getPlayer().getName() +
                                                                  " repeatedly sent USE_ITEM rotations differing from movement rotation (mismatches: " +
                                                                  window.mismatches() + "/" + window.size() + ")."));
                    }
        }).build();
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC121_5.getSupVersionsFrom())
                           .addPacketListeners(packetAdapter)
                           .build();
    }

    private static ItemStack itemInHand(final User user, final InteractionHand hand)
    {
        if (hand == null) return null;
        final var hands = InventoryUtil.INSTANCE.getHandContents(user.getPlayer());
        final int handIndex = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        return hands.size() > handIndex ? hands.get(handIndex) : null;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 2).build();
    }
}
