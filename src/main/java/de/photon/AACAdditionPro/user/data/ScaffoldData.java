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
    // Default buffer size is 6, being well tested.
    private static int BUFFER_SIZE = 6;
    // Use static here as Datas are often created.
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
     * Used to calculate the average and expected time span between the {@link ScaffoldBlockPlace}s in the buffer.
     * Also clears the buffer.
     *
     * @return an array with the following contents:<br>
     * [0] = Expected time <br>
     * [1] = Real time <br>
     */
    public double[] calculateTimes()
    {
        final double[] result = new double[2];

        // -1 because there is one pop to fill the "last" variable in the beginning.
        final int divisor = this.scaffoldBlockPlaces.size() - 1;

        final boolean moonwalk = this.scaffoldBlockPlaces.stream().filter((blockPlace) -> !blockPlace.isSneaked()).count() >= BUFFER_SIZE / 2;

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

                    double delay;
                    if (last.getBlockFace() == current.getBlockFace() || last.getBlockFace() == current.getBlockFace().getOppositeFace())
                    {
                        delay = DELAY_NORMAL;

                        if (!moonwalk && last.isSneaked() && current.isSneaked())
                        {
                            delay += SNEAKING_ADDITION + (SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2 * current.getYaw())));
                        }
                    }
                    else
                    {
                        delay = DELAY_DIAGONAL;
                    }

                    result[0] += delay;

                    // last - current to calculate the delta as the more recent time is always in last.
                    result[1] += (last.getTime() - current.getTime()) * speed_modifier;
                });

        result[0] /= divisor;
        result[1] /= divisor;
        return result;
    }
}
