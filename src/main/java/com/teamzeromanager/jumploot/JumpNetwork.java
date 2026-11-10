package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class JumpNetwork {
    private static final ResourceLocation PROGRESS = id("progress");
    private static final ResourceLocation REQUEST_STORAGE = id("request_storage");
    private static final ResourceLocation STORAGE_SYNC = id("storage_sync");
    private static final ResourceLocation STORAGE_ACTION = id("storage_action");
    private static final ResourceLocation REWARD_BATCH = id("reward_batch");
    private static final ResourceLocation REWARD_READY = id("reward_ready");

    private JumpNetwork() {}
    private static ResourceLocation id(String path) { return new ResourceLocation(JumpLoot.MOD_ID, path); }

    public static void register() {
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

        ClientPlayNetworking.registerGlobalReceiver(PROGRESS, (client, handler, buf, response) -> {
            int current = buf.readVarInt();
            int target = buf.readVarInt();
            client.execute(() -> ClientJumpHud.setProgress(current, target));
        });

        ClientPlayNetworking.registerGlobalReceiver(STORAGE_SYNC, (client, handler, buf, response) -> {
            int count = Math.max(0, Math.min(10000, buf.readVarInt()));
            List<LootStorage.StoredReward> rewards = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ItemStack stack = buf.readItem();
                LootRarity rarity = buf.readEnum(LootRarity.class);
                boolean revealed = buf.readBoolean();
                rewards.add(new LootStorage.StoredReward(stack, rarity, revealed));
            }
            client.execute(() -> Minecraft.getInstance().setScreen(new LootStorageScreen(rewards)));
        });

        ClientPlayNetworking.registerGlobalReceiver(REWARD_BATCH, (client, handler, buf, response) -> {
            int count = Math.max(0, Math.min(1000, buf.readVarInt()));
            List<LootStorage.StoredReward> rewards = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                LootRarity rarity = buf.readEnum(LootRarity.class);
                rewards.add(new LootStorage.StoredReward(buf.readItem(), rarity, true));
            }
            client.execute(() -> Minecraft.getInstance().setScreen(new LootRouletteScreen(rewards)));
        });

        ClientPlayNetworking.registerGlobalReceiver(REWARD_READY, (client, handler, buf, response) -> {
            LootRarity rarity = buf.readEnum(LootRarity.class);
            ItemStack stack = buf.readItem();
            client.execute(() -> Minecraft.getInstance().setScreen(new LootRouletteScreen(
                    java.util.Collections.singletonList(new LootStorage.StoredReward(stack, rarity, true)))));
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

    public static void requestStorage() { ClientPlayNetworking.send(REQUEST_STORAGE, PacketByteBufs.create()); }

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

    private static void sendAction(int action, int index) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarInt(index);
        ClientPlayNetworking.send(STORAGE_ACTION, buf);
    }

    public static void claimStorage(int index) { sendAction(0, index); }
    public static void discardStorage(int index) { sendAction(1, index); }
    public static void claimAllStorage() { sendAction(2, 0); }

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
