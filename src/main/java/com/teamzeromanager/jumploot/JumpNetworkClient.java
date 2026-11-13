package com.teamzeromanager.jumploot;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
                boolean special = buf.readBoolean();
                rewards.add(new LootStorage.StoredReward(stack, rarity, revealed, special));
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
            int specialCount = Math.max(0, Math.min(1000, buf.readVarInt()));
            client.execute(() -> {
                if (!rewards.isEmpty()) Minecraft.getInstance().setScreen(new LootRouletteScreen(rewards, specialCount));
                else Minecraft.getInstance().setScreen(new LootCatalogScreen(null, specialCount));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.REWARD_READY, (client, handler, buf, response) -> {
            LootRarity rarity = buf.readEnum(LootRarity.class);
            ItemStack stack = buf.readItem();
            client.execute(() -> Minecraft.getInstance().setScreen(new LootRouletteScreen(
                    java.util.Collections.singletonList(new LootStorage.StoredReward(stack, rarity, true)))));
        });
        ClientPlayNetworking.registerGlobalReceiver(JumpNetwork.SPECIAL_CHOICE_RESULT, (client, handler, buf, response) -> {
            boolean success = buf.readBoolean();
            int remaining = Math.max(0, buf.readVarInt());
            client.execute(() -> {
                if (Minecraft.getInstance().screen instanceof LootCatalogScreen screen) {
                    screen.onSpecialChoiceResult(success, remaining);
                }
            });
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

    public static void claimSpecial(LootRarity rarity, ResourceLocation itemId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(rarity.ordinal());
        buf.writeResourceLocation(itemId);
        ClientPlayNetworking.send(JumpNetwork.SPECIAL_CHOICE_RESULT, buf);
    }
}
