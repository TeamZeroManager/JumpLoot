package com.teamzeromanager.jumploot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class LootRewardService {
    private LootRewardService() {
    }

    public static void award(ServerPlayer player) {
        LootItemCatalog catalog = LootItemCatalog.build();
        if (catalog.getTotalItemCount() == 0) {
            JumpLoot.LOGGER.error("No eligible items were found for the reward roulette.");
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "No se encontraron objetos disponibles para la ruleta."), false);
            return;
        }

        LootRewardSelector.Result result = LootRewardSelector.select(
                RandomSource.create(), catalog, LootRewardSelector.defaultWeights());
        ItemStack reward = result.item();

        // Save first on the server so closing the reveal screen cannot lose the reward.
        LootStorage.add(player, reward);
        JumpNetwork.sendRewardReady(player, result.rarity(), reward);
        JumpLoot.LOGGER.info("Generated and stored Jump Loot reward for {}: {} ({})",
                player.getGameProfile().getName(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(reward.getItem()),
                result.rarity());
    }
}
