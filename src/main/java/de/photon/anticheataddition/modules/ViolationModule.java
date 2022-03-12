package de.photon.anticheataddition.modules;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.violationlevels.ViolationAggregateManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class ViolationModule extends Module
{
    @Getter(lazy = true) private final ViolationManagement management = createViolationManagement();

    protected ViolationModule(String configString)
    {
        super(configString);
    }

    public static ViolationModule parentOf(String configString, ViolationModule... children)
    {
        Preconditions.checkArgument(children != null && children.length != 0, "Tried to create parent ViolationModule without children.");

        return new ViolationModule(configString)
        {
            @Override
            protected ViolationManagement createViolationManagement()
            {
                return new ViolationAggregateManagement(this, ThresholdManagement.loadThresholds(this.getConfigString() + ".thresholds"), Arrays.stream(children).map(ViolationModule::getManagement).collect(Collectors.toUnmodifiableSet()));
            }
        };
    }

    protected abstract ViolationManagement createViolationManagement();
}
