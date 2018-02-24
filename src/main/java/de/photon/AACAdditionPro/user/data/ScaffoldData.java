package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.ConditionalBuffer;
import de.photon.AACAdditionPro.util.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;

public class ScaffoldData extends TimeData
{
    // Use static here as Datas are often created.
    private static int BUFFER_SIZE = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.buffer_size");
    private static double DELAY_NORMAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.normal");
    private static double SNEAKING_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_addition");
    private static double SNEAKING_SLOW_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_slow_addition");
    private static double DELAY_DIAGONAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.diagonal");

    /**
     * This is used to determine fast rotations prior to scaffolding.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public int rotationFails = 0;

    /**
     * This is used to determine sprinting during scaffolding.
     * Some sprinting might be legit.
     */
    public int sprintingFails = 0;

    @Getter
    private final ConditionalBuffer<ScaffoldBlockPlace> scaffoldBlockPlaces = new ConditionalBuffer<ScaffoldBlockPlace>(BUFFER_SIZE)
    {
        @Override
        protected boolean verifyObject(ScaffoldBlockPlace object)
        {
            return this.isEmpty() ||
                   BlockUtils.isNext(this.peek().getBlock(), object.getBlock(), true);
        }
    };

    public ScaffoldData(User user)
    {
        super(user, 0);
    }

    /**
     * This calculates the maximum expected and real time of this {@link ConditionalBuffer}'s {@link ScaffoldBlockPlace}s.
     *
     * @return the real time in index 0 and the maximum expected time in index 1.
     */
    public double[] calculateTimes()
    {
        final double[] result = new double[2];
        // fraction[0] is the enumerator
        // fraction[1] is the divider
        final double[] fraction = new double[2];

        this.scaffoldBlockPlaces.clearLastTwoObjectsIteration(
                (last, current) ->
                {
                    final double speed_modifier;
                    if (current.getSpeedLevel() == null ||
                        current.getSpeedLevel() < 0)
                    {
                        speed_modifier = 1.0D;
                    }
                    else
                    {
                        //If the speedLevel is <= 0, the speed_modifier is 1
                        switch (current.getSpeedLevel())
                        {
                            case 0:
                                speed_modifier = 1.01D;
                                break;
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                speed_modifier = 1.5D;
                                break;
                            case 6:
                                speed_modifier = 1.55D;
                                break;
                            case 7:
                                speed_modifier = 2.3D;
                                break;
                            default:
                                // Everything above 8 should have a speed_modifier of 3
                                speed_modifier = 3.0D;
                                break;
                        }
                    }

                    result[1] += (last.getBlockFace() == current.getBlockFace() || last.getBlockFace() == current.getBlockFace().getOppositeFace()) ?
                                 // Apply sneaking modifier?
                                 (DELAY_NORMAL + (current.isSneaked() ?
                                                  // How fast can he move while sneaking?
                                                  (SNEAKING_ADDITION + (SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2 * current.getYaw())))) :
                                                  0)) :
                                 DELAY_DIAGONAL;

                    // last - current to calculate the delta as the more recent time is always in last.
                    fraction[0] += (last.getTime() - current.getTime()) * speed_modifier;
                    fraction[1]++;
                });
        result[0] = fraction[0] / fraction[1];
        result[1] /= fraction[1];
        return result;
    }
}
