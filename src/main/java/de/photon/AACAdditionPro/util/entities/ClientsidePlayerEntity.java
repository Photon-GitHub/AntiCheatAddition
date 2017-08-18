package de.photon.AACAdditionPro.util.entities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.util.entities.displayinformation.DisplayInformation;
import de.photon.AACAdditionPro.util.entities.equipment.Equipment;
import de.photon.AACAdditionPro.util.entities.equipment.category.WeaponsEquipmentCategory;
import de.photon.AACAdditionPro.util.entities.movement.BasicMovement;
import de.photon.AACAdditionPro.util.entities.movement.Movement;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    private static final boolean shouldAssignTeam;
    private static final boolean shouldSwing;
    private static final boolean shouldSwap;

    static {
        // Init additional behaviour configs
        shouldAssignTeam = AACAdditionPro.getInstance().getConfig().getBoolean(AdditionHackType.KILLAURA_ENTITY.getConfigString() + ".behaviour.team.enabled");
        shouldSwing = AACAdditionPro.getInstance().getConfig().getBoolean(AdditionHackType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swing.enabled");
        shouldSwap = AACAdditionPro.getInstance().getConfig().getBoolean(AdditionHackType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swap.enabled");
    }

    @Getter
    private final WrappedGameProfile gameProfile;
    @Getter
    @Setter
    private int ping;

    @Getter
    @Setter
    private Team currentTeam;

    // Main ticker for the entity
    private byte lastSwing = 0;
    private byte lastSwap = 0;

    // Movement state machine
    private Map<String, Movement> movementStates = new HashMap<>(); // HashMap is enough since you write to it once and never rehash it
    private Movement currentMovementCalculator;
    private short lastJump = 0;

    private Equipment equipment;

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile, final double entityOffset, final double offsetRandomizationRange, double minXZDifference)
    {
        super(observedPlayer);

        // This needs to match a NMS EntityPlayer hitbox
        this.size.setX(2 * Hitbox.PLAYER.getOffset());
        this.size.setY(Hitbox.PLAYER.getHeight());
        this.size.setZ(2 * Hitbox.PLAYER.getOffset());

        // Get skin data and name
        this.gameProfile = gameProfile;

        // EquipmentData
        this.equipment = new Equipment(this);

        // Init movement states
        this.movementStates.put("basic", new BasicMovement(observedPlayer, entityOffset, offsetRandomizationRange, minXZDifference));

        // Set default movement state
        this.currentMovementCalculator = this.movementStates.get("basic");

        recursiveUpdatePing();
    }

    @Override
    protected void tick()
    {
        super.tick();

        // Teams + Scoreboard
        if (shouldAssignTeam) {
            DisplayInformation.applyTeams(this);
        }

        // Try to look to the target
        Location target = this.observedPlayer.getLocation();
        double diffX = target.getX() - this.location.getX();
        double diffY = target.getY() + this.observedPlayer.getEyeHeight() * 0.9D - (this.location.getY() + this.observedPlayer.getEyeHeight());
        double diffZ = target.getZ() - this.location.getZ();
        double dist = Math.hypot(diffX, diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.asin(diffY / dist));

        pitch += ThreadLocalRandom.current().nextInt(5);

        pitch = reduceAngle(pitch, 90);

        float newHeadYaw;
        do {
            newHeadYaw = (float) (yaw + 10 + ThreadLocalRandom.current().nextDouble(20));
        } while (getFixRotation(headYaw) == getFixRotation(newHeadYaw));

        this.headYaw = reduceAngle(newHeadYaw, 180);

        // Get the next position and move
        Location location = this.currentMovementCalculator.calculate(this.location.clone());
        if (location == null) {
            this.currentMovementCalculator = this.movementStates.get("basic");
            location = this.currentMovementCalculator.calculate(this.location.clone());
        }

        location.setYaw(yaw);
        location.setPitch(pitch);
        this.move(location);

        // Maybe we should switch movement states?
        if (lastJump++ > 30 + ThreadLocalRandom.current().nextInt(80)) {
            lastJump = 0;
            jump();
        }

        // Swing items if enabled
        if (shouldSwing) {
            int should = 15 + ThreadLocalRandom.current().nextInt(35);
            if (lastSwing++ > should) {
                lastSwing = 0;

                if (isSwingable(equipment.getMainHand().getType())) {
                    swing();
                }
            }
        }

        // Swap items if needed
        if (shouldSwap) {
            int should = 40 + ThreadLocalRandom.current().nextInt(65);
            if (lastSwap++ > should) {
                lastSwap = 0;
                equipment.equipInHand();
                equipment.equipPlayerEntity();
            }
        }
    }

    // -------------------------------------------------------------- Yaw/Pitch ------------------------------------------------------------- //

    /**
     * Reduces the angle to make it fit the spectrum of -minMax til +minMax in steps of minMax
     *
     * @param input  the initial angle
     * @param minMax the boundary in the positive and negative spectrum. The parameter itself must be > 0.
     */
    private float reduceAngle(float input, float minMax)
    {
        while (Math.abs(input) > minMax) {
            input -= Math.signum(input) * minMax;
        }
        return input;
    }

    private byte getFixRotation(float yawpitch)
    {
        return (byte) ((int) (yawpitch * 256.0F / 360.0F));
    }

    private boolean isSwingable(Material material)
    {
        WeaponsEquipmentCategory weaponsEquipmentCategory = new WeaponsEquipmentCategory();
        return weaponsEquipmentCategory.getMaterials().contains(material);
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

    public String getName()
    {
        return this.gameProfile.getName();
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * This changes the Ping of the {@link ClientsidePlayerEntity}.
     * The recursive call is needed to randomize the ping-update
     */
    private void recursiveUpdatePing()
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> {
            DisplayInformation.updatePing(this);
            recursiveUpdatePing();
        }, 10 + ThreadLocalRandom.current().nextInt(35));
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    @Override
    public void spawn(Location location)
    {
        super.spawn(location);
        this.lastLocation = location.clone();
        this.move(location);
        // Add the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, (float) 20);
        dataWatcher.setObject(10, (byte) 127); //TODO player's probably do have more things set here

        // Spawn the entity
        final WrapperPlayServerNamedEntitySpawn spawnEntityWrapper = new WrapperPlayServerNamedEntitySpawn();

        spawnEntityWrapper.setEntityID(this.entityID);
        spawnEntityWrapper.setMetadata(dataWatcher);
        spawnEntityWrapper.setPosition(location.toVector());
        spawnEntityWrapper.setPlayerUUID(this.gameProfile.getUUID());
        spawnEntityWrapper.setYaw(ThreadLocalRandom.current().nextInt(15));
        spawnEntityWrapper.setPitch(ThreadLocalRandom.current().nextInt(15));

        spawnEntityWrapper.sendPacket(observedPlayer);

        // Debug
        // System.out.println("Sent player spawn of bot " + this.entityID + " for " + observedPlayer.getName() + " @ " + location);

        // Set the team (most common on respawn)
        if (shouldAssignTeam) {
            DisplayInformation.applyTeams(this);
        }

        // Entity equipment + armor
        this.equipment.equipArmor();
        this.equipment.equipInHand();
        this.equipment.equipPlayerEntity();
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    @Override
    public void despawn()
    {
        super.despawn();

        if (isSpawned()) {
            removeFromTab();
        }
    }

    private void removeFromTab()
    {
        // Remove the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);
    }
}
