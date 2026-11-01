package com.teamzeromanager.jumploot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class LootStorageRequestPacket {
    public static void encode(LootStorageRequestPacket packet, FriendlyByteBuf buffer) { }
    public static LootStorageRequestPacket decode(FriendlyByteBuf buffer) { return new LootStorageRequestPacket(); }

    public static void handle(LootStorageRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                JumpNetwork.sendStorage(player, LootStorage.getContents(player));
            }
        });
        context.setPacketHandled(true);
    }
}
