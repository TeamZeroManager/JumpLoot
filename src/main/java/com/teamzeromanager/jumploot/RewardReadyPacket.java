package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends the server-selected reward to the client. */
public final class RewardReadyPacket {
    private final LootRarity rarity;
    private final ItemStack item;

    public RewardReadyPacket(LootRarity rarity, ItemStack item) {
        this.rarity = rarity;
        this.item = item.copy();
    }

    public static void encode(RewardReadyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.rarity);
        buffer.writeNbt(packet.item.save(new CompoundTag()));
    }

    public static RewardReadyPacket decode(FriendlyByteBuf buffer) {
        LootRarity rarity = buffer.readEnum(LootRarity.class);
        CompoundTag tag = buffer.readNbt();
        return new RewardReadyPacket(rarity, tag == null ? ItemStack.EMPTY : ItemStack.of(tag));
    }

    public static void handle(RewardReadyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(
                        new LootRouletteScreen(java.util.Collections.singletonList(
                                new LootStorage.StoredReward(packet.item, packet.rarity))))));
        context.setPacketHandled(true);
    }
}
