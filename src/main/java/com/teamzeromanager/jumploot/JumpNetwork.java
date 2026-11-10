package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class JumpNetwork {
    static final ResourceLocation PROGRESS = id("progress");
    static final ResourceLocation REQUEST_STORAGE = id("request_storage");
    static final ResourceLocation STORAGE_SYNC = id("storage_sync");
    static final ResourceLocation STORAGE_ACTION = id("storage_action");
    static final ResourceLocation REWARD_BATCH = id("reward_batch");
    static final ResourceLocation REWARD_READY = id("reward_ready");

    private JumpNetwork() {}
    private static ResourceLocation id(String path) { return new ResourceLocation(JumpLoot.MOD_ID, path); }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_STORAGE, (server, player, handler, buf, response) ->
                server.execute(() -> sendStorage(player, LootStorage.getRewards(player))));
        ServerPlayNetworking.registerGlobalReceiver(STORAGE_ACTION, (server, player, handler, buf, response) -> {
            int action = buf.readVarInt();
            int index = buf.readVarInt();
            server.execute(() -> {
                if (action == 0) LootStorage.claim(player, index);
                else if (action == 1) LootStorage.discard(player, index);
                else if (action == 2) {
                    List<LootStorage.StoredReward> pending = LootStorage.revealPending(player);
                    if (!pending.isEmpty()) sendRewardBatch(player, pending);
                    return;
                }
                sendStorage(player, LootStorage.getRewards(player));
            });
        });
    }

    public static void sendProgress(ServerPlayer player, int current, int target) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(current);
        buf.writeVarInt(target);
        ServerPlayNetworking.send(player, PROGRESS, buf);
    }

    public static void sendRewardReady(ServerPlayer player, LootRarity rarity, ItemStack item) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeEnum(rarity);
        buf.writeItem(item);
        ServerPlayNetworking.send(player, REWARD_READY, buf);
    }

    public static void sendStorage(ServerPlayer player, List<LootStorage.StoredReward> rewards) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(rewards.size());
        for (LootStorage.StoredReward reward : rewards) {
            buf.writeItem(reward.stack());
            buf.writeEnum(reward.rarity());
            buf.writeBoolean(reward.revealed());
        }
        ServerPlayNetworking.send(player, STORAGE_SYNC, buf);
    }

    private static void sendRewardBatch(ServerPlayer player, List<LootStorage.StoredReward> rewards) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(rewards.size());
        for (LootStorage.StoredReward reward : rewards) {
            buf.writeEnum(reward.rarity());
            buf.writeItem(reward.stack());
        }
        ServerPlayNetworking.send(player, REWARD_BATCH, buf);
    }
}
