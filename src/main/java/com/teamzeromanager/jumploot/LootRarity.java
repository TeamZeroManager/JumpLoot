package com.teamzeromanager.jumploot;

import net.minecraft.util.RandomSource;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Rarity tiers and weighted selection for the two-stage reward roulette.
 *
 * The weights are independent of the number of items in each tier.
 * Empty tiers are omitted from the available collection before selection.
 */
public enum LootRarity {
    COMMON(60, 0xA0A0A0),
    UNCOMMON(25, 0x55FF55),
    RARE(10, 0x5555FF),
    EPIC(4, 0xAA00AA),
    LEGENDARY(1, 0xFFAA00);

    private final int defaultWeight;
    private final int color;

    LootRarity(int defaultWeight, int color) {
        this.defaultWeight = defaultWeight;
        this.color = color;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }

    public int getColor() {
        return color;
    }

    /**
     * Selects one available rarity using the configured weights.
     * A tier with weight zero or less is treated as disabled.
     */
    public static LootRarity choose(RandomSource random, Collection<LootRarity> available,
                                    Map<LootRarity, Integer> configuredWeights) {
        EnumMap<LootRarity, Integer> weights = new EnumMap<>(LootRarity.class);
        int totalWeight = 0;

        for (LootRarity rarity : available) {
            int weight = configuredWeights.getOrDefault(rarity, rarity.defaultWeight);
            if (weight > 0) {
                weights.put(rarity, weight);
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0) {
            throw new IllegalStateException("No reward rarity with a positive weight is available.");
        }

        int roll = random.nextInt(totalWeight);
        for (Map.Entry<LootRarity, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }

        throw new IllegalStateException("Rarity selection failed despite a positive total weight.");
    }
}
