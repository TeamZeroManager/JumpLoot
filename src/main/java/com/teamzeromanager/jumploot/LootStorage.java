package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Persistent, player-owned reward collection. */
public final class LootStorage {
    private static final String STORAGE_TAG = "jumploot.reward_storage";
    private static final String RARITY_TAG = "jumploot.rarity";

    public static final class StoredReward {
        private final ItemStack stack;
        private final LootRarity rarity;
        public StoredReward(ItemStack stack, LootRarity rarity) { this.stack = stack.copy(); this.rarity = rarity; }
        public ItemStack stack() { return stack.copy(); }
        public LootRarity rarity() { return rarity; }
    }

    private LootStorage() {
    }

    /**
     * Copies the reward collection when Forge creates a replacement player entity
     * (for example, after death). The stored list is deep-copied so the new player
     * owns an independent NBT value.
     */
    public static void copyFrom(Player original, Player replacement) {
        CompoundTag oldData = original.getPersistentData();
        CompoundTag newData = replacement.getPersistentData();

        if (oldData.contains(STORAGE_TAG, CompoundTag.TAG_LIST)) {
            newData.put(STORAGE_TAG, oldData.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND).copy());
        } else {
            newData.remove(STORAGE_TAG);
        }
    }

    public static void add(ServerPlayer player, ItemStack reward, LootRarity rarity) {
        if (reward == null || reward.isEmpty()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        ItemStack remaining = reward.copy();

        // Merge only stacks with identical item and NBT, respecting the live item stack limit.
        for (int i = 0; i < stored.size() && !remaining.isEmpty(); i++) {
            ItemStack existing = ItemStack.of(stored.getCompound(i));
            if (!ItemStack.isSameItemSameTags(existing, remaining)) {
                continue;
            }

            int limit = Math.max(1, existing.getMaxStackSize());
            int room = Math.max(0, limit - existing.getCount());
            int moved = Math.min(room, remaining.getCount());
            if (moved > 0) {
                existing.grow(moved);
                remaining.shrink(moved);
                CompoundTag merged = existing.save(new CompoundTag());
                merged.putString(RARITY_TAG, stored.getCompound(i).contains(RARITY_TAG) ? stored.getCompound(i).getString(RARITY_TAG) : rarity.name());
                stored.set(i, merged);
            }
        }

        // Split overflow into legal stack sizes; no fixed vanilla-only cap is assumed.
        while (!remaining.isEmpty()) {
            int amount = Math.min(remaining.getCount(), Math.max(1, remaining.getMaxStackSize()));
            ItemStack part = remaining.copy();
            part.setCount(amount);
            CompoundTag entry = part.save(new CompoundTag());
            entry.putString(RARITY_TAG, rarity.name());
            stored.add(entry);
            remaining.shrink(amount);
        }

        data.put(STORAGE_TAG, stored);
    }

    public static List<StoredReward> getRewards(ServerPlayer player) {
        List<StoredReward> result = new ArrayList<>();
        ListTag stored = player.getPersistentData().getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) {
                LootRarity rarity = LootRarity.COMMON;
                if (entry.contains(RARITY_TAG)) {
                    try { rarity = LootRarity.valueOf(entry.getString(RARITY_TAG)); }
                    catch (IllegalArgumentException ignored) { rarity = LootRarity.COMMON; }
                }
                result.add(new StoredReward(stack, rarity));
            }
        }
        return result;
    }

    public static List<ItemStack> getContents(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        ListTag stored = player.getPersistentData().getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            ItemStack stack = ItemStack.of(stored.getCompound(i));
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * Claims as much as the player's inventory accepts. Any remainder stays in storage.
     */
    public static boolean claim(ServerPlayer player, int index) {
        CompoundTag data = player.getPersistentData();
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        if (index < 0 || index >= stored.size()) {
            return false;
        }

        ItemStack storedStack = ItemStack.of(stored.getCompound(index));
        if (storedStack.isEmpty()) {
            stored.remove(index);
            data.put(STORAGE_TAG, stored);
            return false;
        }

        ItemStack toInsert = storedStack.copy();
        player.getInventory().add(toInsert);

        if (toInsert.isEmpty()) {
            stored.remove(index);
        } else {
            stored.set(index, toInsert.save(new CompoundTag()));
        }
        data.put(STORAGE_TAG, stored);
        return toInsert.isEmpty();
    }

    /** Claims every pending reward that fits; leftovers remain stored. */
    public static List<StoredReward> claimAllForReveal(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        List<StoredReward> delivered = new ArrayList<>();
        for (int i = stored.size() - 1; i >= 0; i--) {
            ItemStack stack = ItemStack.of(stored.getCompound(i));
            if (stack.isEmpty()) {
                stored.remove(i);
                continue;
            }
            CompoundTag entry = stored.getCompound(i);
            LootRarity rarity = LootRarity.COMMON;
            if (entry.contains(RARITY_TAG)) {
                try { rarity = LootRarity.valueOf(entry.getString(RARITY_TAG)); }
                catch (IllegalArgumentException ignored) { rarity = LootRarity.COMMON; }
            }
            ItemStack remaining = stack.copy();
            player.getInventory().add(remaining);
            int inserted = stack.getCount() - remaining.getCount();
            if (inserted > 0) {
                ItemStack shown = stack.copy();
                shown.setCount(inserted);
                delivered.add(0, new StoredReward(shown, rarity));
            }
            if (remaining.isEmpty()) stored.remove(i);
            else {
                CompoundTag updated = remaining.save(new CompoundTag());
                updated.putString(RARITY_TAG, rarity.name());
                stored.set(i, updated);
            }
        }
        data.put(STORAGE_TAG, stored);
        return delivered;
    }

    public static boolean discard(ServerPlayer player, int index) {
        CompoundTag data = player.getPersistentData();
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        if (index < 0 || index >= stored.size()) {
            return false;
        }
        stored.remove(index);
        data.put(STORAGE_TAG, stored);
        return true;
    }
}
