package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LootStorageSyncPacket {
    private final List<ItemStack> items;

    public LootStorageSyncPacket(List<ItemStack> items) {
        this.items = new ArrayList<>();
        for (ItemStack stack : items) this.items.add(stack.copy());
    }

    public static void encode(LootStorageSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.items.size());
        for (ItemStack stack : packet.items) buffer.writeItem(stack);
    }

    public static LootStorageSyncPacket decode(FriendlyByteBuf buffer) {
        int size = Math.max(0, Math.min(10000, buffer.readVarInt()));
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) items.add(buffer.readItem());
        return new LootStorageSyncPacket(items);
    }

    public static void handle(LootStorageSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new LootStorageScreen(packet.items))));
        context.setPacketHandled(true);
    }
}
