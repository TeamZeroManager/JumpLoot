package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent per-player data shared by jump progression and reward storage. */
public final class PlayerDataStore extends SavedData {
    private static final String SAVE_ID = "jumploot_players";
    private final Map<UUID, CompoundTag> players = new HashMap<>();

    public static CompoundTag get(ServerPlayer player) {
        PlayerDataStore store = player.getServer().overworld().getDataStorage()
                .computeIfAbsent(PlayerDataStore::load, PlayerDataStore::new, SAVE_ID);
        store.setDirty();
        return store.players.computeIfAbsent(player.getUUID(), ignored -> new CompoundTag());
    }

    private static PlayerDataStore load(CompoundTag root) {
        PlayerDataStore store = new PlayerDataStore();
        ListTag entries = root.getList("players", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.hasUUID("uuid") && entry.contains("data", CompoundTag.TAG_COMPOUND)) {
                store.players.put(entry.getUUID("uuid"), entry.getCompound("data").copy());
            }
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, CompoundTag> player : players.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", player.getKey());
            entry.put("data", player.getValue().copy());
            entries.add(entry);
        }
        root.put("players", entries);
        return root;
    }
}
