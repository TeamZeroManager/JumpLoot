package com.teamzeromanager.jumploot;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies the client that the player has earned a roulette reward.
 * The packet is reserved for opening the reward flow; the server currently
 * displays the progression message to avoid duplicate client notifications.
 */
public final class RewardReadyPacket {
    public static void encode(RewardReadyPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
        // No payload is required for this notification.
    }

    public static RewardReadyPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new RewardReadyPacket();
    }

    public static void handle(RewardReadyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // The reward UI will be opened here when the roulette is implemented.
        });
        context.setPacketHandled(true);
    }
}
