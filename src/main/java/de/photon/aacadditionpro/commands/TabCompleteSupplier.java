package de.photon.aacadditionpro.commands;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Creates a TabCompleter which computes the tab possibilities of a command.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TabCompleteSupplier
{
    private final List<Supplier<List<String>>> tabPossibilities;

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Calculates the currently possible tabs while accounting for the partialArgument.
     *
     * @param partialArgument the argument the player has started typing.
     */
    public List<String> getTabPossibilities(final String partialArgument)
    {
        if (partialArgument == null) return getTabPossibilities();
        final String lowerCaseArgument = partialArgument.toLowerCase(Locale.ENGLISH);

        return this.tabPossibilities.stream()
                                    .flatMap(listSupplier -> listSupplier.get().stream())
                                    .filter(potentialTab -> potentialTab.toLowerCase(Locale.ENGLISH).startsWith(lowerCaseArgument))
                                    .collect(Collectors.toList());
    }

    /**
     * Calculates the currently possible tabs.
     */
    public List<String> getTabPossibilities()
    {
        return this.tabPossibilities.stream()
                                    .flatMap(listSupplier -> listSupplier.get().stream())
                                    .collect(Collectors.toList());
    }

    /**
     * Factory for building a {@link TabCompleteSupplier}.
     */
    public static class Builder
    {
        final ImmutableList.Builder<Supplier<List<String>>> supplierBuilder = ImmutableList.builder();
        final List<String> constants = new ArrayList<>();

        /**
         * This will make all players show up in the tab-complete suggestions.
         */
        public Builder allPlayers()
        {
            supplierBuilder.add(() -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return this;
        }

        /**
         * This will make certain constants show up in the tab-complete suggestions.
         */
        public Builder constants(String... constants)
        {
            this.constants.addAll(Arrays.asList(constants));
            return this;
        }

        /**
         * This will make certain constants show up in the tab-complete suggestions.
         */
        public Builder constants(Collection<String> constants)
        {
            this.constants.addAll(constants);
            return this;
        }

        public Builder childCommands(CommandAttributes attributes)
        {
            return constants(attributes.getChildCommands().keySet());
        }

        public Builder commandHelp()
        {
            return constants("help");
        }

        /**
         * Custom supplier that supplies a {@link List} of {@link String}s which will show up in the tab-complete
         * suggestions.
         */
        public Builder supplier(Supplier<List<String>> supplier)
        {
            supplierBuilder.add(supplier);
            return this;
        }

        /**
         * Create a {@link TabCompleteSupplier} from the chosen options.
         */
        public TabCompleteSupplier build()
        {
            if (!constants.isEmpty()) {
                final List<String> immutableConstants = ImmutableList.copyOf(this.constants);
                this.supplierBuilder.add(() -> immutableConstants);
            }
            return new TabCompleteSupplier(this.supplierBuilder.build());
        }
    }
}
