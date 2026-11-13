package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class JumpNetwork {
    static final ResourceLocation PROGRESS = id("progress");
    static final ResourceLocation REQUEST_STORAGE = id("request_storage");
    static final ResourceLocation STORAGE_SYNC = id("storage_sync");
    static final ResourceLocation STORAGE_ACTION = id("storage_action");
    static final ResourceLocation REWARD_BATCH = id("reward_batch");
    static final ResourceLocation REWARD_READY = id("reward_ready");
    static final ResourceLocation SPECIAL_CHOICE_RESULT = id("special_choice_result");

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
                    List<LootStorage.StoredReward> normal = new ArrayList<>();
                    int specialCount = 0;
                    for (LootStorage.StoredReward reward : pending) {
                        if (reward.special()) specialCount++;
                        else normal.add(reward);
                    }
                    if (!normal.isEmpty() || specialCount > 0) sendRewardBatch(player, normal, specialCount);
                    else sendStorage(player, LootStorage.getRewards(player));
                    return;
                }
                sendStorage(player, LootStorage.getRewards(player));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(SPECIAL_CHOICE_RESULT, (server, player, handler, buf, response) -> {
            int rarityId = buf.readVarInt();
            ResourceLocation itemId = buf.readResourceLocation();
            server.execute(() -> {
                LootRarity[] rarities = LootRarity.values();
                boolean success = rarityId >= 0 && rarityId < rarities.length
                        && LootStorage.claimSpecial(player, rarities[rarityId], itemId);
                FriendlyByteBuf result = PacketByteBufs.create();
                result.writeBoolean(success);
                result.writeVarInt(LootStorage.countSpecial(player));
                ServerPlayNetworking.send(player, SPECIAL_CHOICE_RESULT, result);
                if (!success) player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "No se pudo validar la recompensa seleccionada. Inténtalo de nuevo."), false);
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
            buf.writeBoolean(reward.special());
        }
        ServerPlayNetworking.send(player, STORAGE_SYNC, buf);
    }

    private static void sendRewardBatch(ServerPlayer player, List<LootStorage.StoredReward> rewards,
                                        int specialCount) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(rewards.size());
        for (LootStorage.StoredReward reward : rewards) {
            buf.writeEnum(reward.rarity());
            buf.writeItem(reward.stack());
        }
        buf.writeVarInt(specialCount);
        ServerPlayNetworking.send(player, REWARD_BATCH, buf);
    }
}
