package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Persistent, player-owned reward collection. */
public final class LootStorage {
    private static final String STORAGE_TAG = "jumploot.reward_storage";

    private LootStorage() {
    }

    public static void add(ServerPlayer player, ItemStack reward) {
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
                stored.set(i, existing.save(new CompoundTag()));
            }
        }

        // Split overflow into legal stack sizes; no fixed vanilla-only cap is assumed.
        while (!remaining.isEmpty()) {
            int amount = Math.min(remaining.getCount(), Math.max(1, remaining.getMaxStackSize()));
            ItemStack part = remaining.copy();
            part.setCount(amount);
            stored.add(part.save(new CompoundTag()));
            remaining.shrink(amount);
        }

        data.put(STORAGE_TAG, stored);
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
