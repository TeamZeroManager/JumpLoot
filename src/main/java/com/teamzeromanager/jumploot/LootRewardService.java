package com.teamzeromanager.jumploot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** Server-authoritative reward generation. The result is fixed before the client animation starts. */
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
        JumpNetwork.sendRewardReady(player, result.rarity(), result.item());
        JumpLoot.LOGGER.info("Generated Jump Loot reward for {}: {} ({})",
                player.getGameProfile().getName(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.item().getItem()),
                result.rarity());
    }
}
