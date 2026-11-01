package com.teamzeromanager.jumploot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class LootStorageActionPacket {
    public enum Action { CLAIM, DISCARD }
    private final int index;
    private final Action action;

    public LootStorageActionPacket(int index, Action action) {
        this.index = index;
        this.action = action;
    }

    public static void encode(LootStorageActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.index);
        buffer.writeEnum(packet.action);
    }

    public static LootStorageActionPacket decode(FriendlyByteBuf buffer) {
        return new LootStorageActionPacket(buffer.readVarInt(), buffer.readEnum(Action.class));
    }

    public static void handle(LootStorageActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (packet.action == Action.CLAIM) LootStorage.claim(player, packet.index);
            else LootStorage.discard(player, packet.index);
            JumpNetwork.sendStorage(player, LootStorage.getContents(player));
        });
        context.setPacketHandled(true);
    }
}
