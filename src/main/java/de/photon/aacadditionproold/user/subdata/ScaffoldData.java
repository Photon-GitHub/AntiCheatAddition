package de.photon.aacadditionproold.user.subdata;

import com.google.common.base.Preconditions;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.checks.scaffold.Scaffold;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionproold.util.datastructures.batch.Batch;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;

public class ScaffoldData extends SubData
{
    // Default buffer size is 6, being well tested.
    public static final int BATCH_SIZE = 6;
    // Use static here as ScaffoldDatas are often created.
    public static final double DELAY_NORMAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.normal");
    public static final double SNEAKING_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_addition");
    public static final double SNEAKING_SLOW_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_slow_addition");
    public static final double DELAY_DIAGONAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.diagonal");

    // Add a dummy block to start with in order to make sure that the queue is never empty.
    @Getter
    private final Batch<ScaffoldBlockPlace> scaffoldBlockPlaces;
    /**
     * This is used to determine wrong angles while scaffolding.
     * One wrong angle might be legit, but more instances are a clear hint.
     */
    public long angleFails = 0;

    /**
     * This is used to determine fast rotations prior to scaffolding.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long rotationFails = 0;

    /**
     * This is used to suspicious locations.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long safewalkTypeOneFails = 0;

    /**
     * This is used to suspicious locations.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long safewalkTypeTwoFails = 0;

    /**
     * This is used to determine sprinting during scaffolding.
     * Some sprinting might be legit.
     */
    public long sprintingFails = 0;

    public ScaffoldData(User user)
    {
        super(user);
        // Assume that there is at least one world.
        scaffoldBlockPlaces = new Batch<>(user, BATCH_SIZE, new ScaffoldBlockPlace(Preconditions.checkNotNull(Bukkit.getWorlds().get(0), "Scaffold-Batch: No world could be found!").getBlockAt(0, 0, 0), BlockFace.NORTH, 10, 0, false));
        scaffoldBlockPlaces.registerProcessor(Scaffold.getInstance().getBatchProcessor());
    }
}
