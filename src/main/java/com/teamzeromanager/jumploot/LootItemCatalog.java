package com.teamzeromanager.jumploot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SmithingTemplateItem;

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
            if (item instanceof RecordItem || item instanceof BannerItem || item instanceof SpawnEggItem) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                continue;
            }

            LootRarity rarity = RARITY_OVERRIDES.get(id.toString());
            if (rarity == null && item instanceof SmithingTemplateItem) {
                rarity = LootRarity.RARE;
            }
            if (rarity == null && isEmeraldRelated(id)) {
                rarity = LootRarity.UNCOMMON;
            }
            if (rarity == null && isGoldMaterial(id)) {
                rarity = LootRarity.UNCOMMON;
            }
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

    private static boolean isEmeraldRelated(ResourceLocation id) {
        return id.getPath().contains("emerald");
    }

    private static boolean isGoldMaterial(ResourceLocation id) {
        String path = id.getPath();
        return path.equals("gold_ingot") || path.equals("gold_nugget")
                || path.equals("gold_block") || path.equals("raw_gold")
                || path.equals("raw_gold_block") || path.equals("gold_ore")
                || path.equals("deepslate_gold_ore") || path.equals("nether_gold_ore")
                || path.equals("gilded_blackstone")
                || path.endsWith("_gold_ore") || path.endsWith("_gold_ingot")
                || path.endsWith("_gold_nugget") || path.endsWith("_raw_gold");
    }

    private static Map<String, LootRarity> createOverrides() {
        Map<String, LootRarity> overrides = new HashMap<>();
        overrides.put("minecraft:nether_star", LootRarity.LEGENDARY);
        overrides.put("minecraft:elytra", LootRarity.LEGENDARY);
        overrides.put("minecraft:enchanted_golden_apple", LootRarity.LEGENDARY);
        overrides.put("minecraft:dragon_egg", LootRarity.LEGENDARY);
        overrides.put("minecraft:dragon_head", LootRarity.LEGENDARY);
        overrides.put("minecraft:ancient_debris", LootRarity.EPIC);
        overrides.put("minecraft:netherite_upgrade_smithing_template", LootRarity.LEGENDARY);
        overrides.put("minecraft:dragon_breath", LootRarity.EPIC);
        overrides.put("minecraft:beacon", LootRarity.EPIC);
        overrides.put("minecraft:ender_eye", LootRarity.EPIC);
        overrides.put("minecraft:wither_skeleton_skull", LootRarity.RARE);
        overrides.put("minecraft:blaze_rod", LootRarity.RARE);
        overrides.put("minecraft:blaze_powder", LootRarity.RARE);
        overrides.put("minecraft:chainmail_helmet", LootRarity.UNCOMMON);
        overrides.put("minecraft:chainmail_chestplate", LootRarity.UNCOMMON);
        overrides.put("minecraft:chainmail_leggings", LootRarity.UNCOMMON);
        overrides.put("minecraft:chainmail_boots", LootRarity.UNCOMMON);
        overrides.put("minecraft:obsidian", LootRarity.RARE);
        overrides.put("minecraft:crying_obsidian", LootRarity.RARE);
        overrides.put("minecraft:diamond_ore", LootRarity.RARE);
        overrides.put("minecraft:deepslate_diamond_ore", LootRarity.RARE);
        overrides.put("minecraft:diamond_block", LootRarity.RARE);
        overrides.put("minecraft:diamond", LootRarity.RARE);
        overrides.put("minecraft:enchanting_table", LootRarity.UNCOMMON);
        overrides.put("minecraft:ender_pearl", LootRarity.RARE);
        overrides.put("minecraft:gold_ingot", LootRarity.UNCOMMON);
        overrides.put("minecraft:gold_nugget", LootRarity.UNCOMMON);
        overrides.put("minecraft:gold_block", LootRarity.UNCOMMON);
        overrides.put("minecraft:raw_gold", LootRarity.UNCOMMON);
        overrides.put("minecraft:raw_gold_block", LootRarity.UNCOMMON);
        overrides.put("minecraft:gold_ore", LootRarity.UNCOMMON);
        overrides.put("minecraft:deepslate_gold_ore", LootRarity.UNCOMMON);
        overrides.put("minecraft:nether_gold_ore", LootRarity.UNCOMMON);
        overrides.put("minecraft:gilded_blackstone", LootRarity.UNCOMMON);

        // Diamond equipment and weapons are rare (not the default rarity).
        overrides.put("minecraft:diamond_sword", LootRarity.RARE);
        overrides.put("minecraft:diamond_pickaxe", LootRarity.RARE);
        overrides.put("minecraft:diamond_axe", LootRarity.RARE);
        overrides.put("minecraft:diamond_shovel", LootRarity.RARE);
        overrides.put("minecraft:diamond_hoe", LootRarity.RARE);
        overrides.put("minecraft:diamond_helmet", LootRarity.RARE);
        overrides.put("minecraft:diamond_chestplate", LootRarity.RARE);
        overrides.put("minecraft:diamond_leggings", LootRarity.RARE);
        overrides.put("minecraft:diamond_boots", LootRarity.RARE);
        overrides.put("minecraft:totem_of_undying", LootRarity.EPIC);
        overrides.put("minecraft:trident", LootRarity.EPIC);
        // Netherite equipment is legendary because obtaining it requires a long progression.
        overrides.put("minecraft:netherite_sword", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_pickaxe", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_axe", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_shovel", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_hoe", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_helmet", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_chestplate", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_leggings", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_boots", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_scrap", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_ingot", LootRarity.LEGENDARY);
        overrides.put("minecraft:netherite_block", LootRarity.LEGENDARY);
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
