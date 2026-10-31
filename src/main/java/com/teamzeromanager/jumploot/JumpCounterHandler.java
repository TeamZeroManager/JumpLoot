package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tracks player jump progression and synchronizes it with the client HUD.
 */
@Mod.EventBusSubscriber(modid = JumpLoot.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JumpCounterHandler {
    private static final String CURRENT_JUMPS_TAG = "jumploot.current_jumps";
    private static final String NEXT_TARGET_TAG = "jumploot.next_target";

    private static final int FIRST_TARGET = 30;
    private static final int TARGET_INCREMENT = 10;
    private static final int MAX_TARGET = 100;

    private JumpCounterHandler() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        int currentJumps = Math.max(0, data.getInt(CURRENT_JUMPS_TAG));
        int nextTarget = getNextTarget(data);

        currentJumps++;
        boolean rewardEarned = currentJumps >= nextTarget;
        if (rewardEarned) {
            currentJumps = 0;
            nextTarget = Math.min(nextTarget + TARGET_INCREMENT, MAX_TARGET);
            data.putInt(NEXT_TARGET_TAG, nextTarget);
            player.displayClientMessage(
                    Component.literal("¡Recompensa conseguida! Próxima meta: " + nextTarget + " saltos."),
                    false
            );
        }

        data.putInt(CURRENT_JUMPS_TAG, currentJumps);
        JumpNetwork.sendProgress(player, currentJumps, nextTarget);
        if (rewardEarned) {
            JumpNetwork.sendRewardReady(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        newData.putInt(NEXT_TARGET_TAG, getNextTarget(oldData));
        int currentJumps = event.isWasDeath() ? 0 : Math.max(0, oldData.getInt(CURRENT_JUMPS_TAG));
        newData.putInt(CURRENT_JUMPS_TAG, currentJumps);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            JumpNetwork.sendProgress(player, Math.max(0, data.getInt(CURRENT_JUMPS_TAG)), getNextTarget(data));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            JumpNetwork.sendProgress(player, Math.max(0, data.getInt(CURRENT_JUMPS_TAG)), getNextTarget(data));
        }
    }

    private static int getNextTarget(CompoundTag data) {
        int target = data.contains(NEXT_TARGET_TAG) ? data.getInt(NEXT_TARGET_TAG) : FIRST_TARGET;
        if (target < FIRST_TARGET) {
            return FIRST_TARGET;
        }
        return Math.min(target, MAX_TARGET);
    }
}
