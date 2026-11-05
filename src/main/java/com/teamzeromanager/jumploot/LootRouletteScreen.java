package com.teamzeromanager.jumploot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
/** Simultaneous multi-reward gacha reveal. */
public final class LootRouletteScreen extends Screen {
 private enum Phase{RARITY,ITEM,RESULT} private static final int RARITY_TICKS=110,ITEM_TICKS=145;
 private final List<LootStorage.StoredReward> rewards=new ArrayList<>(); private final List<List<Particle>> particles=new ArrayList<>(); private Phase phase=Phase.RARITY;private int ticks;
 private static final class Particle{final float angle,radius,speed,delay;final int size;Particle(RandomSource r){angle=r.nextFloat()*(float)(Math.PI*2);radius=18+r.nextFloat()*34;speed=.5f+r.nextFloat();delay=r.nextFloat()*.45f;size=1+r.nextInt(2);}}
 public LootRouletteScreen(List<LootStorage.StoredReward> list){super(Component.literal("Jump Loot"));for(LootStorage.StoredReward r:list){rewards.add(new LootStorage.StoredReward(r.stack(),r.rarity()));RandomSource random=RandomSource.create();List<Particle> p=new ArrayList<>();for(int i=0;i<24;i++)p.add(new Particle(random));particles.add(p);}}
 @Override public void tick(){super.tick();ticks++;if(phase==Phase.RARITY&&ticks>=RARITY_TICKS){phase=Phase.ITEM;ticks=0;}else if(phase==Phase.ITEM&&ticks>=ITEM_TICKS){phase=Phase.RESULT;ticks=0;}}
 private int color(int c,int a){return(Math.max(0,Math.min(255,a))<<24)|(c&0xffffff);}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);int n=rewards.size();if(n==0){onClose();return;}int cols=n==1?1:n<=4?2:n<=9?3:4;int rows=(n+cols-1)/cols;int cw=Math.min(116,(width-24)/cols),ch=Math.min(112,(height-62)/rows);int pw=cols*cw+16,ph=rows*ch+48,l=(width-pw)/2,t=(height-ph)/2;g.fill(l,t,l+pw,t+ph,0xf0100d16);g.fill(l+2,t+2,l+pw-2,t+ph-2,0xf0272231);g.drawCenteredString(font,"✦ JUMP LOOT · "+n+" RECOMPENSA"+(n==1?"":"S"),width/2,t+10,0xffffd45a);
 for(int i=0;i<n;i++){LootStorage.StoredReward reward=rewards.get(i);ItemStack stack=reward.stack();int col=i%cols,row=i/cols,x=l+8+col*cw,y=t+30+row*ch,c=reward.rarity().getColor(),cx=x+(cw-5)/2,cy=y+ch/2-4;g.fill(x,y,x+cw-5,y+ch-5,0xff15111c);g.fill(x+1,y+1,x+cw-6,y+ch-6,color(c,55));g.drawCenteredString(font,phase==Phase.RARITY?"REVELANDO RAREZA":reward.rarity().name(),cx,y+6,phase==Phase.RARITY?0xffffd45a:c);
 if(phase!=Phase.RESULT){float p=Math.min(1f,ticks/(float)(phase==Phase.RARITY?RARITY_TICKS:ITEM_TICKS));for(Particle q:particles.get(i)){float z=Math.max(0,Math.min(1,(p-q.delay)/(1-q.delay))),rad=q.radius*(1-z*.88f),a=q.angle+ticks*.08f*q.speed;int px=cx+(int)(Math.cos(a)*rad),py=cy+(int)(Math.sin(a)*rad*.65);g.fill(px,py,px+q.size,py+q.size,color(phase==Phase.RARITY?0xffb9c6d8:c,(int)(230*z)));}if(phase==Phase.ITEM&&p>.55f){g.renderItem(stack,cx-8,cy-8);g.drawCenteredString(font,stack.getHoverName(),cx,y+ch-21,0xffffffff);}}
 else{g.renderItem(stack,cx-8,cy-12);g.drawCenteredString(font,stack.getHoverName(),cx,y+ch-31,0xffffffff);g.drawCenteredString(font,"x"+stack.getCount()+" · "+reward.rarity().name(),cx,y+ch-19,c);}}
 g.drawCenteredString(font,phase==Phase.RESULT?"¡RECOMPENSAS OBTENIDAS!":"Todas las recompensas se revelan simultáneamente",width/2,t+ph-12,phase==Phase.RESULT?0xffffd45a:0xffbdb4c9);if(phase==Phase.RESULT)g.drawCenteredString(font,"ENTER o ESC para continuar",width/2,Math.min(height-12,t+ph+5),0xffe6e0ee);super.render(g,mx,my,partial);}
 @Override public boolean keyPressed(int key,int scan,int modifiers){if(phase==Phase.RESULT&&(key==257||key==256)){JumpNetwork.requestStorage();return true;}return super.keyPressed(key,scan,modifiers);}
 @Override public boolean isPauseScreen(){return true;}
}
