package de.photon.aacadditionpro.modules.additions;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.protocol.ProtocolVersion;
import de.photon.aacadditionpro.util.config.Configs;
import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import lombok.val;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VersionControl extends Module
{
    public VersionControl()
    {
        super("VersionControl");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addPluginDependencies("ViaVersion")
                           .build();
    }

    @Override
    public void enable()
    {
        val viaVersionKickMessage = Preconditions.checkNotNull(AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".message"), "VersionControl message is null. Please fix your config.");
        val configValues = Arrays.stream(ProtocolVersion.values())
                                 .map(ProtocolVersion::getName)
                                 .map(key -> ImmutablePair.of(key, AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".allowedVersions." + key)))
                                 .collect(Collectors.toSet());

        val allowedVersions = configValues.stream().filter(ImmutablePair::getSecond).map(ImmutablePair::getFirst).collect(Collectors.joining(", "));
        val blockedProtocolNumbers = configValues.stream()
                                                 .filter(Predicate.not(ImmutablePair::getSecond))
                                                 // Get the ProtocolVersion of a key.
                                                 .map(pair -> Preconditions.checkNotNull(ProtocolVersion.getByName(pair.getFirst()), "A Unknown protocol version \"" + pair.getFirst() + "\" in version control. Please fix your config."))
                                                 // Get all the actual version numbers.
                                                 .flatMap(protocolVersion -> protocolVersion.getVersionNumbers().stream())
                                                 // Make the display more visually appealing.
                                                 .sorted()
                                                 .collect(Collectors.toList());

        // Set the kick message.
        // Construct the message and replace special placeholder.
        Configs.VIAVERSION.getConfigurationRepresentation().requestValueChange("block-disconnect-msg", viaVersionKickMessage.replace("{supportedVersions}", allowedVersions));

        // Block the affected protocol numbers.
        Configs.VIAVERSION.getConfigurationRepresentation().requestValueChange("block-protocols", blockedProtocolNumbers);
    }
}
