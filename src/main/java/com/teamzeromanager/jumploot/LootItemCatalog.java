package com.teamzeromanager.jumploot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a snapshot of eligible registered items after the item registry is ready.
 * Modded items are included automatically. Music discs and banners are excluded
 * by item type, including modded subclasses, alongside dangerous/admin items.
 */
public final class LootItemCatalog {
    private static final Set<String> DEFAULT_BLACKLIST = Set.of(
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:debug_stick",
            "minecraft:knowledge_book",
            "minecraft:barrier",
            "minecraft:light"
    );

    /*
     * Explicit overrides take precedence over ItemStack#getRarity().
     * This is deliberately data-driven rather than relying on display names.
     */
    private static final Map<String, LootRarity> RARITY_OVERRIDES = createOverrides();

    private final EnumMap<LootRarity, List<ItemStack>> items = new EnumMap<>(LootRarity.class);

    private LootItemCatalog() {
        for (LootRarity rarity : LootRarity.values()) {
            items.put(rarity, new ArrayList<>());
        }
    }

    public static LootItemCatalog build() {
        return build(DEFAULT_BLACKLIST);
    }

    public static LootItemCatalog build(Set<String> additionalBlacklist) {
        LootItemCatalog catalog = new LootItemCatalog();
        Set<String> blacklist = new HashSet<>(DEFAULT_BLACKLIST);
        if (additionalBlacklist != null) {
            blacklist.addAll(additionalBlacklist);
        }

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || blacklist.contains(id.toString())) {
                continue;
            }

            // Type-based filtering also catches modded music discs and banners,
            // regardless of their registry namespace or translated display name.
            if (item instanceof RecordItem || item instanceof BannerItem) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }

            LootRarity rarity = RARITY_OVERRIDES.get(id.toString());
            if (rarity == null) {
                rarity = fromItemRarity(stack.getRarity());
            }
            catalog.items.get(rarity).add(stack);
        }

        for (List<ItemStack> tier : catalog.items.values()) {
            tier.sort((first, second) -> BuiltInRegistries.ITEM.getKey(first.getItem()).toString()
                    .compareTo(BuiltInRegistries.ITEM.getKey(second.getItem()).toString()));
            tier.replaceAll(ItemStack::copy);
        }

        return catalog;
    }

    private static LootRarity fromItemRarity(Rarity rarity) {
        // Avoid enum switch bytecode here: large modpacks may extend Minecraft's
        // rarity enum, and an unknown enum constant can trigger IncompatibleClassChangeError.
        if (rarity == Rarity.UNCOMMON) {
            return LootRarity.UNCOMMON;
        }
        if (rarity == Rarity.RARE) {
            return LootRarity.RARE;
        }
        if (rarity == Rarity.EPIC) {
            return LootRarity.EPIC;
        }
        return LootRarity.COMMON;
    }

    private static Map<String, LootRarity> createOverrides() {
        Map<String, LootRarity> overrides = new HashMap<>();
        overrides.put("minecraft:nether_star", LootRarity.LEGENDARY);
        overrides.put("minecraft:elytra", LootRarity.LEGENDARY);
        overrides.put("minecraft:enchanted_golden_apple", LootRarity.LEGENDARY);
        overrides.put("minecraft:dragon_egg", LootRarity.LEGENDARY);
        overrides.put("minecraft:totem_of_undying", LootRarity.EPIC);
        overrides.put("minecraft:trident", LootRarity.EPIC);
        overrides.put("minecraft:netherite_chestplate", LootRarity.EPIC);
        overrides.put("minecraft:netherite_sword", LootRarity.EPIC);
        overrides.put("minecraft:netherite_pickaxe", LootRarity.EPIC);
        overrides.put("minecraft:netherite_axe", LootRarity.EPIC);
        overrides.put("minecraft:netherite_helmet", LootRarity.EPIC);
        overrides.put("minecraft:netherite_leggings", LootRarity.EPIC);
        overrides.put("minecraft:netherite_boots", LootRarity.EPIC);
        return Collections.unmodifiableMap(overrides);
    }

    public List<ItemStack> getItems(LootRarity rarity) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items.get(rarity)) {
            result.add(stack.copy());
        }
        return Collections.unmodifiableList(result);
    }

    public Set<LootRarity> getAvailableRarities() {
        Set<LootRarity> available = new HashSet<>();
        for (Map.Entry<LootRarity, List<ItemStack>> entry : items.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                available.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(available);
    }

    public int getItemCount(LootRarity rarity) {
        return items.get(rarity).size();
    }

    public int getTotalItemCount() {
        return items.values().stream().mapToInt(List::size).sum();
    }
}
