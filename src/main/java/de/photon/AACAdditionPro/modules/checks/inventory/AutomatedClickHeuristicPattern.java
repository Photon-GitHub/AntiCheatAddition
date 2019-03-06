package de.photon.AACAdditionPro.modules.checks.inventory;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

public class AutomatedClickHeuristicPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    private final SavedModelBundle firstModel = SavedModelBundle.load("/tmp/mymodel", "serve");

    @Override
    protected int process(User user, InventoryClickEvent event)
    {
        // TODO: Real input
        final Tensor<Integer> input = Tensors.create(0);

        final float output = firstModel.session().runner().feed("input", input).fetch("output").run().get(0).floatValue();
        return (int) (output * 1.5F);
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.AutomatedClickHeuristic";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}
