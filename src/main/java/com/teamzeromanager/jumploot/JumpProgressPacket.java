package com.teamzeromanager.jumploot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class JumpProgressPacket {
    private final int current;
    private final int target;

    public JumpProgressPacket(int current, int target) {
        this.current = Math.max(0, current);
        this.target = Math.max(1, target);
    }

    public static void encode(JumpProgressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.current);
        buffer.writeVarInt(packet.target);
    }

    public static JumpProgressPacket decode(FriendlyByteBuf buffer) {
        return new JumpProgressPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(JumpProgressPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientJumpHud.setProgress(packet.current, packet.target)
        ));
        context.setPacketHandled(true);
    }
}
