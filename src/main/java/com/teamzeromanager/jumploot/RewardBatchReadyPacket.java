package com.teamzeromanager.jumploot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
public final class RewardBatchReadyPacket {
 private final List<LootStorage.StoredReward> rewards;
 public RewardBatchReadyPacket(List<LootStorage.StoredReward> rewards){this.rewards=new ArrayList<>();for(LootStorage.StoredReward r:rewards)this.rewards.add(new LootStorage.StoredReward(r.stack(),r.rarity()));}
 public static void encode(RewardBatchReadyPacket p,FriendlyByteBuf b){b.writeVarInt(p.rewards.size());for(LootStorage.StoredReward r:p.rewards){b.writeEnum(r.rarity());b.writeItem(r.stack());}}
 public static RewardBatchReadyPacket decode(FriendlyByteBuf b){int n=Math.max(0,Math.min(1000,b.readVarInt()));List<LootStorage.StoredReward> list=new ArrayList<>(n);for(int i=0;i<n;i++){LootRarity rarity=b.readEnum(LootRarity.class);list.add(new LootStorage.StoredReward(b.readItem(),rarity));}return new RewardBatchReadyPacket(list);}
 public static void handle(RewardBatchReadyPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Minecraft.getInstance().setScreen(new LootRouletteScreen(p.rewards))));c.setPacketHandled(true);}
}
