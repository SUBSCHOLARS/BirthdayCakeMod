package com.example.examplemod.birthdaycake;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BirthdayCakeBlockEntity extends BlockEntity {
    //左右の蝋燭の情報をItemStackとして保持する
    private ItemStack candleLeft=ItemStack.EMPTY;
    private ItemStack candleRight=ItemStack.EMPTY;

    //左右の着火状況をBool値として保持する
    private boolean litLeft=false;
    private boolean litRight=false;

    private int effectTicks=0;
    private static final int EFFECT_DURATION=380;

    private int musicIndex=0;
    private int nextNoteTick=0;

    private static final int[] NOTE_PITCHES = { //音階（ドキュメント参照→https://minecraft.fandom.com/ja/wiki/%E9%9F%B3%E7%AC%A6%E3%83%96%E3%83%AD%E3%83%83%E3%82%AF）
            6, 6, 8, 6, 11, 10,
            6, 6, 8, 6, 13, 11,
            6, 6, 18, 15, 11, 10, 8,
            16, 16, 15, 11, 13, 11
    };

    private static final int[] NOTE_DELAYS = { // 各音符の長さ (tick単位、12で1拍)
            12, 6, 12, 12, 12, 24,
            12, 6, 12, 12, 12, 24,
            12, 6, 12, 12, 12, 12, 24,
            12, 6, 12, 12, 12, 24
    };
    //コンストラクタ
    public BirthdayCakeBlockEntity(BlockPos pPos, BlockState pState){
        super(ExampleMod.BIRTHDAY_CAKE_ENTITY,pPos,pState);
    }
    //データをNBTに保存する処理
    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        //ろうそくが立っているかの情報を保存
        pTag.put("CandleLeft",candleLeft.save(new CompoundTag()));
        pTag.put("CandleRight",candleRight.save(new CompoundTag()));

        //ろうそくが着火されているかの情報を保存
        pTag.putBoolean("LitLeft",this.litLeft);
        pTag.putBoolean("LitRight",this.litRight);

        pTag.putInt("EffectTicks", this.effectTicks);
    }
    //NBTからデータを読み込む処理
    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        this.candleLeft=ItemStack.of(pTag.getCompound("CandleLeft"));
        this.candleRight=ItemStack.of(pTag.getCompound("CandleRight"));

        this.litLeft = pTag.getBoolean("LitLeft");
        this.litRight = pTag.getBoolean("LitRight");

        this.effectTicks = pTag.getInt("EffectTicks");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag=new CompoundTag();
        saveAdditional(tag);//現在のデータをNBTに保存
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);//受け取ったNBTでデータを読み込み
    }

    //tickメソッド
    public static void tick(Level level, BlockPos pos, BlockState state, BirthdayCakeBlockEntity cakeEntity){
        //エフェクトが実行中であれば何もしない
        if(cakeEntity.effectTicks<=0){
            return;
        }

        //カウンタを減らす
        cakeEntity.effectTicks--;

        //演奏する
        if(level instanceof ServerLevel serverLevel){
            cakeEntity.nextNoteTick--;
            if(cakeEntity.nextNoteTick<=0&&cakeEntity.musicIndex<NOTE_PITCHES.length){
                //次の音符を鳴らす時間をセットする
                cakeEntity.nextNoteTick=NOTE_DELAYS[cakeEntity.musicIndex];
                //音符ブロックの音を再生
                float pitch=(float)Math.pow(2.0D,(double) (NOTE_PITCHES[cakeEntity.musicIndex]-12)/12.0D);
                serverLevel.playSound(null,pos,SoundEvents.NOTE_BLOCK_BELL,SoundSource.RECORDS,5.0F,pitch);
                serverLevel.sendParticles(
                        ParticleTypes.NOTE,
                        pos.getX()+0.5D,
                        pos.getY()+1.2D,
                        pos.getZ()+0.5D,
                        1,
                        (double) NOTE_PITCHES[cakeEntity.musicIndex]/24.0D,
                        0.0D,
                        0.0D,
                        1.0D
                );
                cakeEntity.musicIndex++;
            }
        }

        //一定間隔で花火を出す
        if(!level.isClientSide && cakeEntity.effectTicks%100==0){
            ItemStack fireworkStack=new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fireworkTag=new CompoundTag();
            ListTag explosionTag=new ListTag();
            CompoundTag explosionDataTag=new CompoundTag();

            explosionDataTag.putByte("Type",(byte) (level.random.nextInt(4)));
            explosionDataTag.putIntArray("Colors",new int[]{level.random.nextInt(0xFFFFFF)});
            explosionDataTag.putBoolean("Flicker",level.random.nextBoolean());
            explosionDataTag.putBoolean("Trail",level.random.nextBoolean());

            explosionTag.add(explosionDataTag);
            fireworkTag.put("Explosions",explosionTag);
            fireworkTag.putByte("Flight",(byte) (level.random.nextInt(2)+1));

            fireworkStack.addTagElement("Fireworks",fireworkTag);

            FireworkRocketEntity fireworkRocketEntity=new FireworkRocketEntity(
                    level,
                    pos.getX()+0.5D,
                    pos.getY()+2.0D,
                    pos.getZ()+0.5D,
                    fireworkStack
            );
            level.addFreshEntity(fireworkRocketEntity);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(Objects.requireNonNull(pkt.getTag()));
    }

    //外部からろうそくの情報を取得・設定するためのメソッド
    public ItemStack getCandleLeft(){
        return candleLeft;
    }
    public ItemStack getCandleRight(){
        return candleRight;
    }
    public void setCandleLeft(ItemStack stack){
        this.candleLeft=stack.copy();
    }
    public void setCandleRight(ItemStack stack){
        this.candleRight=stack.copy();
    }
    public boolean isLitLeft(){
        return this.litLeft;
    }
    public boolean isLitRight(){
        return this.litRight;
    }
    public void setLit(boolean left, boolean right){
        boolean wasUnlit=!this.litLeft||!this.litRight;
        this.litLeft=left;
        this.litRight=right;

        //両方のろうそくが着火された瞬間を検知
        if(wasUnlit&&this.litLeft&&this.litRight){
            startEffect();
        }
        if(level!=null&&!level.isClientSide) {
            level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            setChanged();
        }
    }
    private void startEffect(){
        if(!level.isClientSide&&this.effectTicks<=0){
            this.effectTicks=EFFECT_DURATION;
            this.musicIndex=0;
            this.nextNoteTick=0;
            level.playSound(null,getBlockPos(), SoundEvents.PLAYER_LEVELUP,SoundSource.BLOCKS,1.0F,1.0F);
        }
    }
}
