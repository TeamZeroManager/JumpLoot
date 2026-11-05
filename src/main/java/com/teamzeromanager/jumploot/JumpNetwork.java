package com.teamzeromanager.jumploot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class JumpNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JumpLoot.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;

    private JumpNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(JumpProgressPacket.class, packetId++)
                .encoder(JumpProgressPacket::encode)
                .decoder(JumpProgressPacket::decode)
                .consumerMainThread(JumpProgressPacket::handle)
                .add();
        CHANNEL.messageBuilder(RewardReadyPacket.class, packetId++)
                .encoder(RewardReadyPacket::encode)
                .decoder(RewardReadyPacket::decode)
                .consumerMainThread(RewardReadyPacket::handle)
                .add();
        CHANNEL.messageBuilder(RewardBatchReadyPacket.class, packetId++)
                .encoder(RewardBatchReadyPacket::encode)
                .decoder(RewardBatchReadyPacket::decode)
                .consumerMainThread(RewardBatchReadyPacket::handle)
                .add();
        CHANNEL.messageBuilder(LootStorageRequestPacket.class, packetId++)
                .encoder(LootStorageRequestPacket::encode)
                .decoder(LootStorageRequestPacket::decode)
                .consumerMainThread(LootStorageRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(LootStorageSyncPacket.class, packetId++)
                .encoder(LootStorageSyncPacket::encode)
                .decoder(LootStorageSyncPacket::decode)
                .consumerMainThread(LootStorageSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(LootStorageActionPacket.class, packetId++)
                .encoder(LootStorageActionPacket::encode)
                .decoder(LootStorageActionPacket::decode)
                .consumerMainThread(LootStorageActionPacket::handle)
                .add();
    }

    public static void sendProgress(ServerPlayer player, int current, int target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JumpProgressPacket(current, target));
    }

    public static void sendRewardReady(ServerPlayer player, LootRarity rarity, ItemStack item) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RewardReadyPacket(rarity, item));
    }

    public static void sendRewardBatch(ServerPlayer player, List<LootStorage.StoredReward> rewards) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RewardBatchReadyPacket(rewards));
    }

    public static void requestStorage() {
        CHANNEL.sendToServer(new LootStorageRequestPacket());
    }

    public static void sendStorage(ServerPlayer player, List<ItemStack> items) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LootStorageSyncPacket(items));
    }

    public static void claimStorage(int index) {
        CHANNEL.sendToServer(new LootStorageActionPacket(index, LootStorageActionPacket.Action.CLAIM));
    }

    public static void claimAllStorage() {
        CHANNEL.sendToServer(new LootStorageActionPacket(0, LootStorageActionPacket.Action.CLAIM_ALL));
    }

    public static void discardStorage(int index) {
        CHANNEL.sendToServer(new LootStorageActionPacket(index, LootStorageActionPacket.Action.DISCARD));
    }
}
