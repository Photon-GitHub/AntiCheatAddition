package de.photon.anticheataddition.commands;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Creates a TabCompleter which computes the tab possibilities of a command.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabCompleteSupplier
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
        val lowerCaseArgument = partialArgument.toLowerCase(Locale.ENGLISH);

        return this.tabPossibilities.stream()
                                    .map(Supplier::get)
                                    .flatMap(List::stream)
                                    .filter(potentialTab -> potentialTab.toLowerCase(Locale.ENGLISH).startsWith(lowerCaseArgument))
                                    .sorted()
                                    .toList();
    }

    /**
     * Calculates the currently possible tabs.
     */
    public List<String> getTabPossibilities()
    {
        return this.tabPossibilities.stream()
                                    .map(Supplier::get)
                                    .flatMap(List::stream)
                                    .sorted()
                                    .toList();
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
            supplierBuilder.add(() -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
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

        /**
         * Custom supplier that supplies a {@link List} of {@link String}s which will show up in the tab-complete
         * suggestions.
         */
        public Builder supplier(Supplier<List<String>> supplier)
        {
            this.supplierBuilder.add(supplier);
            return this;
        }

        /**
         * Create a {@link TabCompleteSupplier} from the chosen options.
         */
        public TabCompleteSupplier build(CommandAttributes attributes)
        {
            // Add the child commands.
            constants(attributes.getChildCommands().keySet());
            // Automatically add the command help.
            if (attributes.hasCommandHelp()) constants("help");

            if (!this.constants.isEmpty()) {
                // Explicitly compute the constants here to make sure that we only compute this list once.
                val immutableConstants = List.copyOf(this.constants);
                this.supplierBuilder.add(() -> immutableConstants);
            }
            return new TabCompleteSupplier(this.supplierBuilder.build());
        }
    }
}
