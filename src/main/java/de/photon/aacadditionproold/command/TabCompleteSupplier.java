package de.photon.aacadditionproold.command;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creates a TabCompleter which computes the tab possibilities of a command.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TabCompleteSupplier
{
    private final List<Supplier<List<String>>> tabPossibilities;

    /**
     * Calculates the currently possible tabs while accounting for the partialArgument.
     *
     * @param partialArgument the argument the player has started typing.
     */
    public List<String> getTabPossibilities(String partialArgument)
    {
        final List<String> tabs = new ArrayList<>(this.tabPossibilities.size());
        for (Supplier<List<String>> listSupplier : this.tabPossibilities) {
            for (String potentialTab : listSupplier.get()) {
                if (potentialTab.startsWith(partialArgument)) {
                    tabs.add(potentialTab);
                }
            }
        }
        return tabs;
    }

    /**
     * Calculates the currently possible tabs.
     */
    public List<String> getTabPossibilities()
    {
        final List<String> tabs = new ArrayList<>(this.tabPossibilities.size());
        for (Supplier<List<String>> listSupplier : this.tabPossibilities) {
            tabs.addAll(listSupplier.get());
        }
        return tabs;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Factory for building a {@link TabCompleteSupplier}.
     */
    public static class Builder
    {
        final ImmutableList.Builder<Supplier<List<String>>> supplierBuilder = ImmutableList.builder();

        /**
         * This will make all players show up in the tab-complete suggestions.
         */
        public Builder allPlayers()
        {
            supplierBuilder.add(() -> {
                final Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                final List<String> list = new ArrayList<>(players.size());
                for (Player player : players) {
                    list.add(player.getName());
                }
                return list;
            });
            return this;
        }

        /**
         * This will make certain constants show up in the tab-complete suggestions.
         */
        public Builder constants(String... constants)
        {
            final List<String> constantList = ImmutableList.copyOf(constants);
            if (!constantList.isEmpty()) {
                supplierBuilder.add(() -> constantList);
            }
            return this;
        }

        /**
         * This will make certain constants show up in the tab-complete suggestions.
         */
        public Builder constants(Collection<String> constants)
        {
            final List<String> constantList = ImmutableList.copyOf(constants);
            if (!constantList.isEmpty()) {
                supplierBuilder.add(() -> constantList);
            }
            return this;
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
            return new TabCompleteSupplier(this.supplierBuilder.build());
        }
    }
}
