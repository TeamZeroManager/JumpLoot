package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LootStorageSyncPacket {
    private final List<LootStorage.StoredReward> rewards;

    public LootStorageSyncPacket(List<LootStorage.StoredReward> rewards) {
        this.rewards = new ArrayList<>();
        for (LootStorage.StoredReward reward : rewards) {
            this.rewards.add(new LootStorage.StoredReward(reward.stack(), reward.rarity(), reward.revealed()));
        }
    }

    public static void encode(LootStorageSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.rewards.size());
        for (LootStorage.StoredReward reward : packet.rewards) {
            buffer.writeItem(reward.stack());
            buffer.writeEnum(reward.rarity());
            buffer.writeBoolean(reward.revealed());
        }
    }

    public static LootStorageSyncPacket decode(FriendlyByteBuf buffer) {
        int size = Math.max(0, Math.min(10000, buffer.readVarInt()));
        List<LootStorage.StoredReward> rewards = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rewards.add(new LootStorage.StoredReward(buffer.readItem(), buffer.readEnum(LootRarity.class), buffer.readBoolean()));
        }
        return new LootStorageSyncPacket(rewards);
    }

    public static void handle(LootStorageSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new LootStorageScreen(packet.rewards))));
        context.setPacketHandled(true);
    }
}
