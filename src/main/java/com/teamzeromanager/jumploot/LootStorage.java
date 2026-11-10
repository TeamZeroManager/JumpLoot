package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Persistent player-owned collection. Rarity and reveal state are stored per reward. */
public final class LootStorage {
    private static final String STORAGE_TAG = "jumploot.reward_storage";
    private static final String RARITY_TAG = "jumploot.rarity";
    private static final String REVEALED_TAG = "jumploot.revealed";

    public static final class StoredReward {
        private final ItemStack stack;
        private final LootRarity rarity;
        private final boolean revealed;

        public StoredReward(ItemStack stack, LootRarity rarity) { this(stack, rarity, true); }
        public StoredReward(ItemStack stack, LootRarity rarity, boolean revealed) {
            this.stack = stack.copy();
            this.rarity = rarity;
            this.revealed = revealed;
        }
        public ItemStack stack() { return stack.copy(); }
        public LootRarity rarity() { return rarity; }
        public boolean revealed() { return revealed; }
    }

    private LootStorage() {}

    public static void add(ServerPlayer player, ItemStack reward, LootRarity rarity) {
        if (reward == null || reward.isEmpty()) return;
        CompoundTag data = PlayerDataStore.get(player);
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        ItemStack remaining = reward.copy();

        for (int i = 0; i < stored.size() && !remaining.isEmpty(); i++) {
            CompoundTag entry = stored.getCompound(i);
            if (entry.getBoolean(REVEALED_TAG) || readRarity(entry) != rarity) continue;
            ItemStack existing = ItemStack.of(entry);
            if (!ItemStack.isSameItemSameTags(existing, remaining)) continue;
            int room = Math.max(0, existing.getMaxStackSize() - existing.getCount());
            int moved = Math.min(room, remaining.getCount());
            if (moved > 0) {
                existing.grow(moved);
                remaining.shrink(moved);
                CompoundTag merged = existing.save(new CompoundTag());
                merged.putString(RARITY_TAG, rarity.name());
                merged.putBoolean(REVEALED_TAG, false);
                stored.set(i, merged);
            }
        }
        while (!remaining.isEmpty()) {
            int amount = Math.min(remaining.getCount(), Math.max(1, remaining.getMaxStackSize()));
            ItemStack part = remaining.copy();
            part.setCount(amount);
            CompoundTag entry = part.save(new CompoundTag());
            entry.putString(RARITY_TAG, rarity.name());
            entry.putBoolean(REVEALED_TAG, false);
            stored.add(entry);
            remaining.shrink(amount);
        }
        data.put(STORAGE_TAG, stored);
    }

    private static LootRarity readRarity(CompoundTag entry) {
        if (entry.contains(RARITY_TAG)) {
            try { return LootRarity.valueOf(entry.getString(RARITY_TAG)); }
            catch (IllegalArgumentException ignored) {}
        }
        return LootRarity.COMMON;
    }

    public static List<StoredReward> getRewards(ServerPlayer player) {
        List<StoredReward> result = new ArrayList<>();
        ListTag stored = PlayerDataStore.get(player).getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) result.add(new StoredReward(stack, readRarity(entry), entry.getBoolean(REVEALED_TAG)));
        }
        return result;
    }

    public static List<StoredReward> revealPending(ServerPlayer player) {
        CompoundTag data = PlayerDataStore.get(player);
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        List<StoredReward> pending = new ArrayList<>();
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            ItemStack stack = ItemStack.of(entry);
            if (stack.isEmpty() || entry.getBoolean(REVEALED_TAG)) continue;
            pending.add(new StoredReward(stack, readRarity(entry), false));
            entry.putBoolean(REVEALED_TAG, true);
            stored.set(i, entry);
        }
        data.put(STORAGE_TAG, stored);
        return pending;
    }

    public static boolean claim(ServerPlayer player, int index) {
        CompoundTag data = PlayerDataStore.get(player);
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        if (index < 0 || index >= stored.size()) return false;
        CompoundTag entry = stored.getCompound(index);
        if (!entry.getBoolean(REVEALED_TAG)) return false;
        ItemStack storedStack = ItemStack.of(entry);
        if (storedStack.isEmpty()) {
            stored.remove(index);
            data.put(STORAGE_TAG, stored);
            return false;
        }
        ItemStack toInsert = storedStack.copy();
        player.getInventory().add(toInsert);
        if (toInsert.isEmpty()) stored.remove(index);
        else {
            CompoundTag remainder = toInsert.save(new CompoundTag());
            remainder.putString(RARITY_TAG, readRarity(entry).name());
            remainder.putBoolean(REVEALED_TAG, true);
            stored.set(index, remainder);
        }
        data.put(STORAGE_TAG, stored);
        return toInsert.isEmpty();
    }

    public static boolean discard(ServerPlayer player, int index) {
        CompoundTag data = PlayerDataStore.get(player);
        ListTag stored = data.getList(STORAGE_TAG, CompoundTag.TAG_COMPOUND);
        if (index < 0 || index >= stored.size()) return false;
        stored.remove(index);
        data.put(STORAGE_TAG, stored);
        return true;
    }
}
