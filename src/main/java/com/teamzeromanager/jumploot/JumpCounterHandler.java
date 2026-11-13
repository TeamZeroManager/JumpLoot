package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class JumpCounterHandler {
    private static final String CURRENT_JUMPS_TAG = "jumploot.current_jumps";
    private static final String NEXT_TARGET_TAG = "jumploot.next_target";
    private static final int JUMPS_PER_REWARD = 10;

    private JumpCounterHandler() {}

    public static void onPlayerJump(ServerPlayer player) {
        CompoundTag data = PlayerDataStore.get(player);
        int current = Math.max(0, data.getInt(CURRENT_JUMPS_TAG)) + 1;
        int target = getNextTarget(data);
        boolean earned = current >= target;
        if (earned) {
            current = 0;
            target = JUMPS_PER_REWARD;
            data.putInt(NEXT_TARGET_TAG, target);
            player.displayClientMessage(Component.literal(
                    "¡Recompensa conseguida! Próxima meta: " + target + " saltos."), false);
        }
        data.putInt(CURRENT_JUMPS_TAG, current);
        JumpNetwork.sendProgress(player, current, target);
        if (earned) LootRewardService.award(player);
    }

    public static void sendCurrentProgress(ServerPlayer player) {
        CompoundTag data = PlayerDataStore.get(player);
        JumpNetwork.sendProgress(player, Math.max(0, data.getInt(CURRENT_JUMPS_TAG)), getNextTarget(data));
    }

    private static int getNextTarget(CompoundTag data) {
        // Test configuration: every reward requires exactly 10 jumps.
        return JUMPS_PER_REWARD;
    }
}
