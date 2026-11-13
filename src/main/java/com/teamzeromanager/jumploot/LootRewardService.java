package com.teamzeromanager.jumploot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class LootRewardService {
    private static final String REWARDS_SINCE_HIGH_RARITY_TAG = "jumploot.rewards_since_high_rarity";
    private static final String REWARDS_TOWARD_SPECIAL_TAG = "jumploot.rewards_toward_special";
    private static final int REWARDS_BEFORE_GUARANTEE = 5;
    private static final int REWARDS_BEFORE_SPECIAL = 30;

    private LootRewardService() {}

    public static void award(ServerPlayer player) {
        CompoundTag data = PlayerDataStore.get(player);
        int rewardsTowardSpecial = Math.max(0, data.getInt(REWARDS_TOWARD_SPECIAL_TAG));
        if (rewardsTowardSpecial >= REWARDS_BEFORE_SPECIAL) {
            LootStorage.addSpecial(player);
            data.putInt(REWARDS_TOWARD_SPECIAL_TAG, 0);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "✦ ¡Recompensa especial desbloqueada! Reclámala desde tu colección."), false);
            JumpLoot.LOGGER.info("Special selectable reward unlocked for {}.",
                    player.getGameProfile().getName());
            return;
        }

        LootItemCatalog catalog = LootItemCatalog.build();
        if (catalog.getTotalItemCount() == 0) {
            JumpLoot.LOGGER.error("No eligible items were found for the reward roulette.");
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "No se encontraron objetos disponibles para la ruleta."), false);
            return;
        }

        int rewardsSinceHighRarity = Math.max(0, data.getInt(REWARDS_SINCE_HIGH_RARITY_TAG));
        boolean guaranteedHighRarity = rewardsSinceHighRarity >= REWARDS_BEFORE_GUARANTEE;
        LootRewardSelector.Result result;
        try {
            result = guaranteedHighRarity
                    ? LootRewardSelector.selectGuaranteedHighRarity(RandomSource.create(), catalog)
                    : LootRewardSelector.select(RandomSource.create(), catalog, LootRewardSelector.defaultWeights());
        } catch (IllegalStateException exception) {
            JumpLoot.LOGGER.error("Could not select a reward for {}.", player.getGameProfile().getName(), exception);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "No fue posible generar la recompensa. Tu garantía se conservará."), false);
            return;
        }

        if (guaranteedHighRarity) {
            data.putInt(REWARDS_SINCE_HIGH_RARITY_TAG, 0);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "¡Garantía activada! Tu recompensa es Épica o Legendaria."), false);
        } else {
            data.putInt(REWARDS_SINCE_HIGH_RARITY_TAG, rewardsSinceHighRarity + 1);
        }

        data.putInt(REWARDS_TOWARD_SPECIAL_TAG, rewardsTowardSpecial + 1);
        ItemStack reward = result.item();
        LootStorage.add(player, reward, result.rarity());
        JumpLoot.LOGGER.info("Generated and stored Jump Loot reward for {}: {} ({}) [high rarity guarantee: {}]",
                player.getGameProfile().getName(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(reward.getItem()),
                result.rarity(), guaranteedHighRarity);
    }
}
