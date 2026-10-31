package com.teamzeromanager.jumploot;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Notifies the client that the player has earned a roulette reward.
 */
public final class RewardReadyPacket {
    public static void encode(RewardReadyPacket packet, FriendlyByteBuf buffer) {
        // No payload is required for this notification.
    }

    public static RewardReadyPacket decode(FriendlyByteBuf buffer) {
        return new RewardReadyPacket();
    }

    public static void handle(RewardReadyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(
                                Component.literal("¡Meta alcanzada! Has desbloqueado una recompensa."),
                                false
                        );
                    }
                }
        ));
        context.setPacketHandled(true);
    }
}
