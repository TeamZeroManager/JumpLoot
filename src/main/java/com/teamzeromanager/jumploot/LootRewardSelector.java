package com.teamzeromanager.jumploot;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Performs the two independent roulette decisions:
 * weighted rarity first, then a uniform item choice inside that rarity.
 */
public final class LootRewardSelector {
    private LootRewardSelector() {
    }

    public static Result select(RandomSource random, LootItemCatalog catalog) {
        return select(random, catalog, defaultWeights());
    }

    public static Result select(RandomSource random, LootItemCatalog catalog,
                                Map<LootRarity, Integer> configuredWeights) {
        LootRarity rarity = LootRarity.choose(random, catalog.getAvailableRarities(), configuredWeights);
        var eligibleItems = catalog.getItems(rarity);
        if (eligibleItems.isEmpty()) {
            throw new IllegalStateException("Selected rarity has no eligible items: " + rarity);
        }

        ItemStack selected = eligibleItems.get(random.nextInt(eligibleItems.size()));
        return new Result(rarity, selected);
    }

    /**
     * Forces the rarity roulette to choose only between Epic and Legendary.
     * Existing rarity weights are preserved (4:1 by default).
     */
    public static Result selectGuaranteedHighRarity(RandomSource random, LootItemCatalog catalog) {
        var highRarities = catalog.getAvailableRarities().stream()
                .filter(rarity -> rarity == LootRarity.EPIC || rarity == LootRarity.LEGENDARY)
                .toList();
        if (highRarities.isEmpty()) {
            throw new IllegalStateException("No eligible Epic or Legendary rewards are available.");
        }

        EnumMap<LootRarity, Integer> highWeights = new EnumMap<>(LootRarity.class);
        highWeights.put(LootRarity.EPIC, LootRarity.EPIC.getDefaultWeight());
        highWeights.put(LootRarity.LEGENDARY, LootRarity.LEGENDARY.getDefaultWeight());
        LootRarity rarity = LootRarity.choose(random, highRarities, highWeights);
        var eligibleItems = catalog.getItems(rarity);
        if (eligibleItems.isEmpty()) {
            throw new IllegalStateException("Guaranteed rarity has no eligible items: " + rarity);
        }

        ItemStack selected = eligibleItems.get(random.nextInt(eligibleItems.size()));
        return new Result(rarity, selected);
    }

    public static Map<LootRarity, Integer> defaultWeights() {
        EnumMap<LootRarity, Integer> weights = new EnumMap<>(LootRarity.class);
        for (LootRarity rarity : LootRarity.values()) {
            weights.put(rarity, rarity.getDefaultWeight());
        }
        return weights;
    }

    public record Result(LootRarity rarity, ItemStack item) {
        public Result {
            item = item.copy();
        }

        @Override
        public ItemStack item() {
            return item.copy();
        }
    }
}
