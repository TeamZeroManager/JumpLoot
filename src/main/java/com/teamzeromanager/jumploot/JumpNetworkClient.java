package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class JumpNetworkClient {
    private JumpNetworkClient() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.PROGRESS, (client, handler, buf, response) -> {
            int current = buf.readVarInt();
            int target = buf.readVarInt();
            client.execute(() -> ClientJumpHud.setProgress(current, target));
        });
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.STORAGE_SYNC, (client, handler, buf, response) -> {
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
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.REWARD_BATCH, (client, handler, buf, response) -> {
            int count = Math.max(0, Math.min(1000, buf.readVarInt()));
            List<LootStorage.StoredReward> rewards = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                LootRarity rarity = buf.readEnum(LootRarity.class);
                rewards.add(new LootStorage.StoredReward(buf.readItem(), rarity, true));
            }
            client.execute(() -> Minecraft.getInstance().setScreen(new LootRouletteScreen(rewards)));
        });
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.REWARD_READY, (client, handler, buf, response) -> {
            LootRarity rarity = buf.readEnum(LootRarity.class);
            ItemStack stack = buf.readItem();
            client.execute(() -> Minecraft.getInstance().setScreen(new LootRouletteScreen(
                    java.util.Collections.singletonList(new LootStorage.StoredReward(stack, rarity, true)))));
        });
    }

    public static void requestStorage() {
        ClientPlayNetworking.send(JumpNetwork.REQUEST_STORAGE, PacketByteBufs.create());
    }

    private static void sendAction(int action, int index) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarInt(index);
        ClientPlayNetworking.send(JumpNetwork.STORAGE_ACTION, buf);
    }

    public static void claimStorage(int index) { sendAction(0, index); }
    public static void discardStorage(int index) { sendAction(1, index); }
    public static void claimAllStorage() { sendAction(2, 0); }
}
