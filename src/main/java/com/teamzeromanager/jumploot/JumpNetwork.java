package com.teamzeromanager.jumploot;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

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
    }

    public static void sendProgress(net.minecraft.server.level.ServerPlayer player, int current, int target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JumpProgressPacket(current, target));
    }

    public static void sendRewardReady(net.minecraft.server.level.ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RewardReadyPacket());
    }
}
